#pragma once

#include <cstdint>
#include <map>
#include <string>
#include <string_view>
#include <variant>
#include <vector>

namespace az {

// Минимальный JSON (парсер + сериализатор), C++17, без зависимостей.
// Достаточен для REST-ответов azrael-lab API. Строго UTF-8.
// НЕ потокобезопасный, создавайте свой экземпляр на вызов.

class Json {
public:
    using Array = std::vector<Json>;
    using Object = std::map<std::string, Json, std::less<>>;
    using Value =
        std::variant<std::nullptr_t, bool, double, std::string, Array, Object>;

    Json() : v_(nullptr) {}
    Json(std::nullptr_t) : v_(nullptr) {}
    Json(bool b) : v_(b) {}
    Json(int n) : v_(static_cast<double>(n)) {}
    Json(int64_t n) : v_(static_cast<double>(n)) {}
    Json(double d) : v_(d) {}
    Json(const char* s) : v_(std::string(s)) {}
    Json(std::string s) : v_(std::move(s)) {}
    Json(std::string_view s) : v_(std::string(s)) {}
    Json(Array a) : v_(std::move(a)) {}
    Json(Object o) : v_(std::move(o)) {}

    static Json array() { return Json(Array{}); }
    static Json object() { return Json(Object{}); }

    const Value& value() const { return v_; }
    Value& value() { return v_; }

    bool is_null() const { return std::holds_alternative<std::nullptr_t>(v_); }
    bool is_bool() const { return std::holds_alternative<bool>(v_); }
    bool is_number() const { return std::holds_alternative<double>(v_); }
    bool is_string() const { return std::holds_alternative<std::string>(v_); }
    bool is_array() const { return std::holds_alternative<Array>(v_); }
    bool is_object() const { return std::holds_alternative<Object>(v_); }

    bool as_bool(bool def = false) const {
        return is_bool() ? std::get<bool>(v_) : def;
    }
    double as_number(double def = 0.0) const {
        return is_number() ? std::get<double>(v_) : def;
    }
    int64_t as_int(int64_t def = 0) const {
        return is_number() ? static_cast<int64_t>(std::get<double>(v_)) : def;
    }
    const std::string& as_string(const std::string& def = {}) const {
        static const std::string empty;
        return is_string() ? std::get<std::string>(v_) : def;
    }
    const Array& as_array() const {
        static const Array empty;
        return is_array() ? std::get<Array>(v_) : empty;
    }
    const Object& as_object() const {
        static const Object empty;
        return is_object() ? std::get<Object>(v_) : empty;
    }

    // Доступ по ключу через точку: get("data/nodes[0]/node")? Нет.
    // Простая навигация: get("data.status"), get("data.vmid").
    // Промежуточный объект или массив - смотрим через parent.get(path).
    Json get(std::string_view path) const {
        Json cur = *this;
        size_t start = 0;
        while (start <= path.size()) {
            size_t dot = path.find('.', start);
            size_t end = (dot == std::string_view::npos) ? path.size() : dot;
            std::string_view key = path.substr(start, end - start);
            if (!key.empty()) {
                if (cur.is_object()) {
                    const Object& o = cur.as_object();
                    auto it = o.find(key);
                    if (it == o.end()) return Json(nullptr);
                    cur = it->second;
                } else {
                    return Json(nullptr);
                }
            }
            if (dot == std::string_view::npos) break;
            start = dot + 1;
        }
        return cur;
    }

    // Индексатор по имени ключа (только для object).
    const Json* find(std::string_view key) const {
        if (!is_object()) return nullptr;
        const Object& o = std::get<Object>(v_);
        auto it = o.find(key);
        return it == o.end() ? nullptr : &it->second;
    }
    Json* find(std::string_view key) {
        if (!is_object()) return nullptr;
        Object& o = std::get<Object>(v_);
        auto it = o.find(key);
        return it == o.end() ? nullptr : &it->second;
    }

    Json& operator[](std::string_view key) {
        if (!is_object()) v_ = Object{};
        return std::get<Object>(v_)[std::string(key)];
    }
    Json operator[](std::string_view key) const {
        return get(key);
    }

    // --- Разбор ---
    static bool parse(std::string_view text, Json& out);

    std::string dump(bool pretty = false) const;

private:
    Value v_;
};

}  // namespace az