package dev.tomasmed.jlight.wear.data.sync

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Battery-aware 30-minute background synchronization service using Android's native [JobScheduler].
 *
 * Syncs `/api/events`, `/api/chores`, and `/api/lists` into Room SQLite and triggers
 * [WearSyncCoordinator.requestSurfaceUpdates] so the ProtoLayout Tile and Watch Face Complication
 * stay fresh without waking the watch when the network is unavailable.
 */
class JlightPeriodicSyncJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            val repository = WearSyncCoordinator.provideRepository(applicationContext)
            repository.syncWithRemote()
            WearSyncCoordinator.requestSurfaceUpdates(applicationContext)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val JOB_ID = 42019
        private const val SYNC_INTERVAL_MS = 30L * 60L * 1000L // 30 minutes

        fun schedulePeriodicSync(context: Context) {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return

            val jobInfo = JobInfo.Builder(
                JOB_ID,
                ComponentName(appContext, JlightPeriodicSyncJobService::class.java),
            )
                .setPeriodic(SYNC_INTERVAL_MS)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(false)
                .build()

            scheduler.schedule(jobInfo)
        }
    }
}
