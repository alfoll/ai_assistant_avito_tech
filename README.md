
# AI Assistant Avito Tech

Тестовое задание на стажировку Android-разработки в Авито.

Приложение представляет собой AI-ассистента в формате чата на базе GigaChat.  
Поддерживаются авторизация, список чатов, переписка с ассистентом, профиль пользователя, смена темы и локальное сохранение данных.

## Реализовано

- авторизация и регистрация через Firebase Authentication;
- автологин;
- список чатов с локальным хранением в Room;
- поиск по чатам;
- пагинация списка чатов через Paging 3;
- чат с реальным GigaChat;
- сохранение истории сообщений;
- повторная отправка сообщения при ошибке;
- share ответа ассистента;
- профиль пользователя;
- смена аватара;
- отображение количества использованных токенов;
- светлая/тёмная тема с сохранением через DataStore;
- навигация через NavigationDrawer.

## Стек

- Kotlin
- Jetpack Compose
- Navigation Compose
- Firebase Authentication
- Cloud Firestore
- Room
- Paging 3
- DataStore
- Retrofit
- OkHttp
- Kotlin Serialization
- Coil 3

## Что нужно для запуска

### 1. `google-services.json`
Файл `google-services.json` **не включён** в репозиторий.

Нужно положить свой файл в:

```text
app/google-services.json
````

### 2. `local.properties`

В `local.properties` нужно добавить ключ GigaChat:

```properties
GIGACHAT_AUTH_KEY=ваш_ключ
```

Пример:

```properties
sdk.dir=/Users/username/Library/Android/sdk
GIGACHAT_AUTH_KEY=ваш_ключ
```

`local.properties` в репозиторий не добавляется.

### 3. Firebase Console

Для запуска проекта должны быть включены:

* **Firebase Authentication**

  * Sign-in method → **Email/Password**
* **Cloud Firestore**

### 4. Firestore Rules

Для профиля используется коллекция `user_profiles`.

Минимальные правила:

```txt
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Важный нюанс по GigaChat

Для корректного HTTPS-соединения с GigaChat в проект включены:

* `app/src/main/res/xml/network_security_config.xml`
* `app/src/main/res/raw/russian_trusted_root_ca.crt`

Эти файлы нужны для корректной работы запросов к GigaChat на Android.

## Важный нюанс по аватару

Аватар пользователя хранится **в Firebase через Cloud Firestore**, а не через Firebase Storage.

Такое решение выбрано, потому что Firebase Storage требует billing / Blaze plan.
В текущей реализации изображение сохраняется в документ пользователя в коллекции `user_profiles`.

## Сборка и запуск

1. Открыть проект в Android Studio.
2. Добавить `google-services.json` в `app/`.
3. Добавить `GIGACHAT_AUTH_KEY` в `local.properties`.
4. Выполнить **Sync Project with Gradle Files**.
5. Запустить приложение.

## Что не включено в репозиторий

В репозиторий не добавляются:

* `local.properties`
* `google-services.json`
* ключ GigaChat
* build-артефакты
* keystore / jks

## Проверка основных сценариев

Рекомендуется проверить:

* регистрация нового пользователя;
* вход существующего пользователя;
* автологин;
* создание нового чата;
* поиск по чатам;
* пагинация списка;
* отправка сообщений в GigaChat;
* сохранение истории чата;
* retry при ошибке сети;
* share ответа ассистента;
* смена аватара;
* сохранение темы после перезапуска;
* выход из аккаунта.

## Ограничения текущей реализации

* вход через Google не реализован;
* генерация изображений не реализована;
* редактирование имени пользователя не реализовано;
* аватар хранится в Firestore, а не в Firebase Storage.
