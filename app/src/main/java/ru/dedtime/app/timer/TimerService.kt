package ru.dedtime.app.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.dedtime.app.MainActivity
import ru.dedtime.app.R
import ru.dedtime.app.app
import ru.dedtime.app.data.Session
import ru.dedtime.app.domain.activeMs
import ru.dedtime.app.domain.sessionBaseXp

class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground должен прозвучать сразу после startForegroundService
        startFg(placeholder())
        val action = intent?.action
        scope.launch {
            val repo = app.repo
            when (action) {
                ACTION_TOGGLE -> repo.togglePause()
                ACTION_STOP -> repo.stopTimer()
                ACTION_CONFIRM -> repo.confirmPresence()
            }
            refresh()
        }
        return START_STICKY
    }

    private suspend fun refresh() {
        val s = app.repo.active()
        if (s == null) {
            stopWatcher()
            nm().cancel(ID_PRESENCE)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val cat = app.repo.categoriesNow().firstOrNull { it.id == s.categoryId }?.name ?: "Фокус"
        startFg(build(s, cat))
        if (s.askedAt == null) nm().cancel(ID_PRESENCE)
        startWatcher()
    }

    private fun startWatcher() {
        if (watcher?.isActive == true) return
        watcher = scope.launch {
            while (isActive) {
                delay(30_000)
                val ask = app.repo.presenceTick()
                if (ask) nm().notify(ID_PRESENCE, presence()) else nm().cancel(ID_PRESENCE)
                if (app.repo.active() == null) break
            }
        }
    }

    private fun stopWatcher() { watcher?.cancel(); watcher = null }

    private fun startFg(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, ID_TIMER, n, type)
    }

    private fun nm() = getSystemService(NotificationManager::class.java)

    private fun pi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code, Intent(this, TimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, "timer"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun placeholder(): Notification =
        NotificationCompat.Builder(this, CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Дедтайм")
            .setContentText("Таймер")
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun build(s: Session, cat: String): Notification {
        val now = System.currentTimeMillis()
        val ms = s.activeMs(now)
        val title = if (s.note.isNotBlank()) "$cat · ${s.note}" else cat
        val b = NotificationCompat.Builder(this, CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (s.pausedAt == null) {
            b.setContentTitle(title)
                .setContentText("Идёт фокус · +${sessionBaseXp(ms)} XP к этому моменту")
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(now - ms)
                .addAction(0, "Пауза", pi(ACTION_TOGGLE, 1))
        } else {
            b.setContentTitle("Пауза · $title")
                .setContentText(formatClock(ms))
                .setShowWhen(false)
                .addAction(0, "Продолжить", pi(ACTION_TOGGLE, 1))
        }
        b.addAction(0, "Стоп", pi(ACTION_STOP, 2))
        return b.build()
    }

    private fun presence(): Notification =
        NotificationCompat.Builder(this, CH_PRESENCE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Ты тут?")
            .setContentText("Таймер идёт больше 3 часов. Без ответа через 10 минут дед поставит паузу.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi(ACTION_CONFIRM, 3))
            .addAction(0, "Я тут", pi(ACTION_CONFIRM, 3))
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CH_TIMER = "timer"
        const val CH_PRESENCE = "presence"
        const val ID_TIMER = 7
        const val ID_PRESENCE = 8
        const val ACTION_SYNC = "ru.dedtime.SYNC"
        const val ACTION_TOGGLE = "ru.dedtime.TOGGLE"
        const val ACTION_STOP = "ru.dedtime.STOP"
        const val ACTION_CONFIRM = "ru.dedtime.CONFIRM"

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Таймер", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_PRESENCE, "Ты тут?", NotificationManager.IMPORTANCE_HIGH))
        }

        /** Обновить уведомление по текущему состоянию таймера в базе. */
        fun sync(context: Context) {
            val i = Intent(context, TimerService::class.java).setAction(ACTION_SYNC)
            try {
                ContextCompat.startForegroundService(context, i)
            } catch (e: Exception) {
                // Запуск из фона запрещён системой — уведомление обновится при следующем открытии приложения
            }
        }

        fun formatClock(ms: Long): String {
            val s = ms / 1000
            return "%d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
        }
    }
}
