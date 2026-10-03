package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AsistenDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ReminderReceiver"
        const val ACTION_MORNING_REMINDER = "com.example.notification.ACTION_MORNING_REMINDER"
        const val ACTION_PRE_CLASS_REMINDER = "com.example.notification.ACTION_PRE_CLASS_REMINDER"
        const val ACTION_TEST_REMINDER = "com.example.notification.ACTION_TEST_REMINDER"

        const val EXTRA_MAPEL = "extra_mapel"
        const val EXTRA_KELAS = "extra_kelas"
        const val EXTRA_JAM_MULAI = "extra_jam_mulai"
        const val EXTRA_MENIT_SEBELUM = "extra_menit_sebelum"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)

        when (action) {
            ACTION_MORNING_REMINDER -> {
                val isPagiEnabled = prefs.getBoolean("reminder_pagi_enabled", true)
                val isJadwalEnabled = prefs.getBoolean("reminder_jadwal_enabled", true)
                val minutesBefore = prefs.getInt("reminder_menit_sebelum", 30)

                // Reschedule for next day
                ReminderScheduler.scheduleMorningAlarm(context)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AsistenDatabase.getDatabase(context)
                        val dayName = getTodayDayName()
                        val jadwalHari = db.jadwalDao().getJadwalByHariSync(dayName)

                        if (isPagiEnabled && jadwalHari.isNotEmpty()) {
                            val lines = jadwalHari.joinToString("\n") {
                                "• ${it.jamMulai}-${it.jamSelesai} ${it.mapel}"
                            }
                            NotificationHelper.showMorningScheduleNotification(context, dayName, lines)
                        }

                        // Schedule pre-class reminders for today
                        if (isJadwalEnabled && jadwalHari.isNotEmpty()) {
                            ReminderScheduler.scheduleTodayClassReminders(context, jadwalHari, minutesBefore)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in morning reminder: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_PRE_CLASS_REMINDER -> {
                val isJadwalEnabled = prefs.getBoolean("reminder_jadwal_enabled", true)
                if (!isJadwalEnabled) return

                val mapel = intent.getStringExtra(EXTRA_MAPEL) ?: "Pelajaran"
                val kelas = intent.getIntExtra(EXTRA_KELAS, 4)
                val jamMulai = intent.getStringExtra(EXTRA_JAM_MULAI) ?: ""
                val menit = intent.getIntExtra(EXTRA_MENIT_SEBELUM, 30)
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, NotificationHelper.NOTIFICATION_ID_CLASS_BASE)

                val title = "⏰ $menit menit lagi:"
                val message = "$mapel Kelas $kelas ($jamMulai)"

                NotificationHelper.showPreClassNotification(context, notifId, title, message)
            }

            ACTION_TEST_REMINDER -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AsistenDatabase.getDatabase(context)
                        val dayName = getTodayDayName()
                        val jadwalHari = db.jadwalDao().getJadwalByHariSync(dayName)

                        val lines = if (jadwalHari.isNotEmpty()) {
                            jadwalHari.joinToString("\n") { "• ${it.jamMulai}-${it.jamSelesai} ${it.mapel}" }
                        } else {
                            "• 07:35-09:20 Matematika\n• 09:55-11:05 B. Indonesia\n• 11:05-12:15 IPAS"
                        }
                        NotificationHelper.showMorningScheduleNotification(context, dayName, lines)

                        val sampleJadwal = jadwalHari.firstOrNull()
                        val mapel = sampleJadwal?.mapel ?: "Matematika"
                        val kelas = sampleJadwal?.kelas ?: 4
                        val jam = sampleJadwal?.jamMulai ?: "07:35"
                        NotificationHelper.showPreClassNotification(
                            context,
                            NotificationHelper.NOTIFICATION_ID_CLASS_BASE,
                            "⏰ 30 menit lagi:",
                            "$mapel Kelas $kelas ($jam)"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in test reminder: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }
    }
}
