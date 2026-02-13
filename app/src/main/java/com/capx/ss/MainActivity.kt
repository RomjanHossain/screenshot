package com.capx.ss


import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.capx.ss.pages.MainScreen
import com.capx.ss.services.ScreenshotService
import com.capx.ss.ui.theme.SsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaProjectionManager: MediaProjectionManager

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startScreenshotService(result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SsTheme() {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    MainScreen(
                        onRequestPermission = { requestScreenCapture() },
                        onStopService = { stopScreenshotService() }
                    )
                }
            }
        }
    }

    private fun requestScreenCapture() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    private fun startScreenshotService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_START_SERVICE
            putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenshotService.EXTRA_RESULT_INTENT, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopScreenshotService() {
        val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_STOP_SERVICE
        }
        startService(serviceIntent)
    }
}