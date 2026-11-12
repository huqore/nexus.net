<div align="center">

  <img src="app/src/main/res/drawable/app_logo.png" alt="NEXUS.net" width="112" />

  # NEXUS.net для Android

  **Быстрый и удобный VPN-клиент на базе Xray Core**

  <p>
    <a href="https://github.com/huqore/nexus.net/releases/latest"><img src="https://img.shields.io/badge/релиз-v4.0.0-7657e8?style=for-the-badge" alt="Последний релиз v4.0.0"></a>
    <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 и новее"></a>
    <a href="https://github.com/XTLS/Xray-core"><img src="https://img.shields.io/badge/Xray_Core-v26.10.29-3478c9?style=for-the-badge" alt="Xray Core v26.10.29"></a>
  </p>

  <p>
    <a href="https://github.com/huqore/nexus.net/releases/latest">Скачать APK</a> ·
    <a href="https://nexus-net.xyz">Сайт</a> ·
    <a href="https://t.me/NEXUSnet_robot">Telegram-бот</a> ·
    <a href="https://t.me/NEXUS_net_support">Поддержка</a>
  </p>

</div>

---

## О приложении

NEXUS.net — Android-клиент для подключения к VPN-серверам через Xray. Добавьте серверную ссылку или подписку, выберите нужный режим маршрутизации и подключитесь в одно касание. Подписку также можно синхронизировать через Telegram.

**Актуальный релиз: v4.0.0** · Android 8.0+ · `versionCode 60`

## Возможности

| Подключение | Контроль и удобство |
| --- | --- |
| VLESS, VMess, Trojan и Shadowsocks | Раздельная маршрутизация приложений |
| Ссылки серверов, Xray JSON и подписки | Проверка задержки и статистика трафика |
| Синхронизация подписки через Telegram | Виджет и плитка быстрых настроек |
| Ядро Xray Core | Смена темы оформления и иконки приложения |

## Установка

Скачайте APK со [страницы последнего релиза](https://github.com/huqore/nexus.net/releases/latest). После установки добавьте ссылку на сервер или подписку либо войдите через Telegram.

## Сборка из исходников

Понадобятся **JDK 17** и **Android SDK Platform 35**. Укажите путь к SDK в `local.properties` (шаблон — [`local.properties.example`](local.properties.example)), затем запустите:

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

Чтобы собрать release APK, используйте задачу `assembleRelease`. Для подписи собственным ключом задайте переменные окружения `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` и `KEY_PASSWORD`. Без них сборка подписывается debug-ключом.

Если `app/libs/libv2ray.aar` отсутствует, Gradle скачает Xray Core AAR версии `v26.10.29` из GitHub при конфигурации проекта.

## Технологии

`Kotlin` · `Jetpack Compose` · `Material 3` · `Android VpnService` · `Xray Core`

## Ссылки

- [Все релизы](https://github.com/huqore/nexus.net/releases)
- [Сайт NEXUS.net](https://nexus-net.xyz)
- [Telegram-бот](https://t.me/NEXUSnet_robot)
- [Поддержка](https://t.me/NEXUS_net_support)
