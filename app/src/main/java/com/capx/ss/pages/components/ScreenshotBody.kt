package com.capx.ss.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capx.ss.ui.theme.SsTheme
import com.capx.ss.utils.ThemePreviews
import com.capx.ss.viewmodels.MainViewModel


@Composable
fun ScreenshotBody(
    viewModel: MainViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    paddingValues: PaddingValues,
) {
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // overlay permission
        if (!hasOverlayPermission) {
            OverlayPermission(
                icon = Icons.Default.Warning,
                title = "Overlay permission required",
                buttonText = "Grant",
                buttonAction = onRequestOverlayPermission,
            )
        } else {
            if (!isServiceRunning) {
                OverlayPermission(
                    icon = Icons.Default.Info,
                    title = "Service is stopped",
                    buttonText = "Start",
                    buttonAction = onRequestPermission,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScreenshotListView()
    }
}

@Composable
fun OverlayPermission(
    icon: ImageVector,
    title: String,
    buttonText: String,
    buttonAction: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            FilledTonalButton(
                onClick = buttonAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
            ) {
                Text(buttonText, fontSize = 12.sp)
            }
        }
    }
}

@ThemePreviews
@Composable
fun PreviewWarningMsg() {
    SsTheme() {
        OverlayPermission(
            icon = Icons.Default.Info,
            title = "Service is stopped",
            buttonText = "Start",
            buttonAction = {},
        )
    }
}