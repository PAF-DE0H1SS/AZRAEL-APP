#pragma once

#include <atomic>
#include <condition_variable>
#include <deque>
#include <functional>
#include <future>
#include <mutex>
#include <thread>
#include <utility>
#include <vector>

namespace az {

// Пул потоков (C++17). Для фонового выполнения сетевых вызовов и
// C++-логики без создания потока на каждую задачу.
// Пример использования - in test/ThreadPool_test.cpp.

template <typename T>
class SafeQueue {
public:
    void push(T v) {
        {
            std::lock_guard<std::mutex> lk(m_);
            q_.push_back(std::move(v));
        }
        cv_.notify_one();
    }

    bool pop(T& out, int timeout_ms = -1) {
        std::unique_lock<std::mutex> lk(m_);
        auto pred = [this] { return !q_.empty() || closed_; };
        if (timeout_ms < 0) {
            cv_.wait(lk, pred);
        } else if (!cv_.wait_for(lk, std::chrono::milliseconds(timeout_ms), pred)) {
            return false;
        }
        if (q_.empty()) return false;
        out = std::move(q_.front());
        q_.pop_front();
        return true;
    }

    void close() {
        {
            std::lock_guard<std::mutex> lk(m_);
            closed_ = true;
        }
        cv_.notify_all();
    }

    size_t size() const {
        std::lock_guard<std::mutex> lk(m_);
        return q_.size();
    }

private:
    mutable std::mutex m_;
    std::condition_variable cv_;
    std::deque<T> q_;
    bool closed_ = false;
};

class ThreadPool {
public:
    explicit ThreadPool(size_t n = std::thread::hardware_concurrency()) {
        for (size_t i = 0; i < n; ++i) {
            workers_.emplace_back([this] {
                for (;;) {
                    std::function<void()> task;
                    if (!queue_.pop(task)) {
                        if (!open_) return;
                        continue;
                    }
                    task();
                }
            });
        }
    }

    ~ThreadPool() {
        queue_.close();
        open_ = false;
        queue_.close();
        for (auto& w : workers_) w.join();
    }

    ThreadPool(const ThreadPool&) = delete;
    ThreadPool& operator=(const ThreadPool&) = delete;

    void enqueue(std::function<void()> fn) { queue_.push(std::move(fn)); }

    // Задача с возвращаемым значением (std::async-подобно).
    template <typename F, typename... Args>
    auto submit(F&& fn, Args&&... args) -> std::future<decltype(fn(args...))> {
        using R = decltype(fn(args...));
        auto p = std::make_shared<std::promise<R>>();
        auto fut = p->get_future();
        auto bound = std::bind(std::forward<F>(fn), std::forward<Args>(args)...);
        queue_.push([p, bound = std::move(bound)]() mutable {
            try {
                p->set_value(bound());
            } catch (...) {
                p->set_exception(std::current_exception());
            }
        });
        return fut;
    }

    size_t pending() const { return queue_.size(); }
    size_t workers() const { return workers_.size(); }

private:
    mutable std::atomic<bool> open_{true};
    SafeQueue<std::function<void()>> queue_;
    std::vector<std::thread> workers_;
};

namespace thread_pool_impl {
// Локальный синглтон-пул на весь процесс (удобен для fire-and-forget).
inline ThreadPool& global() {
    static ThreadPool pool;
    return pool;
}
}  // namespace thread_pool_impl

inline void run_async(std::function<void()> fn) {
    thread_pool_impl::global().enqueue(std::move(fn));
}

}  // namespace az