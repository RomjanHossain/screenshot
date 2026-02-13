package com.capx.ss.data.repository


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
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

    fun setMediaProjection(projection: MediaProjection?) {
        mediaProjection = projection
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

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    Bitmap.createBitmap(bitmap, 0, 0, width, height)
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

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }

    fun stopProjection() {
        cleanup()
        mediaProjection?.stop()
        mediaProjection = null
    }
}