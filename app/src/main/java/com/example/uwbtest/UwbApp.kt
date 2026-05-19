package com.example.uwbtest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口點。
 * @HiltAndroidApp 觸發 Hilt 的程式碼生成，建立依賴注入根元件。
 * AndroidManifest.xml 中需設定 android:name=".UwbApp"。
 */
@HiltAndroidApp
class UwbApp : Application()
