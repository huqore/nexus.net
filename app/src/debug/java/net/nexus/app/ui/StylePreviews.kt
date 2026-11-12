package net.nexus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.nexus.app.data.AppTheme
import net.nexus.app.data.DesignStyle
import net.nexus.app.data.Provider
import net.nexus.app.data.Server

private fun srv(name: String, ping: Int) = Server(
    id = name, name = name, protocol = "vless", network = "xhttp", security = "reality", source = "LINK", pingMs = ping,
    outbounds = """[{"protocol":"vless","settings":{"vnext":[{"address":"cdn1.wyverniumx.ru","port":443}]}}]""",
)

private val demoProvider = Provider(
    id = "p", name = "NEXUS", url = "https://example.invalid/sub",
    servers = listOf(
        srv("🇵🇱 Польша Wi-Fi", 118),
        srv("🇩🇪 Германия Wi-Fi", 38),
        srv("🇫🇮 Финляндия Wi-Fi", 45),
        srv("LTE #1 | WL bypass", 64),
        srv("LTE #2 | WL bypass", 71),
    ),
)

@Composable
private fun StyleShowcase(style: DesignStyle, theme: AppTheme = AppTheme.BASE) {
    NexusTheme(fontScale = 1f, appTheme = theme, designStyle = style) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProviderCard(
                    p = demoProvider, selectedId = "🇩🇪 Германия Wi-Fi", refreshing = false, pinging = emptySet(),
                    onToggle = {}, onRefresh = {}, onPingAll = {}, onDeleteProvider = {},
                    onSelect = {}, onDeleteServer = {},
                )
            }
            FloatingNavBar(selected = Tab.SERVERS, onSelect = {}, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Preview(name = "Fluent", widthDp = 380, heightDp = 760)
@Composable
fun PreviewStyleFluent() = StyleShowcase(DesignStyle.FLUENT)

@Preview(name = "Material 3", widthDp = 380, heightDp = 760)
@Composable
fun PreviewStyleMaterial3() = StyleShowcase(DesignStyle.MATERIAL3)

@Preview(name = "Liquid Glass", widthDp = 380, heightDp = 760)
@Composable
fun PreviewStyleLiquidGlass() = StyleShowcase(DesignStyle.LIQUID_GLASS)

@Preview(name = "Liquid Glass Sakura", widthDp = 380, heightDp = 760)
@Composable
fun PreviewStyleLiquidGlassSakura() = StyleShowcase(DesignStyle.LIQUID_GLASS, AppTheme.SAKURA)

@Preview(name = "Material 3 White", widthDp = 380, heightDp = 760)
@Composable
fun PreviewStyleMaterial3White() = StyleShowcase(DesignStyle.MATERIAL3, AppTheme.WHITE_SCREEN)
