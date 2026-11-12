package net.nexus.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import net.nexus.app.MainViewModel
import net.nexus.app.data.AppTheme
import net.nexus.app.data.DesignStyle

/** Full-screen harnesses (debug only): real screens with a real ViewModel under Robolectric. */
@Composable
private fun ScreenHarness(style: DesignStyle, tab: Tab, theme: AppTheme = AppTheme.BASE, sub: SettingsSubscreen = SettingsSubscreen.MAIN, accent: Int = 0xFF8B5CF6.toInt(), bg: Int = 0xFF0A0812.toInt(), amoled: Boolean = false, light: Boolean = false) {
    val app = LocalContext.current.applicationContext as Application
    val vm = remember {
        // Seed demo servers so the Servers screen is reviewable (WL vs Wi-Fi split included).
        fun s(name: String, ping: Int) = net.nexus.app.data.Server(
            id = name, name = name, protocol = "vless", network = "xhttp", security = "reality", source = "LINK", pingMs = ping,
        )
        net.nexus.app.data.Store(app).saveProviders(
            listOf(
                net.nexus.app.data.Provider(
                    id = "demo", name = "NEXUS", url = "https://example.invalid/sub",
                    servers = listOf(
                        s("🇵🇱 Польша Wi-Fi", 118), s("🇩🇪 Германия Wi-Fi", 38), s("🇫🇮 Финляндия Wi-Fi", 45),
                        s("LTE #1 | WL bypass", 64), s("LTE #2 | WL bypass", 71),
                    ),
                    total = 200L * 1024 * 1024 * 1024, download = 37L * 1024 * 1024 * 1024,
                )
            )
        )
        MainViewModel(app)
    }
    NexusTheme(fontScale = 1f, appTheme = theme, designStyle = style, customAccent = accent, customBg = bg, amoled = amoled, lightMode = light) {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                Tab.CONNECT -> ConnectScreen(vm, onToggle = {}, onAddServers = {})
                Tab.SERVERS -> ServersScreen(vm)
                Tab.SETTINGS -> SettingsScreen(vm, initialSubscreen = sub)
            }
            FloatingNavBar(selected = tab, onSelect = {}, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Preview(name = "S Connect F", widthDp = 400, heightDp = 860) @Composable fun S_Connect_F() = ScreenHarness(DesignStyle.FLUENT, Tab.CONNECT)
@Preview(name = "S Connect M", widthDp = 400, heightDp = 860) @Composable fun S_Connect_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT)
@Preview(name = "S Connect G", widthDp = 400, heightDp = 860) @Composable fun S_Connect_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.CONNECT)
@Preview(name = "S Servers F", widthDp = 400, heightDp = 860) @Composable fun S_Servers_F() = ScreenHarness(DesignStyle.FLUENT, Tab.SERVERS)
@Preview(name = "S Servers M", widthDp = 400, heightDp = 860) @Composable fun S_Servers_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SERVERS)
@Preview(name = "S Servers G", widthDp = 400, heightDp = 860) @Composable fun S_Servers_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SERVERS)
@Preview(name = "S Settings F", widthDp = 400, heightDp = 2200) @Composable fun S_Settings_F() = ScreenHarness(DesignStyle.FLUENT, Tab.SETTINGS)
@Preview(name = "S Settings M", widthDp = 400, heightDp = 2200) @Composable fun S_Settings_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS)
@Preview(name = "S Settings G", widthDp = 400, heightDp = 2200) @Composable fun S_Settings_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SETTINGS)
@Preview(name = "S Appear F", widthDp = 400, heightDp = 2600) @Composable fun S_Appear_F() = ScreenHarness(DesignStyle.FLUENT, Tab.SETTINGS, sub = SettingsSubscreen.APPEARANCE)
@Preview(name = "S Appear M", widthDp = 400, heightDp = 2600) @Composable fun S_Appear_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS, sub = SettingsSubscreen.APPEARANCE)
@Preview(name = "S Appear G", widthDp = 400, heightDp = 2600) @Composable fun S_Appear_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SETTINGS, sub = SettingsSubscreen.APPEARANCE)
@Preview(name = "S Conn M", widthDp = 400, heightDp = 1600) @Composable fun S_Conn_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS, sub = SettingsSubscreen.CONNECTION)
@Preview(name = "S Conn G", widthDp = 400, heightDp = 1600) @Composable fun S_Conn_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SETTINGS, sub = SettingsSubscreen.CONNECTION)
@Preview(name = "S Servers G Winter", widthDp = 400, heightDp = 860) @Composable fun S_Servers_G_Winter() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SERVERS, AppTheme.WINTER)
@Preview(name = "S Settings M White", widthDp = 400, heightDp = 2200) @Composable fun S_Settings_M_White() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS, AppTheme.WHITE_SCREEN)
@Preview(name = "S Connect M Winter", widthDp = 400, heightDp = 860) @Composable fun S_Connect_M_Winter() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT, AppTheme.WINTER)
@Preview(name = "S Servers M Winter", widthDp = 400, heightDp = 860) @Composable fun S_Servers_M_Winter() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SERVERS, AppTheme.WINTER)
@Preview(name = "S Settings M Winter", widthDp = 400, heightDp = 2200) @Composable fun S_Settings_M_Winter() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS, AppTheme.WINTER)
@Preview(name = "S Connect M Short", widthDp = 392, heightDp = 760) @Composable fun S_Connect_M_Short() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT, AppTheme.WINTER)
@Preview(name = "S Connect M Custom", widthDp = 400, heightDp = 860) @Composable fun S_Connect_M_Custom() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT, AppTheme.CUSTOM, accent = 0xFFF97316.toInt(), bg = 0xFF120A06.toInt())
@Preview(name = "S Connect F Custom", widthDp = 400, heightDp = 860) @Composable fun S_Connect_F_Custom() = ScreenHarness(DesignStyle.FLUENT, Tab.CONNECT, AppTheme.CUSTOM, accent = 0xFFF97316.toInt(), bg = 0xFF120A06.toInt())
@Preview(name = "S Servers G Custom", widthDp = 400, heightDp = 860) @Composable fun S_Servers_G_Custom() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SERVERS, AppTheme.CUSTOM, accent = 0xFFF97316.toInt(), bg = 0xFF120A06.toInt())
@Preview(name = "S Settings F CustomLight", widthDp = 400, heightDp = 1200) @Composable fun S_Settings_F_CustomLight() = ScreenHarness(DesignStyle.FLUENT, Tab.SETTINGS, AppTheme.CUSTOM, accent = 0xFF0EA5E9.toInt(), bg = 0xFFF7FAFC.toInt())
@Preview(name = "Custom Editor", widthDp = 400, heightDp = 900) @Composable fun P_CustomEditor() = NexusTheme(fontScale = 1f) {
    CustomThemeEditor(initialAccent = 0xFFF97316.toInt(), initialBg = 0xFF120A06.toInt(), onDismiss = {}, onApply = { _, _, _ -> })
}
@Preview(name = "L Connect F", widthDp = 400, heightDp = 860) @Composable fun L_Connect_F() = ScreenHarness(DesignStyle.FLUENT, Tab.CONNECT, light = true)
@Preview(name = "L Settings F", widthDp = 400, heightDp = 1100) @Composable fun L_Settings_F() = ScreenHarness(DesignStyle.FLUENT, Tab.SETTINGS, light = true)
@Preview(name = "A Connect F", widthDp = 400, heightDp = 860) @Composable fun A_Connect_F() = ScreenHarness(DesignStyle.FLUENT, Tab.CONNECT, amoled = true)
@Preview(name = "L Connect M", widthDp = 400, heightDp = 860) @Composable fun L_Connect_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT, light = true)
@Preview(name = "L Settings M", widthDp = 400, heightDp = 1100) @Composable fun L_Settings_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.SETTINGS, light = true)
@Preview(name = "A Connect M", widthDp = 400, heightDp = 860) @Composable fun A_Connect_M() = ScreenHarness(DesignStyle.MATERIAL3, Tab.CONNECT, amoled = true)
@Preview(name = "L Connect G", widthDp = 400, heightDp = 860) @Composable fun L_Connect_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.CONNECT, light = true)
@Preview(name = "L Settings G", widthDp = 400, heightDp = 1100) @Composable fun L_Settings_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.SETTINGS, light = true)
@Preview(name = "A Connect G", widthDp = 400, heightDp = 860) @Composable fun A_Connect_G() = ScreenHarness(DesignStyle.LIQUID_GLASS, Tab.CONNECT, amoled = true)
@Preview(name = "P Ash", widthDp = 400, heightDp = 700) @Composable fun P_Ash() = NexusTheme(fontScale = 1f) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().androidxBg()) { AshEmbersOverlay() }
}
private fun Modifier.androidxBg(): Modifier = this.then(androidx.compose.ui.Modifier.background(androidx.compose.ui.graphics.Color(0xFF120A06)))
