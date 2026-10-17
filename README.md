# Pulse Visuals (Fabric, Minecraft 1.21.11)

Клиентский визуальный мод в стиле Pulse: плашка, TargetHUD, тёмное меню (Right Shift), пресеты цвета, Hit Effects, Jump Circle, Trail, Full Bright.
Fullbright, Trail, HitEffects.

## Сборка
1. Скачай шаблон на https://fabricmc.net/develop/template/ (MC 1.21.11, Yarn, Java 21)
   и возьми из него папку `gradle/` + файлы `gradlew`, `gradlew.bat`.
   (или установи Gradle и выполни `gradle wrapper`)
2. Положи их в эту папку, открой в IntelliJ IDEA (JDK 21), дождись импорта Gradle.
3. `./gradlew build` -> jar в `build/libs/` (файл без `-sources`).
4. Закинь jar и Fabric API в `.minecraft/mods`.

Если Gradle ругается на версию — открой gradle.properties и поставь актуальную
с https://fabricmc.net/develop/
