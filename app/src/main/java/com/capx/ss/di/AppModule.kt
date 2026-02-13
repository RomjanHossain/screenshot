package com.capx.ss.di


import android.content.Context
import android.media.projection.MediaProjectionManager
import com.capx.ss.data.repository.ScreenshotRepository
import com.capx.ss.services.ScreenshotNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMediaProjectionManager(
        @ApplicationContext context: Context
    ): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    @Provides
    @Singleton
    fun provideScreenshotRepository(
        @ApplicationContext context: Context
    ): ScreenshotRepository {
        return ScreenshotRepository(context)
    }

    @Provides
    @Singleton
    fun provideScreenshotNotificationManager(
        @ApplicationContext context: Context
    ): ScreenshotNotificationManager {
        return ScreenshotNotificationManager(context)
    }
}
