# UWB — 保留所有 androidx.core.uwb 類別名稱（avoid stripping UWB API reflection calls）
-keep class androidx.core.uwb.** { *; }

# Hilt — 保留生成的 DI 元件
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
