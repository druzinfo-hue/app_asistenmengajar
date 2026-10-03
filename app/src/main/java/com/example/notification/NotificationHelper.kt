package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID_BRIEFING = "channel_briefing_mengajar"
    const val CHANNEL_ID_REMINDER = "channel_reminder_minum"
    const val CHANNEL_ID_JADWAL = "channel_pengingat_jadwal"

    const val NOTIFICATION_ID_BRIEFING = 1001
    const val NOTIFICATION_ID_REMINDER = 1002
    const val NOTIFICATION_ID_MORNING = 1003
    const val NOTIFICATION_ID_CLASS_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val briefingChannel = NotificationChannel(
                CHANNEL_ID_BRIEFING,
                "Briefing Mengajar Harian",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi persiapan jadwal materi dan bahan mengajar esok hari"
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Pengingat Kesehatan & Hidrasi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat minum air putih dan istirahat sejenak bagi guru"
            }

            val jadwalChannel = NotificationChannel(
                CHANNEL_ID_JADWAL,
                "Pengingat Jadwal Mengajar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat jadwal mengajar pagi dan peringatan sebelum masuk kelas"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(briefingChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(jadwalChannel)
        }
    }

    fun showBriefingNotification(context: Context, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BRIEFING, notification)
    }

    fun showHydrationReminder(context: Context, teacherName: String = "Guru") {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Waktunya Minum Air")
            .setContentText("Jangan lupa minum air dan istirahat sejenak, $teacherName! 💧")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    fun showMorningScheduleNotification(context: Context, dayName: String, scheduleLines: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📅 Jadwal Hari Ini ($dayName):"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_JADWAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(scheduleLines.lines().firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(scheduleLines))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_MORNING, notification)
    }

    fun showPreClassNotification(context: Context, notifId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_JADWAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notification)
    }
}
