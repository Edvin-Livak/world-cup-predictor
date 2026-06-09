package com.sned.worldcuppredictor.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sned.worldcuppredictor.R
import com.sned.worldcuppredictor.storage.PredictionStorage
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PredictionReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val storage = PredictionStorage(applicationContext)

        val matches = storage.matchesFlow.first()
        val predictions = storage.predictionsFlow.first()

        val tomorrow = LocalDate.now().plusDays(1)

        val zone = ZoneId.systemDefault()

        val tomorrowMatches = matches.filter { match ->
            try {
                val localDate = Instant
                    .parse(match.kickoffTime)
                    .atZone(zone)
                    .toLocalDate()

                localDate == tomorrow
            } catch (e: Exception) {
                false
            }
        }

        val missingPredictions = tomorrowMatches.filter { match ->
            predictions[match.id] == null
        }

        if (missingPredictions.isEmpty()) {
            return Result.success()
        }

        showReminder(missingPredictions.size)

        return Result.success()
    }

    private fun showReminder(missingCount: Int) {
        val channelId = "prediction_reminders"

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            applicationContext.getString(R.string.notification_channel_prediction_reminders),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val text =
            if (missingCount == 1) {
                applicationContext.getString(R.string.notification_one_match)
            } else {
                applicationContext.getString(
                    R.string.notification_multiple_matches,
                    missingCount
                )
            }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(1001, notification)
    }
}