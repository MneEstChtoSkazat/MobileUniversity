# Практические работы 2–4. «Мой кошелёк»: интерфейс, база данных, карта и API приложения «Мой кошелёк» (Kotlin + Jetpack Compose)

Четыре экрана по макету Figma: главная («Мой кошелёк»), редактор записи, карта и настройки.
Данные кошелька хранятся в локальной БД SQLite (ПР 3). Карта — OpenStreetMap, курсы — открытые API (ПР 4).

## Как подключить к своему проекту

1. В Android Studio: **New Project → Empty Activity** (шаблон с Compose).
   Name: `Wallet`, Package name: `com.example.wallet`, Minimum SDK: API 24.
2. Скопируй содержимое папки `app/src/main/` из этого архива в `app/src/main/` проекта
   с заменой файлов (`MainActivity.kt` заменится, появятся `WalletApp.kt`, папки `data`, `ui`
   и картинка `res/drawable/map_krasnoyarsk.png`).
3. Папку `ui/theme` из шаблона можно оставить или удалить: приложение её не использует.
4. В `app/build.gradle.kts` в блок `dependencies { ... }` добавь строку:

   ```kotlin
   implementation("androidx.navigation:navigation-compose:2.7.7")
   ```

5. Нажми **Sync Now**, затем **Run**.

Если у проекта другое имя пакета, замени `com.example.wallet` в строках `package` и `import`
во всех файлах (Android Studio сделает это через Refactor → Rename).

## Карта

Сейчас в `res/drawable/map_krasnoyarsk.png` лежит заглушка, вырезанная из скриншота макета.
Для лучшего качества экспортируй карту из Figma: выдели слой с картой → справа **Export**
→ `3x`, PNG, и положи файл под тем же именем `map_krasnoyarsk.png`.

## Практическая работа 4. Карта и внешние API

### Что нужно добавить в проект (один раз)

1. В `app/build.gradle.kts` в блок `dependencies { ... }`:

   ```kotlin
   implementation("org.osmdroid:osmdroid-android:6.1.20")
   ```

2. В `app/src/main/AndroidManifest.xml` **перед** тегом `<application>`:

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

3. Sync Now. Остальные зависимости не нужны: запросы идут через `HttpURLConnection`, JSON разбирается
   встроенным `org.json`. При первом запуске после обновления с ПР 3 база пересоздаётся (версия 2),
   баланс сбрасывается до 100 000 ₽.

### Карта (пункт 1)

`ui/screens/MapScreen.kt`: карта OpenStreetMap через библиотеку osmdroid, ключ не нужен.
Три метки пунктов обмена Красноярска (`data/SampleData.kt`). По нажатию на метку открывается
описание (название, адрес, режим работы). Те же пункты показаны карточками внизу экрана:
нажатие на карточку открывает метку и перемещает к ней карту. Пункты демонстрационные,
координаты приблизительные.

### API курсов (пункт 2)

`data/RatesApi.kt`, оба API бесплатные и без ключа:

| Что | API | Адрес |
|---|---|---|
| Обычные валюты (≈55) | Банк России | `https://www.cbr-xml-daily.ru/daily_json.js` |
| Криптовалюты (топ-30) | CoinGecko | `https://api.coingecko.com/api/v3/coins/markets?vs_currency=rub` |

Курсы загружаются при запуске приложения и по кнопке «Обновить курсы». В редакторе при
покупке («+») валюта выбирается из выпадающего списка, курс подставляется из API (только чтение),
показываются источник и стоимость покупки. Если один из API недоступен, список строится
из второго, а ошибка выводится под списком. У CoinGecko бесплатный лимит запросов:
при ошибке 429 подождите минуту, либо впишите Demo API key в `COINGECKO_API_KEY` в `RatesApi.kt`.

Руководство пользователя: файл `USER_GUIDE.md`.

## Практическая работа 3. Локальная база данных

Используется встроенный SQLite (`SQLiteOpenHelper`), новые зависимости в Gradle не нужны.
Если Android Studio подсвечивает красным `viewModelScope`, добавь в `dependencies`:
`implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")` и нажми Sync Now.

Файлы:

| Файл | Что делает |
|---|---|
| `data/Asset.kt` | модель актива и результат покупки |
| `data/WalletDbHelper.kt` | создание БД: таблицы `assets` и `profile` (баланс), стартовые данные |
| `data/WalletRepository.kt` | CRUD-методы и покупка (транзакция) |
| `data/WalletViewModel.kt` | запуск операций в фоне и состояние для экранов |

CRUD в интерфейсе:

| Операция | Где |
|---|---|
| Create | «+» на главной: форма покупки (валюта из списка, сумма, курс по API), «Купить» |
| Read | список и суммы «Активы» / «Баланс» на главной |
| Update | нажатие на запись, правка полей, «Сохранить» |
| Delete | «Продать» в редакторе: запись удаляется, рубли возвращаются на баланс |

Симуляция покупки: валюта выбирается из выпадающего списка, курс берётся из API, иконка новой валюты случайная. При нажатии «Купить» баланс уменьшается на `сумма × курс`, а запись добавляется в
кошелёк, поэтому «Активы» растут. Если такая валюта уже есть, количество складывается, а курс
пересчитывается как средневзвешенный. Всё выполняется в одной транзакции; при нехватке средств
покупка отклоняется. «Продать» продаёт актив целиком по его курсу. Стартовый баланс 100 000 ₽.

База хранится в `/data/data/com.example.wallet/databases/wallet.db`.
Чтобы сбросить данные: удалить приложение или `adb shell pm clear com.example.wallet`.

## Сборка APK из командной строки

Нужны JDK 17+, Android SDK и интернет (скачиваются зависимости Gradle, в том числе osmdroid) (его ставит Android Studio). Команды выполняются в папке проекта
(там, где лежит `gradlew`).

1. Проверить Java: `java -version`.
2. Если Gradle не находит SDK, создать файл `local.properties` со строкой пути к SDK:
   `sdk.dir=C\:\\Users\\ИМЯ\\AppData\\Local\\Android\\Sdk` (Windows) или
   `sdk.dir=/home/ИМЯ/Android/Sdk` (Linux/macOS), либо задать переменную `ANDROID_HOME`.
3. Собрать debug-APK:

```
gradlew.bat clean assembleDebug        (Windows)
./gradlew clean assembleDebug          (Linux, macOS; при ошибке доступа сначала chmod +x gradlew)
```

4. Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`.
5. Установка на телефон или эмулятор с включённой отладкой по USB:

```
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release-сборка (`gradlew assembleRelease`) требует подписи ключом, для учебной работы хватает debug.

## Навигация (переходы из прототипа)

| Откуда | Действие | Куда |
|---|---|---|
| Главная | Нажатие на запись | Редактор (редактирование, удаление, покупка) |
| Главная | «+» | Редактор (новая запись) |
| Главная | Иконка человечка | Карта (OpenStreetMap) |
| Главная | Шестерёнка | Настройки |
| Редактор | «Сохранить» или стрелка | Главная |
| Карта, Настройки | Стрелка назад | Главная |
