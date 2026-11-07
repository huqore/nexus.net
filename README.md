<div align="center">

<img src="app/src/main/res/drawable/app_logo.png" alt="NEXUS.net Logo" width="128" height="128" />

# NEXUS.net for Android

**Высокоскоростной клиент нового поколения на базе ядра Xray core и Jetpack Compose (Material Design 3)**

[![Latest Release](https://img.shields.io/badge/Release-v3.1.1-6366f1?style=for-the-badge&logo=android&logoColor=white)](https://github.com/huqore/nexus.net/releases/download/v3.1.1/nexus-net-v3.1.1.apk)
[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9335)-34d399?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Xray Core](https://img.shields.io/badge/Xray_Core-v26.10.29-blue?style=for-the-badge&logo=xray)](https://github.com/XTLS/Xray-core)
[![Telegram Bot](https://img.shields.io/badge/Telegram_Bot-@NEXUSnet__robot-229ED9?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/NEXUSnet_robot)
[![Website](https://img.shields.io/badge/Web_Platform-nexus--net.xyz-0ea5e9?style=for-the-badge&logo=google-chrome&logoColor=white)](https://nexus-net.xyz)

<br/>

[![Скачать APK v3.1.1](https://img.shields.io/badge/📥_Скачать_актуальный_APK-v3.1.1-22c55e?style=for-the-badge&logo=android&logoColor=white)](https://github.com/huqore/nexus.net/releases/download/v3.1.1/nexus-net-v3.1.1.apk)
[![Все релизы](https://img.shields.io/badge/Архив_релизов-GitHub_Releases-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/huqore/nexus.net/releases)

</div>

---

## 📥 Актуальная версия для скачивания

> [!IMPORTANT]
> Для корректной работы и обхода блокировок используйте **только актуальную версию**. Старые версии могут быть заблокированы на уровне протоколов провайдеров РФ.

| Параметр | Значение |
| :--- | :--- |
| **Текущая версия** | **v3.1.1 (Stable)** |
| **Номер сборки (versionCode)** | `57` |
| **Прямая ссылка на APK** | [**nexus-net-v3.1.1.apk**](https://github.com/huqore/nexus.net/releases/download/v3.1.1/nexus-net-v3.1.1.apk) |
| **Размер файла** | `~71.5 MB` (Universal: arm64-v8a, armeabi-v7a, x86_64, x86) |
| **Контрольная сумма SHA-256** | `be0b63e7870969fb079ab7c6bd4b2502b9052a0d201e9ce58a22087addcbb066` |
| **Системные требования** | Android 8.0 (Oreo, API 26) и новее (включая Android 15 / API 35) |

### 🚀 Что нового в версии 3.1.1:
- **Совместимость с RemnaWave**: полная поддержка обновленных профилей и параметров подписки.
- **Оптимизация WL Bypass Routing**: надежный обход белых списков и ускоренная маршрутизация.
- **Обход DPI для автообновлений**: многоуровневая доставка манифестов обновлений через CDN-зеркала (jsDelivr, ungh.cc, BFF proxy) при блокировках GitHub провайдерами.
- **Стабильность туннеля**: улучшенная обработка сетевых переключений (Wi-Fi ↔ LTE) и быстрый реконнект.

---

## ⚡ Ключевые возможности

* **Передовые протоколы обхода блокировок**:
  * Поддержка **VLESS + Reality + XHTTP**, Shadowsocks, Trojan и VMess (транспорты: ws, grpc, httpupgrade, h2, tcp).
  * Официальное ядро **Xray core v26.10.29** со встроенным TUN-интерфейсом (`libhev-socks5-tunnel.so`), обеспечивающее минимальное энергопотребление и максимальную скорость.
* **Синхронизация в 1 клик с экосистемой**:
  * Интеграция с Telegram-ботом [@NEXUSnet_robot](https://t.me/NEXUSnet_robot) и WebApp.
  * Автоматическая загрузка серверов, лимитов трафика, срока действия подписки и ссылок поддержки.
* **Раздельное туннелирование (Split Tunneling)**:
  * Гибкая настройка: пускать через VPN только выбранные приложения или исключать российские сервисы (банки, Госуслуги, маркетплейсы).
* **Мониторинг сети в реальном времени**:
  * Быстрый замер задержки серверов (⚡) и тест скорости прямо с главного экрана.
  * Точный учёт входящего и исходящего трафика на уровне UID системы.
* **Строгий дизайн Material Design 3 (Material You)**:
  * Глубокая темная тема, чистые тональные поверхности, отсутствие визуального мусора.
  * Выбор акцентных цветов, смена иконок приложения на рабочем столе и деликатный тактильный отклик (haptic feedback).

---

## 📖 Быстрый старт

1. **Установите приложение**:
   Скачайте актуальный файл [**nexus-net-v3.1.1.apk**](https://github.com/huqore/nexus.net/releases/download/v3.1.1/nexus-net-v3.1.1.apk) и разрешите установку из доверенного источника.
2. **Получите подписку**:
   Запустите Telegram-бота [@NEXUSnet_robot](https://t.me/NEXUSnet_robot) или откройте Mini-App на [nexus-net.xyz](https://nexus-net.xyz).
3. **Подключитесь**:
   Вставьте ссылку подписки или авторизуйтесь через Telegram — сервера загрузятся мгновенно. Нажмите центральную кнопку подключения.

---

## 🛠️ Сборка из исходников (Developer Guide)

Проект использует современный стек: **Kotlin + Jetpack Compose + Gradle KTS**.

### Требования к окружению:
* **JDK:** OpenJDK 17
* **Android SDK:** Platform SDK 35 (`compileSdk = 35`, `minSdk = 26`)
* **Gradle:** 8.x / 9.x (в комплекте через `./gradlew`)

### Сборка релизного APK:
```bash
git clone https://github.com/huqore/nexus.net.git
cd nexus.net
./gradlew assembleRelease
```
> *Ядро Xray core (`libs/libv2ray.aar`, ~65 МБ) загружается автоматически при первом запуске сборки.*  
> Готовый универсальный APK находится в `app/build/outputs/apk/release/app-release-unsigned.apk` (или `apks/release/`).

---

## 💬 Поддержка и контакты

* 🤖 **Telegram-бот:** [@NEXUSnet_robot](https://t.me/NEXUSnet_robot)
* 🆘 **Служба поддержки:** [@NEXUS_net_support](https://t.me/NEXUS_net_support)
* 🌐 **Официальный сайт:** [nexus-net.xyz](https://nexus-net.xyz)
* 👨‍💻 **Архитектор проекта:** [@huqore](https://t.me/huqore)
