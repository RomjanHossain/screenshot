package com.capx.ss.viewmodels


import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capx.ss.data.repository.ScreenshotRepository
import com.capx.ss.domain.model.Screenshot
import com.capx.ss.services.ScreenshotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        viewModelScope.launch {
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
    }

    fun captureScreenshot() {
        viewModelScope.launch {
            val intent = Intent(context, ScreenshotService::class.java).apply {
                action = ScreenshotService.ACTION_CAPTURE_SCREENSHOT
            }
            context.startService(intent)
        }
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
                // Simple service status check
                // You might want to implement a more sophisticated check
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