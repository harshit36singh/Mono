package com.mono.launcher.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppEntry::class], version = 1, exportSchema = true)
abstract class MonoDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
