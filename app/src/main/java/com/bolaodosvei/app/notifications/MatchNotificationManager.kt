package com.bolaodosvei.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bolaodosvei.app.MainActivity
import com.bolaodosvei.app.R
import com.bolaodosvei.app.model.GoalEvent
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.Match
import com.bolaodosvei.app.model.NotificationSettings

class MatchNotificationManager(private val context: Context) {

    init {
        createChannels()
    }

    private fun channelIdFor(league: League): String = "bolao_league_${league.id}"

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            League.entries.forEach { league ->
                val channel = NotificationChannel(
                    channelIdFor(league),
                    "Jogos - ${league.displayName}",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações de início de jogo e gols da liga ${league.displayName}"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyMatchStarted(match: Match, settings: NotificationSettings): Boolean {
        if (!settings.isEnabled(match.league)) return false
        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            match.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelIdFor(match.league))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bola rolando • ${match.league.displayName}")
            .setContentText("${match.homeTeam.name} x ${match.awayTeam.name} já começou!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${match.homeTeam.name} x ${match.awayTeam.name} acabou de começar no ${match.stadium}. Confira os palpites no Bolão dos Véi!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(match.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun notifyGoalScored(event: GoalEvent, settings: NotificationSettings): Boolean {
        if (!settings.isEnabled(event.match.league)) return false
        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scoreLine =
            "${event.match.homeTeam.shortName} ${event.match.homeScore} x ${event.match.awayScore} ${event.match.awayTeam.shortName}"

        val notification = NotificationCompat.Builder(context, channelIdFor(event.match.league))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GOOOOOL! ${event.scoringTeamName} (${event.minute}')")
            .setContentText("$scoreLine • Gol de ${event.scorerName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(event.id.toInt(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
