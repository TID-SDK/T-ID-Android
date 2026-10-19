Это устаревший Changelog. Новый находится [здесь](../CHANGELOG.md)

## 1.3.0

#### Fixed
#### Changes

- [tinkoff-id] Удалено API для бейджа кнопки `tinkoff_id_badge` MTID-2637
- [tinkoff-id] Текст `Т-Банк` добавляется только при пустом значении `tinkoff_id_title` MTID-2637
- [tinkoff-id] Обновлено лого в компактной версии кнопки MTID-2637

#### Additions

## 1.2.2

#### Fixed

- [tinkoff-id] Исправлена обработка ошибок во флоу авторизации через WebView MTID-1918

#### Changes
#### Additions

## 1.2.1

#### Fixed
#### Changes

- [tinkoff-id] Ребрендинг текста кнопки `TinkoffIdSignInButton` и документации SDK MTID-2154

#### Additions

## 1.2.0

#### Fixed

- [tinkoff-id] Исправлено неверное определение наличия приложения Тинькофф `isTinkoffAppAuthAvailable()` на Android 11 и ниже при не установленном приложении MTID-2165

#### Changes

- [tinkoff-id] Изменен UI в соответствии с новым дизайном: `app:tinkoff_id_style="yellow"` -> `app:tinkoff_id_style="primary"`, новый стиль `"white"` MTID-1956
- [tinkoff-id] Поднятие версии `ssl-trusted-certs` до `1.17.1` MTID-2312
- [tinkoff-id] Новое имя сертификата: `tinkoff_root_cert` -> `tinkoff_root_cert_production` MTID-2312

#### Additions

## 1.1.2

#### Fixed
#### Changes

- [tinkoff-id] Подняты версии compileSdkVersion: 33 -> 34, targetSdkVersion: 33 -> 34, gradle: 7.6.1 -> 8.6, agp: 7.4.2 -> 8.3.0, java: 1.8 -> 17, MTID-1890

#### Additions

## 1.1.1

#### Fixed
#### Changes

- [tinkoff-id] Улучшена логика проверки возможности перейти в приложение Тинькофф `TinkoffIdAuth.isTinkoffAppAuthAvailable()`: добавлена логика проверки верификации AppLink, чтобы не попадать в браузер, когда AppLink не верифицирован MC-12579

#### Additions

## 1.1.0

#### Fixed
#### Changes
#### Additions

- [app-demo], [tinkoff-id] Добавлен флоу авторизации через WebView MC-7723

## 1.0.5

#### Fixed

- [tinkoff-id] Внесены правки в документацию MC-6740

#### Changes

- [tinkoff-id] update versions of target and compile sdk, AGP and Gradle, dependencies MC-9132

#### Additions

- [tinkoff-id] В Intent для запуска авторизации добавлен параметр версии SDK MC-6740

## 1.0.4

#### Fixed
#### Changes
#### Additions
- [tinkoff-id] add self-signed SSL certificates MC-7689

## 1.0.3

#### Fixed
#### Changes
- [app-demo], [tinkoff-id] redesign TinkoffIdSignInButton MC-6146
#### Additions

## 1.0.2

#### Fixed
#### Changes
- [app-demo], [tinkoff-id] bump up API versions (targetSdk and compileSdk to 31) and dependencies versions MC-5659
#### Additions

## 1.0.1

#### Fixed
#### Changes
#### Additions
- Added redirect_uri configuration logic

## 1.0.0

#### Fixed
#### Changes
#### Additions

[app-demo]: ../app-demo
[tinkoff-id]: tinkoff-id
