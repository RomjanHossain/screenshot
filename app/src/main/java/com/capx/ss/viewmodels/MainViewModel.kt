package com.capx.ss.viewmodels


import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capx.ss.data.repository.ScreenshotRepository
import com.capx.ss.domain.model.Screenshot
import com.capx.ss.services.ScreenshotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotRepository: ScreenshotRepository
) : ViewModel() {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _screenshots = MutableStateFlow<List<Screenshot>>(emptyList())
    val screenshots: StateFlow<List<Screenshot>> = _screenshots.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _pendingDeleteIntent = MutableSharedFlow<IntentSender>()
    val pendingDeleteIntent: SharedFlow<IntentSender> = _pendingDeleteIntent.asSharedFlow()


    init {
        viewModelScope.launch {
            loadExistingScreenshots()
            screenshotRepository.screenshots.collect { screenshot ->
                val currentList = _screenshots.value.toMutableList()
                currentList.add(0, screenshot)
                if (currentList.size > 20) {
                    currentList.removeAt(currentList.size - 1)
                }
                _screenshots.value = currentList
            }
        }

        // Check service status periodically
        checkServiceStatus()
        checkOverlayPermission()
    }

    fun loadExistingScreenshots() {
        viewModelScope.launch {
            try {
                val existingScreenshots = screenshotRepository.loadExistingScreenshots()
                _screenshots.value = existingScreenshots
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun captureScreenshot() {
        viewModelScope.launch {
            val intent = Intent(context, ScreenshotService::class.java).apply {
                action = ScreenshotService.ACTION_CAPTURE_SCREENSHOT
            }
            context.startService(intent)
        }
    }

    fun deleteScreenshot(screenshot: Screenshot) {
        viewModelScope.launch {
            try {
                val intentSender = screenshotRepository.deleteScreenshot(screenshot)
                if (intentSender != null) {
                    _pendingDeleteIntent.emit(intentSender)
                } else {
                    loadExistingScreenshots()
                }
            } catch (securityException: SecurityException) {
                securityException.printStackTrace()
            }
        }
    }

    fun checkOverlayPermission() {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    fun stopService() {
        val intent = Intent(context, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
        _isServiceRunning.value = false
    }

    private fun checkServiceStatus() {
        viewModelScope.launch {
            while (true) {
                _isServiceRunning.value = isMyServiceRunning(ScreenshotService::class.java)
                delay(2000)
            }
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}