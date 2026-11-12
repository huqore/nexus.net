<div align="center">

<img src="app/src/main/res/drawable/app_logo.png" alt="NEXUS.net" width="104" />

# NEXUS.net

**Быстрый VPN-клиент для Android на базе Xray Core**

<a href="https://github.com/huqore/nexus.net/releases/latest"><img src="https://img.shields.io/github/v/release/huqore/nexus.net?style=for-the-badge&label=релиз&color=7657e8" alt="Последний релиз"></a>
<a href="https://github.com/huqore/nexus.net/releases"><img src="https://img.shields.io/github/downloads/huqore/nexus.net/total?style=for-the-badge&label=загрузки&color=3478c9" alt="Загрузки"></a>
<img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+">

<br><br>

<a href="https://github.com/huqore/nexus.net/releases/latest"><img src="https://img.shields.io/badge/⬇_Скачать_APK-7657e8?style=for-the-badge" height="38" alt="Скачать APK"></a>

<br>

[Сайт](https://nexus-net.xyz) · [Telegram-бот](https://t.me/NEXUSnet_robot) · [Поддержка](https://t.me/NEXUS_net_support)

<br>

<img src="docs/screens/fluent.png" width="200" alt="Fluent">&nbsp;
<img src="docs/screens/material-you.png" width="200" alt="Material You">&nbsp;
<img src="docs/screens/liquid-glass.png" width="200" alt="Liquid Glass">&nbsp;
<img src="docs/screens/custom-theme.png" width="200" alt="Своя тема">

<sub>Fluent · Material You · Liquid Glass · Своя тема</sub>

</div>

## ✨ Что нового в 4.0

- 🎨 **Три стиля оформления** — Fluent, Material You Expressive и Liquid Glass с живыми бликами.
- 🌈 **Своя тема** — цвета и падающие частицы: сакура, снег, пепел с угольками. Плюс AMOLED и светлая тема.
- 👆 **Живая нижняя панель** — зажмите и ведите пальцем между вкладками.
- ⚡ **Стабильнее** — автовыбор держит один сервер, подключение быстрее, меньше расход батареи.

[Все изменения →](https://github.com/huqore/nexus.net/releases/latest)

## 🚀 Возможности

| 🔌 Подключение | 🎛 Удобство |
| --- | --- |
| VLESS, VMess, Trojan, Shadowsocks | Раздельная маршрутизация приложений |
| Ссылки, Xray JSON и подписки | Пинг серверов и статистика трафика |
| Вход и синхронизация через Telegram | Виджет и плитка быстрых настроек |
| Автовыбор лучшего сервера | Темы, стили и смена иконки |

## 📲 Установка

1. Скачайте APK со [страницы релиза](https://github.com/huqore/nexus.net/releases/latest).
2. Установите и откройте приложение.
3. Войдите через Telegram или добавьте ссылку / подписку — и подключайтесь в одно касание.

Обновления приходят прямо в приложении.

<details>
<summary><b>🛠 Сборка из исходников</b></summary>

<br>

Нужны **JDK 17** и **Android SDK Platform 35**. Укажите путь к SDK в `local.properties` (шаблон — [`local.properties.example`](local.properties.example)):

```bash
./gradlew assembleDebug      # Windows: gradlew.bat assembleDebug
```

Release-сборка — `assembleRelease`. Для подписи своим ключом задайте `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; без них используется debug-ключ.

Если `app/libs/libv2ray.aar` нет, Gradle сам скачает Xray Core AAR `v26.10.29`.

**Стек:** Kotlin · Jetpack Compose · Material 3 · Android VpnService · Xray Core

</details>

<div align="center">
<sub>© NEXUS.net · <a href="https://nexus-net.xyz">nexus-net.xyz</a></sub>
</div>
