package com.example.uwbtest.di

import android.content.Context
import androidx.core.uwb.UwbManager
import com.example.uwbtest.data.repository.UwbRepositoryImpl
import com.example.uwbtest.domain.repository.UwbRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI Module：UWB 相關依賴的組裝點。
 *
 * ● providesUwbManager：建立 UwbManager（需要 ApplicationContext）
 * ● bindsUwbRepository：將 UwbRepositoryImpl 綁定到 UwbRepository 介面
 *
 * SingletonComponent 確保整個 App 生命週期只有一個實例，
 * 這對於 UwbScope 快取至關重要（see UwbRepositoryImpl）。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UwbModule {

    @Binds
    @Singleton
    abstract fun bindsUwbRepository(impl: UwbRepositoryImpl): UwbRepository

    companion object {
        @Provides
        @Singleton
        fun providesUwbManager(@ApplicationContext context: Context): UwbManager =
            UwbManager.createInstance(context)
    }
}
