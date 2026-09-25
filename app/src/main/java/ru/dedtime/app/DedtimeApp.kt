package ru.dedtime.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.dedtime.app.data.AppDb
import ru.dedtime.app.data.Repository
import ru.dedtime.app.data.SettingsStore
import ru.dedtime.app.timer.TimerService
import ru.dedtime.app.work.Work

class DedtimeApp : Application() {
    val appScope = CoroutineScope(SupervisorJob())
    lateinit var db: AppDb
        private set
    lateinit var repo: Repository
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDb.create(this)
        repo = Repository(this, db)
        settings = SettingsStore(this)
        TimerService.createChannels(this)
        Work.scheduleWidgetRefresh(this)

        // Фиксируем новые ачивки, как только условие выполнено
        appScope.launch {
            repo.state
                .map { st -> st.newlyReached.map { it.def.id } to st }
                .distinctUntilChanged { a, b -> a.first == b.first }
                .collect { (ids, st) -> if (ids.isNotEmpty()) repo.recordUnlocks(st) }
        }
        // Восстановить уведомление таймера после перезапуска процесса
        appScope.launch {
            if (repo.active() != null) TimerService.sync(this@DedtimeApp)
        }
    }
}

val android.content.Context.app: DedtimeApp get() = applicationContext as DedtimeApp
