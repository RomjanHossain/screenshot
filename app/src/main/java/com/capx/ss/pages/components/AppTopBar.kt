package com.capx.ss.pages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capx.ss.R
import com.capx.ss.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    CenterAlignedTopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.app_title))
                if (isServiceRunning) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(10.dp)
                            .background(color = Color(0xff1B7A2A))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(10.dp)
                            .background(color = Color(0xffFFB800))
                    )

                }
            }
        }
    )
}
