package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AsistenDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BriefingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AsistenDatabase.getDatabase(applicationContext)
            val jadwalDao = database.jadwalDao()
            val materiDao = database.materiDao()

            val calendar = Calendar.getInstance()
            // Calculate tomorrow day name in Indonesian
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowDayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Senin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Kamis"
                Calendar.FRIDAY -> "Jumat"
                Calendar.SATURDAY -> "Sabtu"
                else -> "Minggu"
            }

            val tomorrowJadwal = jadwalDao.getJadwalByHariSync(tomorrowDayName)

            if (tomorrowJadwal.isNotEmpty()) {
                val sessionItems = mutableListOf<String>()
                for (item in tomorrowJadwal) {
                    val nextMateri = materiDao.getNextMateri(item.kelas, item.mapel)
                    val pLabel = if (nextMateri != null) "P${nextMateri.pertemuan}" else "Review"
                    sessionItems.add("${item.mapel} $pLabel")
                }
                val sessionsSummary = sessionItems.joinToString(", ")

                val title = "Briefing Besok ($tomorrowDayName)"
                val body = "${tomorrowJadwal.size} sesi mengajar: $sessionsSummary. Siapkan bahan ajar malam ini ya!"
                NotificationHelper.showBriefingNotification(applicationContext, title, body)
            } else {
                NotificationHelper.showBriefingNotification(
                    applicationContext,
                    "Briefing Esok Hari ($tomorrowDayName)",
                    "Besok tidak ada jadwal mengajar tetap. Selamat beristirahat!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "BriefingDailyWorker"

        fun scheduleDailyBriefing(context: Context, hour: Int = 20, minute: Int = 0) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val briefingRequest = PeriodicWorkRequestBuilder<BriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                briefingRequest
            )
        }

        fun cancelDailyBriefing(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
