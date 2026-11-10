package com.mono.launcher.data

import androidx.room.Entity
import androidx.room.Index

/**
 * One launchable activity for one user profile.
 * [userSerial] is the stable serial number from UserManager, so entries survive reboots.
 */
@Entity(
    tableName = "apps",
    primaryKeys = ["packageName", "activityName", "userSerial"],
    indices = [Index("isFavorite"), Index("packageName")],
)
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int? = null,
    val launchCount: Int = 0,
    val lastLaunched: Long = 0L,
    val isWorkProfile: Boolean = false,
) {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: label
    val key: AppKey get() = AppKey(packageName, activityName, userSerial)
}

data class AppKey(val packageName: String, val activityName: String, val userSerial: Long)
