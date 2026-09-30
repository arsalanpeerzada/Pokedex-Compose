package dev.pokedex.feature.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.data.DailyRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_NAME = "daily-reminder"
private const val CHANNEL_ID = "daily"
private const val NOTIFICATION_ID = 1
private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

/** Schedules or cancels the morning reminder. Safe to call repeatedly. */
@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun apply(enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        createChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(untilNext(REMINDER_TIME).toMinutes(), TimeUnit.MINUTES)
            .build()
        // Keep the existing schedule if there is one, so reopening the app doesn't shift the time.
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        internal fun untilNext(time: LocalTime, now: LocalDateTime = LocalDateTime.now()): Duration {
            val today = now.toLocalDate().atTime(time)
            val next = if (today.isAfter(now)) today else today.plusDays(1)
            return Duration.between(now, next)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderEntryPoint {
    fun daily(): DailyRepository
}

/** Posts the reminder unless today's Pokémon is already solved. It never names the Pokémon. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val daily = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java).daily()
        val solved = daily.result(LocalDate.now().toEpochDay()).first()?.solved == true
        if (!solved && canNotify(applicationContext)) notify(applicationContext)
        return Result.success()
    }

    private fun notify(context: Context) {
        createChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post; skip today's reminder.
        }
    }
}

/** On Android 13+ this needs the runtime permission; earlier versions only need notifications enabled. */
fun canNotify(context: Context): Boolean {
    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun createChannel(context: Context) {
    val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        .apply { description = context.getString(R.string.reminder_channel_description) }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
