package com.capx.ss.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.ServiceCompat
import com.capx.ss.data.repository.ScreenshotRepository
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class ScreenshotService : Service() {

    @Inject
    lateinit var screenshotRepository: ScreenshotRepository

    @Inject
    lateinit var notificationManager: ScreenshotNotificationManager

    private var mediaProjection: MediaProjection? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Floating button views
    private var windowManager: WindowManager? = null
    private var floatingButtonView: View? = null
    private var isFloatingButtonAdded = false


    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_CAPTURE_SCREENSHOT = "ACTION_CAPTURE_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
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
            val screenshot = screenshotRepository.captureScreenshot()
            withContext(Dispatchers.Main) {
                if (screenshot != null) {
                    Toast.makeText(
                        this@ScreenshotService,
                        "Screenshot saved: ${screenshot.fileName}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ScreenshotService,
                        "Failed to capture screenshot",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addFloatingButton() {
        if (isFloatingButtonAdded) return

        val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        // Position the button at the bottom right
        layoutParams.gravity = Gravity.BOTTOM or Gravity.END
        layoutParams.x = 50 // margin from right
        layoutParams.y = 50 // margin from bottom

        // Create ComposeView for the floating button
        floatingButtonView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FloatingCaptureButton(
                        onClick = {
                            captureScreenshot()
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(floatingButtonView, layoutParams)
            isFloatingButtonAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingButton() {
        if (isFloatingButtonAdded && floatingButtonView != null) {
            try {
                windowManager?.removeView(floatingButtonView)
                floatingButtonView = null
                isFloatingButtonAdded = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        screenshotRepository.stopProjection()
        serviceScope.cancel()
        notificationManager.cancelNotification()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingCaptureButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                color = Color(0xFF6200EE),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Capture Screenshot",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}