package com.mono.launcher.di

import android.content.Context
import androidx.room.Room
import com.mono.launcher.data.AppDao
import com.mono.launcher.data.MonoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MonoDatabase =
        Room.databaseBuilder(context, MonoDatabase::class.java, "mono.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun appDao(db: MonoDatabase): AppDao = db.appDao()

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
