package net.nexus.app.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LteNamesTest {
    private val locale = Locale.getDefault()
    @Before fun ru() { Locale.setDefault(Locale("ru")) }
    @After fun back() { Locale.setDefault(locale) }

    @Test fun lteOneIsGermanyAndLteTwoIsFinland() {
        assertEquals("Германия LTE #1", cleanServerName("🇪🇺 LTE #1 | WL bypass "))
        assertEquals("Финляндия LTE #2", cleanServerName("🇪🇺 LTE #2 | WL bypass "))
        assertEquals("DE", countryCodeFor("🇪🇺 LTE #1 | WL bypass "))
        assertEquals("FI", countryCodeFor("🇪🇺 LTE #2 | WL bypass "))
    }
}
