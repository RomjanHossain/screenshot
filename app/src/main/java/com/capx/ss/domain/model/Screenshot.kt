package com.capx.ss.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Screenshot(
    val id: Long = System.currentTimeMillis(),
    val uri: Uri,
    val fileName: String,
    val timestamp: Date = Date(),
    val fileSize: Long = 0
) : Parcelable