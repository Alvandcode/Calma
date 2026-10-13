package app.calma.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.calma.MainActivity
import app.calma.R

/**
 * Notification channels and builders.
 *
 * Calma uses exactly two channels, both user-visible in system settings, and both only
 * posted when the matching setting is enabled.
 */
object NotificationChannels {

    const val PLAYBACK_ID = "ambient_playback"
    const val SESSION_ID = "focus_sessions"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val playback = NotificationChannel(
            PLAYBACK_ID,
            context.getString(R.string.sound_playback_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.sound_playback_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val session = NotificationChannel(
            SESSION_ID,
            context.getString(R.string.focus_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.focus_notification_channel_desc)
            setShowBadge(true)
            enableVibration(true)
        }

        manager.createNotificationChannel(playback)
        manager.createNotificationChannel(session)
    }

    fun contentIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun playbackNotification(context: Context): Notification =
        NotificationCompat.Builder(context, PLAYBACK_ID)
            .setSmallIcon(R.drawable.ic_notification_calma)
            .setContentTitle(context.getString(R.string.sound_playback_notification))
            .setContentText(context.getString(R.string.app_name))
            .setContentIntent(contentIntent(context, REQUEST_PLAYBACK))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    fun sessionCompletedNotification(
        context: Context,
        title: String,
        text: String,
    ): Notification = NotificationCompat.Builder(context, SESSION_ID)
        .setSmallIcon(R.drawable.ic_notification_calma)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(contentIntent(context, REQUEST_SESSION))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    fun notify(context: Context, notification: Notification, id: Int) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    const val REQUEST_PLAYBACK = 1001
    const val REQUEST_SESSION = 1002
    const val NOTIFICATION_ID_PLAYBACK = 2001
}