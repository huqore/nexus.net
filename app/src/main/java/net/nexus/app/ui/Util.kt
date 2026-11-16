package net.nexus.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val countryNames = mapOf(
    "FINLAND" to "FI", "GERMANY" to "DE", "SWITZERLAND" to "CH", "NETHERLANDS" to "NL", "RUSSIA" to "RU",
    "SWEDEN" to "SE", "FRANCE" to "FR", "POLAND" to "PL", "TURKEY" to "TR", "KAZAKHSTAN" to "KZ",
    "USA" to "US", "UNITED" to "US", "UK" to "GB", "BRITAIN" to "GB", "JAPAN" to "JP", "LATVIA" to "LV",
    "ESTONIA" to "EE", "LITHUANIA" to "LT", "ARMENIA" to "AM", "GEORGIA" to "GE", "SINGAPORE" to "SG",
)

private fun codeToFlag(code: String): String =
    code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

/** Best-effort flag for a server name: an emoji already in the name, "AUTO", an ISO code ("FI", "CH v2") or a country name. */
fun flagFor(name: String): String {
    // flag emoji already present (two regional indicator symbols)
    val cps = name.codePoints().toArray()
    for (i in 0 until cps.size - 1) {
        if (cps[i] in 0x1F1E6..0x1F1FF && cps[i + 1] in 0x1F1E6..0x1F1FF) {
            return String(Character.toChars(cps[i])) + String(Character.toChars(cps[i + 1]))
        }
    }
    val tokens = Regex("[A-Za-z]{2,}").findAll(name).map { it.value.uppercase() }.toList()
    if (tokens.any { it == "AUTO" }) return "🇺🇳" // UN flag, like the reference UI
    val iso = Locale.getISOCountries().toSet()
    tokens.firstOrNull { it.length == 2 && it in iso }?.let { return codeToFlag(it) }
    tokens.firstNotNullOfOrNull { countryNames[it] }?.let { return codeToFlag(it) }
    return "🌐" // globe
}

/** Country of the node behind LTE host #[n]: LTE #1 — Germany, LTE #2 — Finland. */
fun lteCountry(n: String): String? = when (n) {
    "1" -> "DE"
    "2" -> "FI"
    else -> null
}

/** Strips leading country flag emojis from server name so it's not duplicated beside the FlagBox. */
fun cleanServerName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return name

    var i = 0
    val len = trimmed.length
    var strippedAny = false

    while (i < len) {
        val cp = trimmed.codePointAt(i)
        val charCount = Character.charCount(cp)

        // Regional Indicator Symbols: 0x1F1E6 (A) .. 0x1F1FF (Z)
        if (cp in 0x1F1E6..0x1F1FF) {
            i += charCount
            strippedAny = true
        } else if (cp == 0x1F3F4 || (cp in 0xE0061..0xE007F) || cp == 0xFE0F || cp == 0x200D) {
            i += charCount
            strippedAny = true
        } else if (strippedAny && Character.isWhitespace(cp)) {
            i += charCount
        } else {
            break
        }
    }

    val result = trimmed.substring(i).trimStart()
    val base = if (result.isNotEmpty()) result else trimmed
    val lower = base.lowercase()

    // 1. LTE / WL bypass: the country of the node behind each LTE host (owner's setup)
    Regex("""lte\s*#?(\d+)\b""", RegexOption.IGNORE_CASE).find(lower)?.let { m ->
        val n = m.groupValues[1]
        val lte = "LTE #$n"
        return when (lteCountry(n)) {
            "DE" -> AppLang.t("Германия", "Germany") + " " + lte
            "FI" -> AppLang.t("Финляндия", "Finland") + " " + lte
            else -> lte
        }
    }

    // 2. Strip redundant "WI-FI", "Wi-Fi", "wifi" suffix/prefix and separators
    val strippedWifi = base.replace(Regex("""(?i)\s*[|\-–—]?\s*wi-?fi\b"""), "").trim()
    val name = if (strippedWifi.isNotEmpty()) strippedWifi else base
    // Server names come from the panel in Russian: translate country names for the English UI.
    return if (AppLang.isRu()) name else COUNTRY_EN.entries.fold(name) { acc, (ru, en) -> acc.replace(ru, en, ignoreCase = true) }
}

private val COUNTRY_EN = linkedMapOf(
    "Германия" to "Germany", "Финляндия" to "Finland", "Нидерланды" to "Netherlands", "Швеция" to "Sweden",
    "Польша" to "Poland", "Франция" to "France", "Великобритания" to "United Kingdom", "США" to "USA",
    "Турция" to "Turkey", "Казахстан" to "Kazakhstan", "Латвия" to "Latvia", "Эстония" to "Estonia",
    "Литва" to "Lithuania", "Австрия" to "Austria", "Швейцария" to "Switzerland", "Россия" to "Russia",
    "Япония" to "Japan", "Сингапур" to "Singapore", "Канада" to "Canada", "Испания" to "Spain", "Италия" to "Italy",
)

/**
 * App language for text built in code: the in-app choice, else the system language.
 * Used where there is no Context (formatters, managers); UI text should use string resources.
 */
object AppLang {
    fun isRu(): Boolean = runCatching {
        val tags = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isNotBlank()) tags.startsWith("ru", ignoreCase = true) else Locale.getDefault().language == "ru"
    }.getOrDefault(true)

    fun t(ru: String, en: String): String = if (isRu()) ru else en

    /** "1.25 GB" / "1.25 ГБ" with the given decimals for GB and up. */
    fun bytes(bytes: Long, gbDecimals: Int = 2): String {
        val ru = isRu()
        val units = if (ru) listOf("Б", "КБ", "МБ", "ГБ", "ТБ") else listOf("B", "KB", "MB", "GB", "TB")
        if (bytes <= 0L) return "0 " + units[2]
        var v = bytes.toDouble(); var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        val digits = when { i >= 3 -> gbDecimals; i == 2 -> 1; else -> 0 }
        return String.format(Locale.US, "%." + digits + "f %s", v, units[i])
    }

    fun speed(bytesPerSec: Long): String {
        val ru = isRu()
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec " + (if (ru) "Б/с" else "B/s")
            bytesPerSec < 1024 * 1024 -> String.format(Locale.US, "%.0f %s", bytesPerSec / 1024f, if (ru) "КБ/с" else "KB/s")
            else -> String.format(Locale.US, "%.1f %s", bytesPerSec / (1024f * 1024f), if (ru) "МБ/с" else "MB/s")
        }
    }
}

fun fmtBytes(bytes: Long, ru: Boolean, locale: Locale): String {
    val units = if (ru) listOf("Б", "КБ", "МБ", "ГБ", "ТБ") else listOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble(); var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    val digits = if (i >= 3) 2 else if (i >= 1) 1 else 0
    return String.format(locale, "%.${digits}f %s", v, units[i])
}

fun fmtSpeed(bytesPerSec: Long, ru: Boolean, locale: Locale): String {
    val mb = bytesPerSec / 1048576.0
    return if (mb >= 1) String.format(locale, "%.1f %s/s", mb, if (ru) "МБ" else "MB")
    else String.format(locale, "%.0f %s/s", bytesPerSec / 1024.0, if (ru) "КБ" else "KB")
}

fun fmtDuration(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun fmtDate(millis: Long, locale: Locale): String =
    SimpleDateFormat("dd.MM.yy, HH:mm", locale).format(Date(millis))

fun protoLabel(p: String): String = when (p) {
    "vless" -> "VLESS"
    "vmess" -> "VMESS"
    "trojan" -> "TROJAN"
    "shadowsocks" -> "Shadowsocks"
    else -> p.uppercase()
}
