package net.nexus.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import net.nexus.app.data.AppIcon

object AppIconManager {
    fun applyIcon(context: Context, targetIcon: AppIcon) {
        val pm = context.packageManager
        val packageName = context.packageName

        // 1. Enable target alias first so there is always at least one active launcher
        val targetComponent = ComponentName(packageName, targetIcon.aliasName)
        try {
            if (pm.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Disable all other aliases
        AppIcon.entries.filter { it != targetIcon }.forEach { icon ->
            val component = ComponentName(packageName, icon.aliasName)
            try {
                if (pm.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** The picture of the launcher icon the user picked (used in notifications and the picker). */
    fun avatarRes(icon: AppIcon): Int = when (icon) {
        AppIcon.DEFAULT -> net.nexus.app.R.drawable.ic_avatar_nexus_white
        AppIcon.NEXUS_DARK -> net.nexus.app.R.drawable.ic_avatar_nexus_dark
        AppIcon.BASE -> net.nexus.app.R.drawable.ic_avatar_base
        AppIcon.MINIMAL_GREY -> net.nexus.app.R.drawable.ic_avatar_grey
        AppIcon.TOTAL_BLACK -> net.nexus.app.R.drawable.ic_avatar_black
        AppIcon.INVERSION_MINIMAL -> net.nexus.app.R.drawable.ic_avatar_inversion
        AppIcon.WHITE_VIOLET -> net.nexus.app.R.drawable.ic_avatar_violet
        AppIcon.NYA_VPN -> net.nexus.app.R.drawable.ic_avatar_nya
    }
}
