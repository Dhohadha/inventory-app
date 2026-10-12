package com.packready.shopkeeper.di

import android.content.Context
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.ScreenWakeLockManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModule {

    @Provides
    @Singleton
    fun provideSoundPoolManager(@ApplicationContext context: Context): SoundPoolManager {
        return SoundPoolManager(context)
    }

    @Provides
    @Singleton
    fun provideHapticManager(@ApplicationContext context: Context): HapticManager {
        return HapticManager(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothPrinterManager(): BluetoothPrinterManager {
        return BluetoothPrinterManager()
    }

    @Provides
    @Singleton
    fun provideScreenWakeLockManager(): ScreenWakeLockManager {
        return ScreenWakeLockManager()
    }
}
