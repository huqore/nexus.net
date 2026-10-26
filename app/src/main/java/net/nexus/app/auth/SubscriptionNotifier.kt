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

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_vpn)
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
                .build()

            nm.notify(NOTIF_ID, notif)
        }
    }

    fun parseExpiry(expireAt: String): Instant? {
        val trimmed = expireAt.trim()
        return runCatching { Instant.parse(trimmed) }.getOrNull()
            ?: runCatching {
                val datePart = trimmed.take(10)
                LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
            }.getOrNull()
    }

    fun getDaysLeft(expireAt: String): Long? {
        val instant = parseExpiry(expireAt) ?: return null
        return Duration.between(Instant.now(), instant).toDays()
    }
}
