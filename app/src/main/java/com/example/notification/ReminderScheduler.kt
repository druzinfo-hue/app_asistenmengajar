package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AsistenDatabase
import com.example.data.local.JadwalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE_MORNING = 100

    fun scheduleMorningAlarm(context: Context) {
        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        val isPagiEnabled = prefs.getBoolean("reminder_pagi_enabled", true)
        val isJadwalEnabled = prefs.getBoolean("reminder_jadwal_enabled", true)

        if (!isPagiEnabled && !isJadwalEnabled) {
            cancelMorningAlarm(context)
            return
        }

        val timeString = prefs.getString("reminder_pagi_time", "06:00") ?: "06:00"
        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

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

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MORNING_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
            }
            Log.d(TAG, "Morning reminder scheduled for: ${target.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule morning alarm: ${e.message}", e)
        }
    }

    fun cancelMorningAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MORNING_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTodayClassReminders(context: Context, jadwalList: List<JadwalEntity>, minutesBefore: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = Calendar.getInstance()

        jadwalList.forEachIndexed { index, jadwal ->
            val parts = jadwal.jamMulai.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return@forEachIndexed
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return@forEachIndexed

            val classTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Subtract minutes
            classTime.add(Calendar.MINUTE, -minutesBefore)

            if (classTime.after(now)) {
                val notifId = NotificationHelper.NOTIFICATION_ID_CLASS_BASE + index
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_PRE_CLASS_REMINDER
                    putExtra(ReminderReceiver.EXTRA_MAPEL, jadwal.mapel)
                    putExtra(ReminderReceiver.EXTRA_KELAS, jadwal.kelas)
                    putExtra(ReminderReceiver.EXTRA_JAM_MULAI, jadwal.jamMulai)
                    putExtra(ReminderReceiver.EXTRA_MENIT_SEBELUM, minutesBefore)
                    putExtra(ReminderReceiver.EXTRA_NOTIF_ID, notifId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notifId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, classTime.timeInMillis, pendingIntent)
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, classTime.timeInMillis, pendingIntent)
                        }
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, classTime.timeInMillis, pendingIntent)
                    }
                    Log.d(TAG, "Scheduled pre-class reminder for ${jadwal.mapel} at ${classTime.time}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule class alarm: ${e.message}", e)
                }
            }
        }
    }

    fun triggerImmediateTest(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TEST_REMINDER
        }
        context.sendBroadcast(intent)
    }

    fun rescheduleAll(context: Context) {
        scheduleMorningAlarm(context)
        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        val isJadwalEnabled = prefs.getBoolean("reminder_jadwal_enabled", true)
        val minutesBefore = prefs.getInt("reminder_menit_sebelum", 30)

        if (isJadwalEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AsistenDatabase.getDatabase(context)
                    val dayName = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "Senin"
                        Calendar.TUESDAY -> "Selasa"
                        Calendar.WEDNESDAY -> "Rabu"
                        Calendar.THURSDAY -> "Kamis"
                        Calendar.FRIDAY -> "Jumat"
                        Calendar.SATURDAY -> "Sabtu"
                        else -> "Minggu"
                    }
                    val jadwalHari = db.jadwalDao().getJadwalByHariSync(dayName)
                    scheduleTodayClassReminders(context, jadwalHari, minutesBefore)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in rescheduleAll: ${e.message}")
                }
            }
        }
    }
}
