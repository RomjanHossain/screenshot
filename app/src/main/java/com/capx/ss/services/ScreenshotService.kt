package com.capx.ss.services

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.capx.ss.data.repository.ScreenshotRepository
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ScreenshotService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    @Inject
    lateinit var screenshotRepository: ScreenshotRepository

    @Inject
    lateinit var notificationManager: ScreenshotNotificationManager

    private var mediaProjection: MediaProjection? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Owners for Compose
    private val mViewModelStore = ViewModelStore()
    private val mSavedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

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

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        mSavedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundService()
        isRunning = true
    }

    private fun startForegroundService() {
        val notification = notificationManager.createNotification().build()
        startForeground(1001, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
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

                // Add floating button with delay on main thread
                Handler(Looper.getMainLooper()).postDelayed({
                    addFloatingButton()
                }, 1000)
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
        try {
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data!!)
            screenshotRepository.setMediaProjection(mediaProjection)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to setup media projection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureScreenshot() {
        serviceScope.launch {
            // Hide FAB before capture
            withContext(Dispatchers.Main) {
                floatingButtonView?.visibility = View.GONE
            }
            
            // Give a tiny delay for the view to disappear from the buffer
            delay(50)

            val screenshot = screenshotRepository.captureScreenshot()
            
            withContext(Dispatchers.Main) {
                // Show FAB again
                floatingButtonView?.visibility = View.VISIBLE
                
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.BOTTOM or Gravity.END
                x = 50
                y = 50
            }

            val frameLayout = FrameLayout(this)

            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme {
                        FloatingCaptureButton(
                            onClick = {
                                captureScreenshot()
                            },
                            onDrag = { dx, dy ->
                                // Update layout params based on drag
                                // Since gravity is BOTTOM | END, increasing x moves it left, 
                                // and increasing y moves it up.
                                layoutParams.x -= dx.toInt()
                                layoutParams.y -= dy.toInt()
                                try {
                                    windowManager?.updateViewLayout(frameLayout, layoutParams)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }
            }

            frameLayout.addView(composeView)

            // Set the owners on the root view (frameLayout)
            frameLayout.setViewTreeLifecycleOwner(this)
            frameLayout.setViewTreeSavedStateRegistryOwner(this)
            frameLayout.setViewTreeViewModelStoreOwner(this)

            windowManager?.addView(frameLayout, layoutParams)

            floatingButtonView = frameLayout
            isFloatingButtonAdded = true

            Toast.makeText(this, "Floating button added", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to add button: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeFloatingButton() {
        if (isFloatingButtonAdded && floatingButtonView != null) {
            try {
                windowManager?.removeView(floatingButtonView)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                floatingButtonView = null
                isFloatingButtonAdded = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeFloatingButton()
        screenshotRepository.stopProjection()
        serviceScope.cancel()
        notificationManager.cancelNotification()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

@Composable
fun FloatingCaptureButton(
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                color = Color(0xFF6200EE),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
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
