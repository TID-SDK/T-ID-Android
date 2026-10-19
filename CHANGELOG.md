## 1.3.2

#### Fixed

#### Changes
- [tid] Bump targetSdk to 36 MMIG-1724
#### Additions

## 1.3.1

#### Fixed

- [tid] Исправлен запуск веб-авторизации через Chrome Custom Tabs, когда на устройстве не выбран браузер по умолчанию

#### Changes
#### Additions

## 1.3.0

#### Fixed
#### Changes

- [tid] Поддержка нескольких доменов для авторизации через приложение Т-Банка и возможность их настройки

#### Additions

## 1.2.0

#### Fixed
#### Changes

- [tid] Методы авторизации с `callbackUrl` помечены как deprecated. Рекомендуется использовать новый API без параметра `callbackUrl`
- [tid] Новый app-auth API временно дублирует `redirectUri` в `callback_url` для совместимости со старыми версиями приложения Т-Банка
- [tid] Веб-авторизация открывается через Chrome Custom Tabs. Fallback на WebView отсутствует
- [tid] Обновлена версия sslTrustedCerts до 1.22.5

#### Additions

- [tid] Добавлены методы `createTidAuthIntent(webMode)`, `createTBankAppAuthIntent()` и `createTidWebAuthIntent(webMode)`

## 1.1.4

#### Fixed

- [tid] Исправлено некорректное поведение при запрете открытия ссылок в приложении от пользователя

#### Changes
#### Additions

## 1.1.3

#### Fixed
#### Changes
#### Additions

- [tid] Добавлена фильтрация по категории при определении доступности приложения Т-Банка для авторизации

## 1.1.2

#### Fixed

- [tid] Исправлена поддержка edge-to-edge в WebView

#### Changes
#### Additions

## 1.1.1

#### Fixed
#### Changes
- [tid] Подняли версии:
    - targetSdkVersion 34 -> 35
    - compileSdkVersion 34 -> 35
    - Gradle -> 8.6 -> 8.14.3
    - androidPluginVersion -> 8.3.0 -> 8.10.1

#### Additions

## 1.1.0

#### Fixed
#### Changes

- [tid] Опциональное получение refreshToken в TidTokenPayload

#### Additions

## 1.0.5

#### Fixed
#### Changes
#### Additions

- [tid] Добавлена возможность подмены хоста для T-ID API

## 1.0.4

#### Additions

- [app-demo] Удалили флаг nonTransitiveRClass

## 1.0.3

#### Additions

- [tid] Добавлен режим WhiteLabel для Web авторизации

## 1.0.2

#### Fixed
#### Changes

- [tid] Обновлена информация о разработчике для POM-файла и ссылка на почту поддержки в README
- [tid] Обновлена ссылка на Issues проекта

#### Additions

## 1.0.1

#### Fixed

- [tid] Исправлен хост для восстановления работы сценария авторизации через веб Т-Банка MTID-3035

#### Changes
#### Additions

## 1.0.0

#### Fixed
#### Changes
#### Additions

- [tid] Первый релиз после полного ребрендинга библиотеки. Подробнее - см. [MIGRATION.md](https://opensource.tbank.ru/mobile-tech/T-ID-Android/-/blob/master/MIGRATION.md) MTID-2575

[app-demo]: app-demo
[tid]: tid
