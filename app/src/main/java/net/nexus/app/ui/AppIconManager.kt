package net.nexus.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import net.nexus.app.data.AppIcon

object AppIconManager {
    fun applyIcon(context: Context, targetIcon: AppIcon) {
        val pm = context.packageManager
        val packageName = context.packageName

        AppIcon.entries.forEach { icon ->
            val component = ComponentName(packageName, icon.aliasName)
            val targetState = if (icon == targetIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                if (pm.getComponentEnabledSetting(component) != targetState) {
                    pm.setComponentEnabledSetting(
                        component,
                        targetState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

