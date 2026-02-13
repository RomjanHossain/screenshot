package com.capx.ss.di


import android.content.Context
import android.media.projection.MediaProjectionManager
import com.capx.ss.data.repository.ScreenshotRepository
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
}

/*
2026-02-13 00:04:08.445 17021-17021 DesktopExperienceFlags  com.capx.ss                          D  Toggle override initialized to: false
2026-02-13 00:04:08.448 17021-17021 ashmem                  com.capx.ss                          E  Pinning is deprecated since Android Q. Please use trim or other methods.
2026-02-13 00:04:08.450 17021-17021 TransactionExecutor     com.capx.ss                          E  Failed to execute the transaction: tId:-745685686 ClientTransaction{
                                                                                                    tId:-745685686   transactionItems=[
                                                                                                    tId:-745685686     LaunchActivityItem{activityToken=android.os.BinderProxy@4189c61,intent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000 xflg=0x4 cmp=com.capx.ss/.MainActivity },ident=73027349,info=ActivityInfo{2262a4 com.capx.ss.MainActivity},curConfig={1.0 310mcc260mnc [en_US] ldltr sw411dp w411dp h914dp 420dpi nrml long compactNeeded port finger qwerty/v/v dpad/v winConfig={ mBounds=Rect(0, 0 - 1080, 2400) mAppBounds=Rect(0, 0 - 1080, 2400) mMaxBounds=Rect(0, 0 - 1080, 2400) mDisplayRotation=ROTATION_0 mWindowingMode=fullscreen mActivityType=undefined mAlwaysOnTop=undefined mRotation=ROTATION_0} s.108 fontWeightAdjustment=0},overrideConfig={1.0 310mcc260mnc [en_US] ldltr sw411dp w411dp h914dp 420dpi nrml long compactNeeded port finger qwerty/v/v dpad/v winConfig={ mBounds=Rect(0, 0 - 1080, 2400) mAppBounds=Rect(0, 0 - 1080, 2400) mMaxBounds=Rect(0, 0 - 1080, 2400) mDisplayRotation=ROTATION_0 mWindowingMode=fullscreen mActivityType=standard mAlwaysOnTop=undefined mRotation=ROTATION_0} s.2 fontWeightAdjustment=0},deviceId=0,referrer=com.android.shell,procState=2,state=null,persistentState=null,pendingResults=null,pendingNewIntents=null,sceneTransitionInfo=null,profilerInfo=null,assistToken=android.os.BinderProxy@9b05fd1,shareableActivityToken=android.os.BinderProxy@9f0aa36,activityWindowInfo=ActivityWindowInfo{isEmbedded=false, taskBounds=Rect(0, 0 - 1080, 2400), taskFragmentBounds=Rect(0, 0 - 1080, 2400)},displayId=0}
                                                                                                    tId:-745685686     ResumeActivityItem{mActivityToken=android.os.BinderProxy@4189c61,procState=-1,isForward=true,shouldSendCompatFakeFocus=false}
                                                                                                    tId:-745685686     Target activity: com.capx.ss.MainActivity
                                                                                                    tId:-745685686   ]
                                                                                                    tId:-745685686 }
2026-02-13 00:04:08.450 17021-17021 AndroidRuntime          com.capx.ss                          D  Shutting down VM
2026-02-13 00:04:08.451 17021-17021 AndroidRuntime          com.capx.ss                          E  FATAL EXCEPTION: main (Fix with AI)
                                                                                                    Process: com.capx.ss, PID: 17021
 java.lang.RuntimeException: Unable to start activity ComponentInfo{com.capx.ss/com.capx.ss.MainActivity}
 	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4496)
 	at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:4699)
 	at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:224)
 	at android.app.servertransaction.TransactionExecutor.executeNonLifecycleItem(TransactionExecutor.java:133)
 	at android.app.servertransaction.TransactionExecutor.executeTransactionItems(TransactionExecutor.java:103)
 	at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:80)
 	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2961)
 	at android.os.Handler.dispatchMessage(Handler.java:132)
 	at android.os.Looper.dispatchMessage(Looper.java:333)
 	at android.os.Looper.loopOnce(Looper.java:263)
 	at android.os.Looper.loop(Looper.java:367)
 	at android.app.ActivityThread.main(ActivityThread.java:9282)
 	at java.lang.reflect.Method.invoke(Native Method)
 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:566)
 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:929)
 Caused by: java.lang.IllegalStateException: Hilt Activity must be attached to an @HiltAndroidApp Application. Did you forget to specify your Application's class name in your manifest's <application />'s android:name attribute?
 	at dagger.hilt.android.internal.managers.ActivityComponentManager.createComponent(ActivityComponentManager.java:102)
 	at dagger.hilt.android.internal.managers.ActivityComponentManager.generatedComponent(ActivityComponentManager.java:71)
 	at com.capx.ss.Hilt_MainActivity.generatedComponent(Hilt_MainActivity.java:66)
 	at com.capx.ss.Hilt_MainActivity.inject(Hilt_MainActivity.java:88)
 	at com.capx.ss.Hilt_MainActivity$1.onContextAvailable(Hilt_MainActivity.java:42)
 	at androidx.activity.contextaware.ContextAwareHelper.dispatchOnContextAvailable(ContextAwareHelper.kt:78)
 	at androidx.activity.ComponentActivity.onCreate(ComponentActivity.kt:336)
 	at com.capx.ss.Hilt_MainActivity.onCreate(Hilt_MainActivity.java:54)
 	at com.capx.ss.MainActivity.onCreate(MainActivity.kt:36)
 	at android.app.Activity.performCreate(Activity.java:9306)
 	at android.app.Activity.performCreate(Activity.java:9284)
 	at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1541)
 	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4480)
 	... 14 more
2026-02-13 00:04:08.456 17021-17044 GFXSTREAM               com.capx.ss                          I  [eglDisplay.cpp(297)] Opening libGLESv1_CM_emulation.so
2026-02-13 00:04:08.456 17021-17044 GFXSTREAM               com.capx.ss                          I  [eglDisplay.cpp(297)] Opening libGLESv2_emulation.so

 */