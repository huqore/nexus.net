package net.nexus.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import net.nexus.app.MainViewModel
import net.nexus.app.auth.TelegramAuthState
import net.nexus.app.data.AppTheme
import net.nexus.app.data.DesignStyle
import net.nexus.app.update.AppReleaseInfo
import net.nexus.app.update.UpdateState

/** Debug-only harnesses for dialogs and the mandatory auth screen in every design style. */
private val demoRelease = AppReleaseInfo(
    tagName = "v4.0.0", versionName = "4.0.0",
    changelog = "• Material 3 redesign\n• Liquid Glass polish\n• Live slider in the bottom bar",
    downloadUrl = "https://example.invalid/app.apk", apkFileName = "app.apk", apkSize = 31_000_000,
)

@Composable
private fun DialogHarness(style: DesignStyle, content: @Composable (MainViewModel) -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm = remember { MainViewModel(app) }
    NexusTheme(fontScale = 1f, appTheme = AppTheme.BASE, designStyle = style) {
        Box(Modifier.fillMaxSize()) { content(vm) }
    }
}

@Composable private fun Tg(style: DesignStyle) = DialogHarness(style) {
    TelegramAuthDialog(TelegramAuthState.WaitingConfirmation("s", "https://t.me/example_bot"), it)
}
@Composable private fun Upd(style: DesignStyle) = DialogHarness(style) {
    UpdateDialog(UpdateState.Available(demoRelease), it)
}
@Composable private fun Dl(style: DesignStyle) = DialogHarness(style) {
    UpdateDialog(UpdateState.Downloading(demoRelease, 0.42f, 13_000_000, 31_000_000), it)
}
@Composable private fun Mand(style: DesignStyle) = DialogHarness(style) { MandatoryTelegramAuthScreen(it) }
@Composable private fun Web(style: DesignStyle) = DialogHarness(style) { WebsiteLoginDialog(it, onDismiss = {}) }

@Preview(name = "D Tg F", widthDp = 400, heightDp = 860) @Composable fun D_Tg_F() = Tg(DesignStyle.FLUENT)
@Preview(name = "D Tg M", widthDp = 400, heightDp = 860) @Composable fun D_Tg_M() = Tg(DesignStyle.MATERIAL3)
@Preview(name = "D Tg G", widthDp = 400, heightDp = 860) @Composable fun D_Tg_G() = Tg(DesignStyle.LIQUID_GLASS)
@Preview(name = "D Upd F", widthDp = 400, heightDp = 860) @Composable fun D_Upd_F() = Upd(DesignStyle.FLUENT)
@Preview(name = "D Upd M", widthDp = 400, heightDp = 860) @Composable fun D_Upd_M() = Upd(DesignStyle.MATERIAL3)
@Preview(name = "D Upd G", widthDp = 400, heightDp = 860) @Composable fun D_Upd_G() = Upd(DesignStyle.LIQUID_GLASS)
@Preview(name = "D Dl M", widthDp = 400, heightDp = 860) @Composable fun D_Dl_M() = Dl(DesignStyle.MATERIAL3)
@Preview(name = "D Dl G", widthDp = 400, heightDp = 860) @Composable fun D_Dl_G() = Dl(DesignStyle.LIQUID_GLASS)
@Preview(name = "D Mand F", widthDp = 400, heightDp = 860) @Composable fun D_Mand_F() = Mand(DesignStyle.FLUENT)
@Preview(name = "D Mand M", widthDp = 400, heightDp = 860) @Composable fun D_Mand_M() = Mand(DesignStyle.MATERIAL3)
@Preview(name = "D Mand G", widthDp = 400, heightDp = 860) @Composable fun D_Mand_G() = Mand(DesignStyle.LIQUID_GLASS)
@Preview(name = "D Web M", widthDp = 400, heightDp = 860) @Composable fun D_Web_M() = Web(DesignStyle.MATERIAL3)
@Preview(name = "D Web G", widthDp = 400, heightDp = 860) @Composable fun D_Web_G() = Web(DesignStyle.LIQUID_GLASS)
