package com.mono.launcher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mono.launcher.data.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Fallback for missed LauncherApps callbacks (some OEMs drop them). Runs rarely; no polling. */
@HiltWorker
class AppRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        repository.sync()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppRefreshWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("app-refresh", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
