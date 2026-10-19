# Документ по миграции

## T-ID 1.2.0 -> T-ID 1.3.0

Добавлена поддержка нескольких доменов для открытия приложения Т-Банка. Для обратной совместимости были добавлены копии методов с логикой получения доступного домена внутри.

### Что изменилось

Теперь через `TidAuth` можно определить список доменов, через которые будет проверяться доступность приложения Т-Банка в системе.
По умолчанию используется зашитый список из 3-ех доменов: www.tbank.ru, id.srv-hub.org, ipa.srv-hub.org

## T-ID 1.1.4 -> T-ID 1.2.0

Добавлен новый API для запуска авторизации без параметра `callbackUrl`. Старые методы с `callbackUrl` сохранены для обратной совместимости и помечены как deprecated.

### Что изменилось

Теперь для возврата в ваше приложение используется `redirectUri`, переданный в конструктор `TidAuth`.
Отдельно передавать `callbackUrl` в методы запуска авторизации больше не нужно.

Новый API для открытия приложения Т-Банка передает `redirectUri` в параметре `redirect_uri`.
Для совместимости со старыми версиями МБ SDK временно дублирует этот же `redirectUri` в параметр `callback_url`.
Партнеру не нужно передавать отдельный `callbackUrl`: внешний контракт нового API остается redirectUri-only.
Дублирование `redirectUri` в `callback_url` будет удалено после готовности и достаточной раскатки МБ с поддержкой redirectUri-only контракта.
Для совместимости со старыми версиями приложения Т-Банка deprecated методы с `callbackUrl` сохраняют передачу параметра `callback_url`.

Веб-авторизация теперь открывается через Chrome Custom Tabs. Fallback на WebView отсутствует:
если на устройстве нет браузера с поддержкой Chrome Custom Tabs, SDK возвращает в ваше приложение результат с `auth_status_code=cancelled_by_user`.

Для сценария веб-авторизации миграция на `redirectUri` обязательна: если `redirectUri` не добавлен в SSO или не открывает приложение, авторизация не сможет корректно вернуть результат в приложение.

### Before

```kotlin
val tidAuth = TidAuth(
    context = applicationContext,
    clientId = clientId,
    redirectUri = redirectUri,
)

val intent = tidAuth.createTidAuthIntent(
    callbackUrl = callbackUrl,
    webMode = TidWebMode.Partner,
)
startActivity(intent)
```

### After

```kotlin
val tidAuth = TidAuth(
    context = applicationContext,
    clientId = clientId,
    redirectUri = redirectUri,
)

val intent = tidAuth.createTidAuthIntent(
    webMode = TidWebMode.Partner,
)
startActivity(intent)
```

### Чек-лист перед миграцией

1. Убедитесь, что `redirectUri` добавлен в конфиг SSO для вашего `client_id`.
   Если раньше в конфиге был только `callbackUrl`, необходимо добавить `redirectUri`.
2. Убедитесь, что `redirectUri` открывает ваше приложение через AppLink или DeepLink.
   Раньше эту функцию выполнял `callbackUrl`; после миграции возврат должен работать по `redirectUri`.
3. Если вы явно выбираете способ авторизации, используйте новые методы:
   - `createTBankAppAuthIntent()`
   - `createTidWebAuthIntent(webMode)`

### Deprecated API

Следующие методы сохранены, но помечены как deprecated:

| Before                                                                      | After                                  |
|-----------------------------------------------------------------------------|----------------------------------------|
| `createTidAuthIntent(callbackUrl: Uri, webMode: TidWebMode)`                | `createTidAuthIntent(webMode)`         |
| `createTBankAppAuthIntent(callbackUrl: Uri)`                                | `createTBankAppAuthIntent()`           |
| `createTidWebViewAuthIntent(callbackUrl: Uri, webMode: TidWebMode)`         | `createTidWebAuthIntent(webMode)`      |

## T-ID 1.0.5 -> T-ID 1.1.0

В `ru.tbank.core.tid.TidTokenPayload` поле `refreshToken` теперь `String?`.

Если вы использовали Refresh Token до этого, то вам следует использовать `requireNotNull()` при получении его из `TidTokenPayload`.
А так же явно сообщить нам при интеграции, что вам нужен Refresh Token, чтобы сервер продолжил его выпускать.

### Before
```kotlin
when (tidAuth.getStatusCode(uri)) {
    TidStatusCode.SUCCESS -> {
        tidAuth.getTidTokenPayload(uri)
            ...
            .subscribe({ tokenPayload ->
                saveRefreshToken(tokenPayload.refreshToken)
            }, { ... })
    }
    ...
}
```

### After
```kotlin
when (tidAuth.getStatusCode(uri)) {
    TidStatusCode.SUCCESS -> {
        tidAuth.getTidTokenPayload(uri)
            ...
            .subscribe({ tokenPayload ->
                saveRefreshToken(requireNotNull(tokenPayload.refreshToken))
            }, { ... })
    }
    ...
}
```

## TinkoffID 1.3.0 -> T-ID 1.0.0

Все классы были переименованы в соответствии с новым названием библиотеки:
`TinkoffId` -> `Tid`, `Tinkoff` -> `Tid`
Также, изменилось имя пакета: `ru.tinkoff.core.tinkoffId` -> `ru.tbank.core.tid`

Полная таблица:

| Before                                                 |    | After                                             |
|:-------------------------------------------------------|:--:|:--------------------------------------------------|
| `ru.tinkoff.core.tinkoffId.TinkoffErrorMessage`        | -> | `ru.tbank.core.tid.TidErrorMessage`               |
| `ru.tinkoff.core.tinkoffId.TinkoffLoggingConstants`    | -> | `ru.tbank.core.tid.TidLoggingConstants`           |
| `ru.tinkoff.core.tinkoffId.TinkoffRequestException`    | -> | `ru.tbank.core.tid.TidRequestException`           |
| `ru.tinkoff.core.tinkoffId.TinkoffTokenErrorConstants` | -> | `ru.tbank.core.tid.TidTokenErrorConstants`        |
| `ru.tinkoff.core.tinkoffId.TokenSignOutErrorConstants` | -> | `ru.tbank.core.tid.TidTokenSignOutErrorConstants` |
| `ru.tinkoff.core.tinkoffId.TinkoffIdSignInButton`      | -> | `ru.tbank.core.tid.TidSignInButton`               |
| `ru.tinkoff.core.tinkoffId.TinkoffIdAuth`              | -> | `ru.tbank.core.tid.TidAuth`                       |
| `ru.tinkoff.core.tinkoffId.TinkoffCall`                | -> | `ru.tbank.core.tid.TidCall`                       |
| `ru.tinkoff.core.tinkoffId.TinkoffIdStatusCode`        | -> | `ru.tbank.core.tid.TidStatusCode`                 |
| `ru.tinkoff.core.tinkoffId.TinkoffTokenPayload`        | -> | `ru.tbank.core.tid.TidTokenPayload`               |

Кроме этого, был изменен префикс в названиях ресурсов: `tinkoff_id` -> `tid`

Таблица с примерами:

| Before                            |    | After                    |
|:----------------------------------|:--:|:-------------------------|
| `@drawable/tinkoff_id_close`      | -> | `@drawable/tid_close`    |
| `@color/tinkoff_id_black_text`    | -> | `@color/tid_black_text`  |
| `@string/tinkoff_id_tinkoff_text` | -> | `@string/tid_tbank_text` |
