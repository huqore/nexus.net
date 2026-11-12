package net.nexus.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.nexus.app.data.AppTheme
import net.nexus.app.data.DesignStyle
import net.nexus.app.data.Server

private fun mx(name: String, ping: Int) =
    Server(id = name, name = name, protocol = "vless", network = "tcp", security = "reality", source = "LINK", pingMs = ping)

@Composable
private fun Cell(theme: AppTheme, style: DesignStyle) {
    NexusTheme(fontScale = 1f, appTheme = theme, designStyle = style) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(theme.name + " / " + style.name, color = NX.text, fontSize = 12.sp)
                val shape = nxCardShape(28.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(NX.card).nxBorder(shape).padding(8.dp)
                ) {
                    ServerRow(server = mx("\uD83C\uDDE9\uD83C\uDDEA Германия Wi-Fi", 38), selected = true, pinging = false, onClick = {}, onDelete = {})
                    ServerRow(server = mx("\uD83C\uDDF5\uD83C\uDDF1 Польша Wi-Fi", 118), selected = false, pinging = false, onClick = {}, onDelete = {})
                }
            }
            FloatingNavBar(selected = Tab.CONNECT, onSelect = {}, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Preview(name = "HOME_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_HOME_SCREEN_FLUENT() = Cell(AppTheme.HOME_SCREEN, DesignStyle.FLUENT)

@Preview(name = "HOME_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_HOME_SCREEN_MATERIAL3() = Cell(AppTheme.HOME_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "HOME_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_HOME_SCREEN_LIQUID_GLASS() = Cell(AppTheme.HOME_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "BASE FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_BASE_FLUENT() = Cell(AppTheme.BASE, DesignStyle.FLUENT)

@Preview(name = "BASE MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_BASE_MATERIAL3() = Cell(AppTheme.BASE, DesignStyle.MATERIAL3)

@Preview(name = "BASE LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_BASE_LIQUID_GLASS() = Cell(AppTheme.BASE, DesignStyle.LIQUID_GLASS)

@Preview(name = "AMOLED_BLACK FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_AMOLED_BLACK_FLUENT() = Cell(AppTheme.AMOLED_BLACK, DesignStyle.FLUENT)

@Preview(name = "AMOLED_BLACK MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_AMOLED_BLACK_MATERIAL3() = Cell(AppTheme.AMOLED_BLACK, DesignStyle.MATERIAL3)

@Preview(name = "AMOLED_BLACK LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_AMOLED_BLACK_LIQUID_GLASS() = Cell(AppTheme.AMOLED_BLACK, DesignStyle.LIQUID_GLASS)

@Preview(name = "GREEN_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_GREEN_SCREEN_FLUENT() = Cell(AppTheme.GREEN_SCREEN, DesignStyle.FLUENT)

@Preview(name = "GREEN_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_GREEN_SCREEN_MATERIAL3() = Cell(AppTheme.GREEN_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "GREEN_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_GREEN_SCREEN_LIQUID_GLASS() = Cell(AppTheme.GREEN_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "BLUE_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_BLUE_SCREEN_FLUENT() = Cell(AppTheme.BLUE_SCREEN, DesignStyle.FLUENT)

@Preview(name = "BLUE_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_BLUE_SCREEN_MATERIAL3() = Cell(AppTheme.BLUE_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "BLUE_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_BLUE_SCREEN_LIQUID_GLASS() = Cell(AppTheme.BLUE_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "RED_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_RED_SCREEN_FLUENT() = Cell(AppTheme.RED_SCREEN, DesignStyle.FLUENT)

@Preview(name = "RED_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_RED_SCREEN_MATERIAL3() = Cell(AppTheme.RED_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "RED_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_RED_SCREEN_LIQUID_GLASS() = Cell(AppTheme.RED_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "WHITE_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_WHITE_SCREEN_FLUENT() = Cell(AppTheme.WHITE_SCREEN, DesignStyle.FLUENT)

@Preview(name = "WHITE_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_WHITE_SCREEN_MATERIAL3() = Cell(AppTheme.WHITE_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "WHITE_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_WHITE_SCREEN_LIQUID_GLASS() = Cell(AppTheme.WHITE_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "PINK_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_PINK_SCREEN_FLUENT() = Cell(AppTheme.PINK_SCREEN, DesignStyle.FLUENT)

@Preview(name = "PINK_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_PINK_SCREEN_MATERIAL3() = Cell(AppTheme.PINK_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "PINK_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_PINK_SCREEN_LIQUID_GLASS() = Cell(AppTheme.PINK_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "YELLOW_SCREEN FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_YELLOW_SCREEN_FLUENT() = Cell(AppTheme.YELLOW_SCREEN, DesignStyle.FLUENT)

@Preview(name = "YELLOW_SCREEN MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_YELLOW_SCREEN_MATERIAL3() = Cell(AppTheme.YELLOW_SCREEN, DesignStyle.MATERIAL3)

@Preview(name = "YELLOW_SCREEN LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_YELLOW_SCREEN_LIQUID_GLASS() = Cell(AppTheme.YELLOW_SCREEN, DesignStyle.LIQUID_GLASS)

@Preview(name = "SAKURA FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_SAKURA_FLUENT() = Cell(AppTheme.SAKURA, DesignStyle.FLUENT)

@Preview(name = "SAKURA MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_SAKURA_MATERIAL3() = Cell(AppTheme.SAKURA, DesignStyle.MATERIAL3)

@Preview(name = "SAKURA LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_SAKURA_LIQUID_GLASS() = Cell(AppTheme.SAKURA, DesignStyle.LIQUID_GLASS)

@Preview(name = "WINTER FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_WINTER_FLUENT() = Cell(AppTheme.WINTER, DesignStyle.FLUENT)

@Preview(name = "WINTER MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_WINTER_MATERIAL3() = Cell(AppTheme.WINTER, DesignStyle.MATERIAL3)

@Preview(name = "WINTER LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_WINTER_LIQUID_GLASS() = Cell(AppTheme.WINTER, DesignStyle.LIQUID_GLASS)

@Preview(name = "RGB FLUENT", widthDp = 300, heightDp = 360)
@Composable
fun M_RGB_FLUENT() = Cell(AppTheme.RGB, DesignStyle.FLUENT)

@Preview(name = "RGB MATERIAL3", widthDp = 300, heightDp = 360)
@Composable
fun M_RGB_MATERIAL3() = Cell(AppTheme.RGB, DesignStyle.MATERIAL3)

@Preview(name = "RGB LIQUID_GLASS", widthDp = 300, heightDp = 360)
@Composable
fun M_RGB_LIQUID_GLASS() = Cell(AppTheme.RGB, DesignStyle.LIQUID_GLASS)
