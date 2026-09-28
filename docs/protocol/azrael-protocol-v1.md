# AZRAEL Protocol v1 (azrael-protocol-v1)

Транспорт между публичным клиентом AZRAEL-APP и защищённым шлюзом `azrael-lab.xyz`.

## Принципы

1. Клиент знает ТОЛЬКО единую точку входа (домен) и номер версии протокола.
   Никаких IP, портов, токенов, паролей или внутренней топологии в коде клиента.
2. Все RPC-сообщения шифруются AEAD (end-to-end между клиентом и шлюзом).
3. Идентификация/авторизация — короткоживущие сессии, ключи не покидают устройство.
4. Протокол версионируется полем `v`; неподдерживаемая версия возвращает `ErrVersion`.

## Единая точка входа

- URL шлюза задаётся конфигом (НЕ хардкодится в репозитории):
  `AZRAEL_GATEWAY_URL` / runtime-конфиг (например `.env` вне git).
- Метод: `POST`.
- Путь: `/gateway/v1` (может быть прокинут через любой прокси).

## Конверт (Envelope)

Каждый запрос/ответ — JSON-объект:

```json
{
  "v": 1,
  "op": "<operation>",
  "id": "<uuid>",
  "ts": 1750000000,
  "session": "<session-token>",
  "payload": "<base64 AEAD-шифротекст>",
  "nonce": "<base64>"
}
```

- `v`: версия протокола (сейчас `1`).
- `op`: операция (см. ниже).
- `id`: уникальный ID запроса (антиреплей, дедупликация на сервере).
- `ts`: UNIX-время клиента в секундах.
- `session`: токен сессии (после `session.init`), иначе пустая строка.
- `payload`: сериализованный (CBOR/JSON) и зашифрованный контент операции.
- `nonce`: случайный AEAD-нонс.

## Шифрование

- Асимметрия: X25519 (ECDH, кривая Curve25519 «складской» режим).
- KDF: НЕТ — shared-secret X25519 (32 байта) используется напрямую как ключ AES-256 (в коде нет HKDF).
- AEAD: AES-256-GCM (128-бит тег, nonce 12 байт). Расшифровывающая сторона срезает последние 16 байт шифротекста как тег.
- Дополнительные данные (AAD): `op | id | ts | v` (одна и та же конструкция в обе стороны) — защита от подмены полей конверта. **Важно:** счётчик/`counter` в AAD не участвует — клиент и сервер строят AAD детерминированно только из полей конверта.

Ключ клиента генерируется при запуске (JCA/JVM) и не покидает устройство.
Ключ сервера (публичный) доставляется в ответе `session.init` (TOFU) поверх TLS `azrael-lab.xyz`.

## Формат ключей X25519 (совместимость клиент↔сервер)

- **Клиент → сервер** (`eph_pub`): X.509 SPKI DER (44 байта: prefix `302a300506032b656e032100` + raw-32). Сервер берёт **последние 32 байта** как сырой координату.
- **Сервер → клиент** (`srv_pub`): **raw 32 байта** (base64). Клиент парсит как `BigInteger` через `XECPublicKeySpec`/`NamedParameterSpec.X25519` (SPKI сервер не шлёт).

## Жизненный цикл сессии

1. `session.init` — клиент шлёт `eph_pub` (SPKI, base64 в JSON, сам JSON закодирован base64 в `payload` — открытым конвертом). Необязательное поле `auth` — site-сессия: если она валидна, сервер повышает роль канала.
2. Сервер отвечает `payload = base64(JSON {"srv_pub": <raw32 base64>, "session": <token>, "role": <guest|standard|admin>, "user": <username>})` — тоже открытым (до обмена ключами шифровать нечем). Токен TTL 15 мин, роль в нём сохраняется на весь срок сессии.
3. Дальнейшие `op`-вызовы идут с `session`-токеном и AEAD-шифрованным payload (op/status/hello/session.rotate/session.bye/ai.chat/cmd.*).
4. `session.rotate` — смена токена без смены ключа (для смены ключа повторяется `session.init`).
5. `session.bye` — явное завершение (сервер удаляет сессию и отвечает зашифрованным `bye`).

## RBAC (роли и доступ операций)

Роль выдаётся при `session.init` и не меняется до конца сессии (ротация токена её сохраняет):

- `guest` (по умолчанию, без `auth`) — `session.init/bye/rotate`, `status`, `hello`.
- `standard` (валидная site-сессия обычного пользователя) — всё у `guest` + `ai.chat`.
- `admin` (валидная site-сессия owner/admin) — всё у `standard` + `cmd.*` (`cmd.ping`, `cmd.info`).

Попытка вызова сверх роли даёт `err=108` (HTTP 403). Ответы `status`/`hello` содержат поле `role`, чтобы клиент видел свои права.

## Пример handshake (actual)

```
C → S:  POST /gateway/v1  {"v":1,"op":"session.init","id":"…","ts":…,
                           "payload": b64(JSON {"eph_pub": b64(SPKI_DER)})}
S → C:  {"v":1,"op":"session.init","ts":…,"session":<token>,
          "payload": b64(JSON {"srv_pub": b64(raw32), "session": <token>,
                               "role": "guest", "user": ""}), "err":0}
C:      shared = X25519(clientPriv, srv_pub_raw32)   // 32 байта = ключ AES-256
C → S:  {"v":1,"op":"status","id":"…","ts":…,"session":token,
          "payload": b64(ct||authTag), "nonce": b64(12B),
          AAD = "status|id|ts|1"}
S → C:  {"v":1,"op":"status","id":"…","ts":…,"session":token,
          "payload": b64(ct||authTag), "nonce": b64(12B),
          AAD = "status|<newId>|<newTs>|1", "err":0}
```

## Операции

| op               | Направление       | Назначение                                |
|------------------|-------------------|-------------------------------------------|
| `session.init`   | client → server   | старт сессии (обмен ключами)             |
| `session.rotate` | client → server   | ротация ключа/токена                     |
| `session.bye`    | client → server   | завершение сессии                        |
| `status`         | client → server   | статус подключения / версия шлюза        |
| `hello`          | client → server   | ping/echo для проверки канала            |
| `cmd.*`          | client → server   | командные операции (админ-панель)        |
| `ai.chat`        | client → server   | chat через координатор FREE-AI           |
| `err.*`          | server → client   | ошибки                                   |

## Ошибки (top-level)

| code      | HTTP | смысл                                        |
|-----------|------|----------------------------------------------|
| 0         | 200  | ok                                           |
| 100       | 400  | malformed envelope                           |
| 101       | 400  | unsupported `v`                               |
| 102       | 400  | bad timestamp (отклонено по skew)             |
| 103       | 409  | duplicate `id` (replay)                       |
| 104       | 401  | invalid/missing session                       |
| 105       | 404  | unknown `op`                                  |
| 106       | 429  | rate limit                                    |
| 107       | 500  | internal gateway error                        |
| 108       | 403  | forbidden by RBAC                             |

Ответ так же шифруется AEAD; `payload` равен пустой строке при ошибке,
а код ошибки приходит полем `err` конверта (не в payload).

## Ограничения и защита

- `ts` клиента должно отличаться от времени сервера не более чем на ±120 с (настраивается).
- Каждый `id` принимается один раз (скользящее окно на Redis/слой шлюза).
- Rate-limit: N запросов/мин на сессию и на IP (настраивается).
- RBAC: каждая операция требует роль (guest/standard/admin); отказ = код 108.
- Полезные данные не логируются. Логируются только метаданные конверта (без payload/nonce).

## Пример handshake (упрощённо)

```
C → S:  POST /gateway/v1  {"v":1,"op":"session.init","id":"…","ts":…,"payload":E(ephPub),… }
S → C:  {"v":1,"op":"session.init","id":"…","payload":E(srvPub|sessionToken),"err":0}
C → S:  POST /gateway/v1  {"v":1,"op":"status","session":token,"payload":AEAD(request),"nonce":…}
S → C:  {"v":1,"op":"status","session":token,"payload":AEAD(response),"nonce":…}
```

## Язык реализации

- Клиент: Kotlin Multiplatform (`core/protocol`), impl JVM/JCA + WasmJS/WebCrypto.
- Сервер: Next.js Route Handler на SITE (закрытый код).