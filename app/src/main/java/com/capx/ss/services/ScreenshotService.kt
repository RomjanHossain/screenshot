package com.capx.ss.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.capx.ss.data.repository.ScreenshotRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ScreenshotService : Service() {

    @Inject
    lateinit var screenshotRepository: ScreenshotRepository

    @Inject
    lateinit var notificationManager: ScreenshotNotificationManager

    private var mediaProjection: MediaProjection? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_CAPTURE_SCREENSHOT = "ACTION_CAPTURE_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT"
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        val notification = notificationManager.createNotification().build()
        startForeground(1001, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_INTENT)
                }
                setupMediaProjection(resultCode, data)
            }

            ACTION_CAPTURE_SCREENSHOT -> {
                captureScreenshot()
            }

            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent?) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data!!)
        screenshotRepository.setMediaProjection(mediaProjection)
    }

    private fun captureScreenshot() {
        serviceScope.launch {
            screenshotRepository.captureScreenshot()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotRepository.stopProjection()
        serviceScope.cancel()
        notificationManager.cancelNotification()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideScreenshotNotificationManager(
        @ApplicationContext context: Context
    ): ScreenshotNotificationManager {
        return ScreenshotNotificationManager(context)
    }
}
