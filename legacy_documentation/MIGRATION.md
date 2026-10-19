Это устаревший Migration Guide. Новый находится [здесь](../MIGRATION.md)

# Документ по миграции

## 1.1.2 -> 1.2.0

| Before                                     |    | After                                       |
|:-------------------------------------------|:--:|:--------------------------------------------|
| `TinkoffIdSignInButton.ButtonStyle.YELLOW` | -> | `TinkoffIdSignInButton.ButtonStyle.PRIMARY` |
| `app:tinkoff_id_style="yellow"`            | -> | `app:tinkoff_id_style="primary"`            |

В связи с поднятием версии `ssl-trusted-certs` обновилось имя сертификата с `tinkoff_root_cert` на `tinkoff_root_cert_production`.
Нужно обновить `network_security_config.xml`

Before

``` xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="@raw/tinkoff_root_cert" />
            <certificates src="@raw/ministry_of_digital_development_root_cert" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

After

``` xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="@raw/tinkoff_root_cert_production" />
            <certificates src="@raw/ministry_of_digital_development_root_cert" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

## 1.0.6 -> 1.1.0

В связи с добавлением альтернативного способа авторизации через веб Тинькофф с помощью WebView,
переработана логика метода `createTinkoffAuthIntent(callbackUrl: Uri): Intent`. Теперь внутри него, на основе значения
`isTinkoffAppAuthAvailable(): Boolean`, происходит создание Intent для открытия или приложения Тинькофф,
или `TinkoffWebViewAuthActivity` (для прохождения авторизации в вебе).

Рекомендуется использовать `createTinkoffAuthIntent(callbackUrl: Uri): Intent`, чтобы пользователю в любом случае была
доступна авторизации через Тинькофф:

**Before**:

``` kotlin
if (tinkoffPartnerAuth.isTinkoffAuthAvailable()) { 
    val intent = tinkoffPartnerAuth.createTinkoffAuthIntent(callbackUrl)
    startActivity(intent)
} else {
    // The logic of disabling authorization via Tinkoff
}
```

**After**:

``` kotlin
val intent = tinkoffPartnerAuth.createTinkoffAuthIntent(callbackUrl)
startActivity(intent)
```

Изменения в методах класса `TinkoffIdAuth`:

- `createTinkoffAuthIntent(callbackUrl: Uri): Intent` -> `createTinkoffAppAuthIntent(callbackUrl: Uri): Intent`
- `isTinkoffAuthAvailable(): Boolean` -> `isTinkoffAppAuthAvailable(): Boolean`
