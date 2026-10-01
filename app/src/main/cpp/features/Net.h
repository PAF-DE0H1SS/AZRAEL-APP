#pragma once

#include <array>
#include <cstdint>
#include <string>

namespace az {
namespace net {

// Простой блокирующий TCP-сокет (POSIX, C++17).
// Для HTTPS поверх используйте OpenSSL/вызов JVM (OkHttp) - тут чистый TCP,
// чтобы не тянуть тяжёлые зависимости в NDK-сборку.

class TcpSocket {
public:
    TcpSocket() = default;
    ~TcpSocket() { close(); }

    TcpSocket(const TcpSocket&) = delete;
    TcpSocket& operator=(const TcpSocket&) = delete;
    TcpSocket(TcpSocket&& o) noexcept : fd_(o.fd_) { o.fd_ = -1; }
    TcpSocket& operator=(TcpSocket&& o) noexcept {
        if (this != &o) { close(); fd_ = o.fd_; o.fd_ = -1; }
        return *this;
    }

    bool valid() const { return fd_ >= 0; }

    // Подключение к host:port (getaddrinfo, IPv4/IPv6, первый успешный адрес).
    // timeout_ms: <=0 - блокирово; >0 - лимит на connect().
    bool connect(const std::string& host, uint16_t port, int timeout_ms = 0);

    void close();

    // Возвращает количество байт или -1 при ошибке/timeout (errno сохранён).
    ssize_t send_bytes(const void* data, size_t len);
    ssize_t recv_bytes(void* buf, size_t len);

    // Приёмоаднастроенная обёртка: шлёт всё целиком.
    bool send_all(const void* data, size_t len);
    // Приём до появления len байт (CRC для частичных чтений).
    size_t recv_some(void* buf, size_t max_len, int32_t timeout_ms = 0);

    // Установить времяожидания для последующих recv().
    void set_timeout(int timeout_ms);

    int fd() const { return fd_; }

private:
    int fd_ = -1;
};

// Чтение одной строки HTTP-ответа (до \r\n), не блокируя дальше.
bool read_crlf_line(TcpSocket& s, std::string& line, size_t max_len = 8192,
                    int32_t timeout_ms = 0);

// Простой разбор "host:port". Также умеет "[::1]:443".
bool parse_host_port(const std::string& s, std::string& host, uint16_t& port,
                     uint16_t default_port = 443);

}  // namespace net
}  // namespace az