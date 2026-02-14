package com.capx.ss.pages.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.capx.ss.domain.model.Screenshot

@Composable
fun ScreenshotItem(
    screenshot: Screenshot,
    onDelete: (Screenshot) -> Unit
) {
    val context = LocalContext.current

    var showFullImage by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    if (showFullImage) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = screenshot.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showFullImage = false }, // Close on tap
                    contentScale = ContentScale.Fit
                )
                // The Action Buttons (Overlay)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Share Button
                    FilledTonalButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, screenshot.uri)
                                type = "image/png"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val shareIntent =
                                Intent.createChooser(sendIntent, "Share screenshot via")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(
                            height = 60.dp, width = 160.dp,
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Text("SHARE")
                        }
                    }

                    // Delete Button
                    FilledTonalButton(
                        onClick = { deleteDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(
                            height = 60.dp, width = 160.dp,
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {

                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Text("DELETE")
                        }
                    }
                }
            }
        }
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete Screenshot?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(screenshot)
                    deleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()

            .height(300.dp)
            .padding(5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            showFullImage = true
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. The Preview Image
            AsyncImage(
                model = screenshot.uri,
                contentDescription = "Screenshot preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Fills the card area
            )

            // 2. The Delete Button (Bottom Right)
            IconButton(
                onClick = {
                    deleteDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Screenshot",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun onShare(screenshot: Screenshot) {

}

//
//@Composable
//fun ScreenshotItem(screenshot: Screenshot) {
//    val context = LocalContext.current
//    Card(
//        modifier = Modifier
//            .fillMaxWidth(
//            )
//            .height(300.dp)
//            .padding(5.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        onClick = {
//            // Open the screenshot
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(screenshot.uri, "image/png")
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//            context.startActivity(intent)
//        }
//    ) {
//        Text("View Screenshot")
//    }
//}
