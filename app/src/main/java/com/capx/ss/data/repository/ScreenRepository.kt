package com.capx.ss.data.repository


import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.capx.ss.domain.model.Screenshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val _screenshots = MutableSharedFlow<Screenshot>(replay = 10)
    val screenshots: Flow<Screenshot> = _screenshots

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            cleanup()
        }
    }

    fun setMediaProjection(projection: MediaProjection?) {
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection = projection
        mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
    }

    suspend fun loadExistingScreenshots(): List<Screenshot> = withContext(Dispatchers.IO) {
        val screenshotList = mutableListOf<Screenshot>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("Screenshot_%")

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val resolver = context.contentResolver

        resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds
                val size = cursor.getLong(sizeColumn)

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                val screenshot = Screenshot(
                    id = id,
                    uri = uri,
                    fileName = name,
                    timestamp = Date(dateAdded),
                    fileSize = size
                )
                screenshotList.add(screenshot)
            }
        }

        return@withContext screenshotList
    }

    suspend fun captureScreenshot(): Screenshot? {
        return withContext(Dispatchers.IO) {
            try {
                val metrics = context.resources.displayMetrics
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val densityDpi = metrics.densityDpi

                imageReader = ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )

                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenshotDisplay",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )

                Thread.sleep(500) // Wait for virtual display to be ready

                val image = imageReader?.acquireLatestImage()
                val bitmap = image?.let {
                    val planes = it.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    createBitmap(bitmap, 0, 0, width, height)
                }

                image?.close()

                bitmap?.let { saveBitmapToStorage(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                cleanup()
            }
        }
    }


    private fun saveBitmapToStorage(bitmap: Bitmap): Screenshot? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Screenshot_$timestamp.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/SimpleScreenshot"
                )
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val screenshot = Screenshot(
                    uri = it,
                    fileName = fileName
                )
                _screenshots.tryEmit(screenshot)
                return screenshot
            }
        }
        return null
    }

    suspend fun deleteScreenshot(screenshot: Screenshot): IntentSender? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        try {
            resolver.delete(screenshot.uri, null, null)
            null
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(screenshot.uri))
                pendingIntent.intentSender
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                securityException.userAction.actionIntent.intentSender
            } else {
                throw securityException
            }
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }

    fun stopProjection() {
        cleanup()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }
}