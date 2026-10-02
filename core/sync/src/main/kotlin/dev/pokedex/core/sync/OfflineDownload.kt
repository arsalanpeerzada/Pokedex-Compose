package dev.pokedex.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.data.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_NAME = "offline-download"

/**
 * The optional "download everything" job. It only fetches what's missing, at most four requests
 * at a time (PokeAPI's fair-use policy asks for caching, which this is), and by default waits for
 * Wi-Fi. If the phone stops it part-way, it carries on from where it got to.
 */
@Singleton
class OfflineDownload @Inject constructor(@ApplicationContext private val context: Context) {

    fun start(wifiOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** True while the download is queued (for example waiting for Wi-Fi) or running. */
    val active: Flow<Boolean> = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(WORK_NAME).map { infos ->
        infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SyncEntryPoint {
    fun pokemon(): PokemonRepository
}

class OfflineDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pokemon = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java).pokemon()
        if (pokemon.refreshIndex().isFailure) return Result.retry()
        val missing = pokemon.downloadAll()
        // A few failures (a flaky connection) are worth another go later; give up after a handful of tries.
        return when {
            missing == 0 -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
