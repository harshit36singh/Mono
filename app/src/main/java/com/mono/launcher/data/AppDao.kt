package com.mono.launcher.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun observeAll(): Flow<List<AppEntry>>

    @Query("SELECT * FROM apps")
    suspend fun getAll(): List<AppEntry>

    @Upsert
    suspend fun upsertAll(entries: List<AppEntry>)

    @Query("DELETE FROM apps WHERE packageName = :pkg AND activityName = :activity AND userSerial = :user")
    suspend fun delete(pkg: String, activity: String, user: Long)

    @Query("DELETE FROM apps WHERE packageName = :pkg AND userSerial = :user")
    suspend fun deletePackage(pkg: String, user: Long)

    @Query("UPDATE apps SET isHidden = :hidden WHERE packageName = :pkg AND activityName = :activity AND userSerial = :user")
    suspend fun setHidden(pkg: String, activity: String, user: Long, hidden: Boolean)

    @Query("UPDATE apps SET customLabel = :label WHERE packageName = :pkg AND activityName = :activity AND userSerial = :user")
    suspend fun setCustomLabel(pkg: String, activity: String, user: Long, label: String?)

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastLaunched = :now WHERE packageName = :pkg AND activityName = :activity AND userSerial = :user")
    suspend fun recordLaunch(pkg: String, activity: String, user: Long, now: Long)

    @Query("UPDATE apps SET isFavorite = :fav, favoriteOrder = :order WHERE packageName = :pkg AND activityName = :activity AND userSerial = :user")
    suspend fun setFavorite(pkg: String, activity: String, user: Long, fav: Boolean, order: Int?)

    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY favoriteOrder")
    suspend fun favorites(): List<AppEntry>

    @Transaction
    suspend fun reorderFavorites(ordered: List<AppKey>) {
        ordered.forEachIndexed { i, k -> setFavorite(k.packageName, k.activityName, k.userSerial, true, i) }
    }
}
