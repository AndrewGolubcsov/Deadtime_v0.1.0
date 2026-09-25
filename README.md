# Дедтайм

Android-приложение: привычки + таймер фокуса по категориям + очки, монеты, уровни, серии, ачивки.
Все данные только на телефоне (Room + DataStore), без аккаунтов.

Стек: Kotlin 2.1, Jetpack Compose (BOM 2024.12), Room 2.6, DataStore, WorkManager, Glance (7 виджетов).
minSdk 26, targetSdk 35. Пакет `ru.dedtime.app`.

## Сборка через GitHub Actions

Секреты не нужны. Ключ подписи `app/dedtime-release.jks` лежит в репозитории (пароль `dedtime123`).
Залей проект в ветку `main` — сборка запустится сама. APK: Actions → последний запуск → Artifacts → `dedtime-apk`.

Не удаляй и не меняй `dedtime-release.jks`: без него обновления не встанут поверх установленного приложения.
Если репозиторий публичный, ключ видят все — для личного приложения это нормально, для Google Play сделай отдельный приватный ключ.

## Обновления без потери данных

- Всегда тот же ключ подписи, applicationId не менять.
- `versionCode` = номер запуска CI, растёт сам. `versionName` — в `gradle.properties`.
- Схема Room экспортируется в `app/schemas/` — коммить её. При изменении таблиц: поднять `AppDb.VERSION` и написать миграцию. `fallbackToDestructiveMigration` не использовать.
- Резервная копия JSON: Настройки → Данные.

## Локально (Android Studio)

Открой папку проекта. Если Studio попросит Gradle wrapper — согласись (или `gradle wrapper --gradle-version 8.11.1`). Debug-сборка подписывается отладочным ключом — не ставь её поверх релизной.

## Отличия от ТЗ

- Вместо Hilt — ручное внедрение через `DedtimeApp` (меньше генерации кода, проще сборка).
- Тексты интерфейса прямо в коде, а не в strings.xml.
- Автокопия хранит 4 последних файла в выбранной папке (SAF).

## Структура

```
data/     Entities, AppDao, AppDb, Repository (все операции + бэкап), SettingsStore
domain/   Engine — вся экономика (XP, монеты, уровни, серии, задания, ачивки). Чистый Kotlin, покрыт тестами
timer/    TimerService — foreground-уведомление, пауза/стоп, «Ты тут?»
widget/   7 Glance-виджетов
work/     обновление виджетов, автокопия
ui/       экраны Compose
```
