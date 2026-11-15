package net.nexus.app.data

import android.content.pm.PackageManager

/**
 * Russian apps that should go around the VPN ("Russian apps direct"): banks, government services,
 * Yandex, VK, marketplaces, carriers, transport, streaming, delivery. Many of them refuse to work
 * (or block the account) when they see a VPN or a foreign IP, so they are excluded from the tunnel.
 *
 * Matched by exact package name or by the developer's package prefix, so apps missing from the
 * exact list (new Yandex/VK/bank apps) are caught too. Names not installed are simply ignored.
 */
object RussianApps {

    /** Well-known exact package names. */
    val PACKAGES: Set<String> = setOf(
        // Banks
        "ru.sberbankmobile", "com.idamob.tinkoff.android", "ru.tinkoff.investing", "ru.vtb24.mobilebanking.android",
        "ru.alfabank.mobile.android", "ru.gazprombank.android.mobilebank.app", "ru.raiffeisen.mobile.new",
        "ru.rshb.dbo", "ru.rosbank.android", "ru.psbank.online", "ru.sovcomcard.halva.v1", "ru.mts.money",
        "ru.pochtabank.pochtaapp", "ru.uralsib.mb", "com.openbank", "ru.yoo.money", "ru.akbars.mobile",
        "ru.bspb", "com.yandex.bank", "ru.ozon.fintech.finance",
        // Government
        "ru.rostel", "ru.mos.app", "ru.nalog.www", "ru.fns.lkfl", "ru.emias.patient", "ru.gosuslugi.auto",
        // Yandex
        "com.yandex.searchapp", "com.yandex.browser", "ru.yandex.taxi", "ru.yandex.yandexmaps",
        "ru.yandex.yandexnavi", "ru.yandex.music", "ru.kinopoisk", "ru.beru.android", "ru.foodfox.client",
        "com.yandex.lavka", "ru.yandex.disk", "ru.yandex.mail", "ru.yandex.metro", "ru.yandex.weatherplugin",
        // VK
        "com.vkontakte.android", "com.vk.im", "com.vk.vkvideo", "com.vk.calls", "ru.ok.android",
        "ru.mail.mailapp", "ru.mail.cloud", "ru.zen.android", "ru.vk.store", "ru.oneme.app", "com.uma.musicvk",
        "com.allgoritm.youla",
        // Marketplaces and shops
        "ru.ozon.app.android", "com.wildberries.ru", "com.avito.android", "ru.megamarket.marketplace",
        "ru.instamart", "ru.sbcs.store", "ru.perekrestok.app", "ru.vkusvill", "com.icemobile.lenta.prod",
        "ru.dns.shop.android", "ru.citilink", "ru.mvideo.mobile", "ru.lamoda.app", "ru.goldapple.app",
        "ru.detmir.dmbonus", "ru.sportmaster.app", "ru.letu.ecom",
        // Carriers
        "ru.mts.mymts", "ru.megafon.mlk", "ru.beeline.services", "ru.tele2.mytele2", "ru.rt.lk", "com.yota.android",
        // Transport and maps
        "ru.rzd.pass", "ru.aeroflot.app", "ru.tutu.tutu_app", "ru.dublgis.dgismobile", "ru.mosmetro.metro",
        // Streaming and media
        "ru.ivi.client", "ru.more.play", "ru.rutube.app", "gpm.tnt_premier", "ru.start.androidmobile",
        "ru.rt.video.app.mobile", "ru.kion.kion", "com.zvooq.openplay",
        // Food and delivery
        "ru.dodopizza.app", "ru.samokat.app", "ru.cdek.app", "ru.russianpost.mobileapp",
    )

    /** Developer package prefixes (every app from that developer). Kept specific on purpose. */
    val PREFIXES: List<String> = listOf(
        // Banks
        "ru.sberbank", "ru.sber.", "ru.tinkoff.", "ru.tbank.", "com.idamob.tinkoff", "ru.vtb", "ru.alfabank.",
        "ru.gazprombank.", "ru.raiffeisen.", "ru.rshb.", "ru.rosbank.", "ru.psbank", "ru.sovcom", "ru.pochtabank",
        "ru.mtsbank", "ru.uralsib.", "ru.yoo.", "ru.akbars.", "ru.bspb",
        // Government
        "ru.rostel", "ru.gosuslugi", "ru.mos.", "ru.nalog.", "ru.fns.", "ru.emias", "ru.sfr.",
        // Yandex / VK / Sber ecosystems
        "ru.yandex.", "com.yandex.", "com.vkontakte.", "com.vk.", "ru.vk.", "ru.ok.", "ru.mail.", "ru.zen.",
        "ru.oneme.", "com.zvooq.",
        // Marketplaces and shops
        "ru.ozon.", "com.wildberries.", "com.avito.", "ru.megamarket", "ru.sbermegamarket", "ru.x5.",
        "ru.pyaterochka", "ru.perekrestok", "ru.magnit", "ru.vkusvill", "ru.dns.", "ru.citilink", "ru.mvideo",
        "ru.eldorado", "ru.lamoda", "ru.goldapple", "ru.detmir", "ru.sportmaster", "ru.letu",
        // Carriers
        "ru.mts.", "ru.megafon.", "ru.beeline.", "ru.tele2.", "ru.t2.", "ru.rt.", "com.yota.",
        // Transport, streaming, delivery
        "ru.rzd.", "ru.aeroflot", "ru.tutu.", "ru.dublgis.", "ru.mosmetro", "ru.ivi.", "ru.rutube.", "ru.kion",
        "ru.dodopizza", "ru.samokat", "ru.cdek", "ru.russianpost",
    )

    fun isRussian(pkg: String): Boolean = pkg in PACKAGES || PREFIXES.any { pkg.startsWith(it) }

    /** Installed Russian apps (package names), never including this app. */
    fun installed(pm: PackageManager, selfPackage: String): List<String> = runCatching {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0).asSequence()
            .map { it.packageName }
            .filter { it != selfPackage && isRussian(it) }
            .sorted()
            .toList()
    }.getOrDefault(emptyList())
}
