package com.capx.ss.services


import android.R.attr.action
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.capx.ss.MainActivity
import com.capx.ss.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "screenshot_service_channel"
    private val notificationId = 1001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Screenshot Service"
            val descriptionText = "Service for capturing screenshots"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(): NotificationCompat.Builder {
        val captureIntent = Intent(context, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_CAPTURE_SCREENSHOT
        }
        val capturePendingIntent = PendingIntent.getService(
            context,
            0,
            captureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            2,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Simple Screenshot")
            .setContentText("Ready to capture screenshots")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(activityPendingIntent)
            .addAction(R.drawable.ic_camera, "Capture", capturePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    fun showNotification() {
        val notification = createNotification().build()
        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(notificationId)
    }
}