package net.nexus.app.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import net.nexus.app.R
import net.nexus.app.data.AppIcon
import net.nexus.app.data.Store
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SubscriptionNotifier {
    private const val CHANNEL_ID = "nexus_sub_channel"
    private const val NOTIF_ID = 2001

    fun checkAndNotify(context: Context, user: TelegramUser) {
        if (user.expireAt.isBlank()) return

        val daysLeft = getDaysLeft(user.expireAt) ?: return
        val today = LocalDate.now().toString()
        val store = Store(context)

        // Only notify if expiring in 0..3 days, or already expired, and haven't notified today
        if (daysLeft in 0..3 || (!user.isSubscribed && daysLeft <= 0)) {
            if (store.lastExpiryNotifiedDay == today) return
            store.lastExpiryNotifiedDay = today

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.sub_notif_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                nm.createNotificationChannel(channel)
            }

            val botIntent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.tg_auth_bot_url))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(
                context, 0, botIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val text = when {
                daysLeft <= 0 -> context.getString(R.string.sub_expired)
                daysLeft == 0L -> context.getString(R.string.sub_notif_expiring_today)
                else -> context.getString(R.string.sub_notif_expiring_days, daysLeft)
            }

            val avatarRes = when (store.loadSettings().appIcon) {
                AppIcon.MINIMAL_GREY -> R.drawable.ic_avatar_grey
                AppIcon.TOTAL_BLACK -> R.drawable.ic_avatar_black
                else -> R.drawable.ic_avatar_base
            }

            val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(avatarRes)
                .setContentTitle(context.getString(R.string.sub_notif_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .addAction(
                    0,
                    context.getString(R.string.sub_renew_btn),
                    pi
                )

            nm.notify(NOTIF_ID, notifBuilder.build())
        }
    }

    fun getDaysLeft(expireAtStr: String): Long? {
        return try {
            val instant = Instant.parse(expireAtStr)
            val now = Instant.now()
            Duration.between(now, instant).toDays()
        } catch (_: Exception) {
            try {
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val ldt = java.time.LocalDateTime.parse(expireAtStr, formatter)
                val instant = ldt.atZone(ZoneId.systemDefault()).toInstant()
                Duration.between(Instant.now(), instant).toDays()
            } catch (_: Exception) {
                null
            }
        }
    }
}
