package com.capx.ss.pages


import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capx.ss.pages.components.AppTopBar
import com.capx.ss.pages.components.ScreenshotBody
import com.capx.ss.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    onRequestPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onStopService: () -> Unit
) {
    val postNotificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    LaunchedEffect(Unit) {
        if (postNotificationPermissionState?.status?.isGranted == false) {
            postNotificationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar()
        },
        floatingActionButton = {
            StopServiceFAB(
                onStopService = onStopService,
            )
        }

    ) { innerPadding ->
        ScreenshotBody(
            onRequestOverlayPermission = onRequestOverlayPermission,
            onRequestPermission = onRequestPermission,
            paddingValues = innerPadding,
        )
    }
}


@Composable
fun StopServiceFAB(
    viewModel: MainViewModel = hiltViewModel(),
    onStopService: () -> Unit,
) {
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    if (isServiceRunning)
        FloatingActionButton(
            onClick = onStopService,
            containerColor =
                MaterialTheme.colorScheme.error,
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
            )
        }
}