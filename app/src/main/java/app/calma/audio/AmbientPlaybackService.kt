package app.calma.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import app.calma.calmaContainer
import app.calma.core.notifications.NotificationChannels

/**
 * Keeps ambient audio playing while Calma is in the background.
 *
 * Android 9+ does not let a background app keep audio alive without a foreground service,
 * so the service does exactly that and nothing else: it promotes the process, shows one
 * low-importance notification, and stops itself the moment the last layer stops. It never
 * runs while nothing is playing, which keeps it from being a persistent background process.
 */
class AmbientPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = applicationContext.calmaContainer.soundEngine

        when (intent?.action) {
            ACTION_STOP -> {
                engine.stopAll()
                stopSelfSafely()
                return START_NOT_STICKY
            }
        }

        if (!engine.isPlaying()) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        promoteToForeground()
        // Deliberately not sticky: if the process dies there is nothing meaningful to
        // resume, and the mixer state is already persisted in DataStore.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val notification = NotificationChannels.playbackNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NotificationChannels.NOTIFICATION_ID_PLAYBACK,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            ServiceCompat.startForeground(
                this,
                NotificationChannels.NOTIFICATION_ID_PLAYBACK,
                notification,
                0,
            )
        }
    }

    private fun stopSelfSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        const val ACTION_STOP = "app.calma.action.STOP_AMBIENT"

        /** Called by the UI whenever playback starts, if the user allowed background audio. */
        fun start(context: Context) {
            val intent = Intent(context, AmbientPlaybackService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AmbientPlaybackService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}