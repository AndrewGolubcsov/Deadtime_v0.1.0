package ru.dedtime.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.dedtime.app.BuildConfig
import ru.dedtime.app.domain.Days
import ru.dedtime.app.domain.Engine
import ru.dedtime.app.domain.GameState
import ru.dedtime.app.domain.Rules
import ru.dedtime.app.domain.activeMs
import ru.dedtime.app.timer.TimerService
import ru.dedtime.app.widget.WidgetUpdater
import java.time.Instant
import java.time.ZoneId

@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val profile: Profile? = null,
    val categories: List<Category> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val rewards: List<Reward> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val unlocks: List<AchievementUnlock> = emptyList(),
)

data class BackupPreview(
    val file: BackupFile,
    val name: String,
    val avatarId: String,
    val createdAt: Long,
    val level: Int,
    val streak: Int,
    val sessions: Int,
    val coins: Long,
)

class Repository(private val context: Context, private val db: AppDb) {
    private val dao = db.dao()

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val profile: Flow<Profile?> = dao.profile()
    val categories: Flow<List<Category>> = dao.categories()
    val habits: Flow<List<Habit>> = dao.habits()
    val logs: Flow<List<HabitLog>> = dao.logs()
    val rewards: Flow<List<Reward>> = dao.rewards()
    val activeSession: Flow<Session?> = dao.activeSession()

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private data class Raw(
        val s: List<Session>, val h: List<Habit>, val l: List<HabitLog>,
        val p: List<Purchase>, val u: List<AchievementUnlock>,
    )

    val state: Flow<GameState> = combine(
        combine(dao.sessions(), dao.habits(), dao.logs(), dao.purchases(), dao.unlocks()) { s, h, l, p, u -> Raw(s, h, l, p, u) },
        dao.categories(),
        ticker,
    ) { r, c, now -> Engine.compute(r.s, r.h, r.l, r.p, r.u, c, now) }

    suspend fun snapshot(now: Long = System.currentTimeMillis()): GameState = Engine.compute(
        dao.allSessions(), dao.allHabits(), dao.allLogs(), dao.allPurchases(), dao.allUnlocks(),
        dao.allCategories(), now,
    )

    suspend fun habitsNow(): List<Habit> = dao.allHabits().filter { !it.archived }
    suspend fun categoriesNow(): List<Category> = dao.allCategories().filter { !it.archived }
    suspend fun logValue(habitId: Long, date: String): Int = dao.getLog(habitId, date)?.value ?: 0
    suspend fun active(): Session? = dao.getActive()
    suspend fun profileNow(): Profile? = dao.getProfile()

    private fun changed() = WidgetUpdater.refresh(context)

    // ——— Онбординг ———

    suspend fun createProfile(name: String, avatarId: String) {
        db.withTransaction {
            dao.upsertProfile(Profile(name = name.ifBlank { "Друг" }, avatarId = avatarId, createdAt = System.currentTimeMillis()))
            if (dao.allCategories().isEmpty()) {
                listOf(
                    "Учёба" to "accent", "Спорт" to "#1D4ED8", "Чтение" to "#F59E68",
                    "Проект" to "#93B4F5", "Другое" to "#8A877F",
                ).forEachIndexed { i, (n, c) -> dao.insertCategory(Category(name = n, colorHex = c, sortOrder = i)) }
            }
            if (dao.allRewards().isEmpty()) {
                dao.insertReward(Reward(title = "Заморозка серии", price = 50, isCustom = false, code = "freeze"))
                dao.insertReward(Reward(title = "Двойной XP на час фокуса", price = 120, isCustom = false, code = "double"))
            }
            if (dao.allHabits().isEmpty()) {
                dao.insertHabit(Habit(title = "Выпить стакан воды", xp = 10))
            }
        }
        changed()
    }

    suspend fun updateProfile(name: String, avatarId: String) {
        val p = dao.getProfile() ?: return
        dao.upsertProfile(p.copy(name = name.ifBlank { p.name }, avatarId = avatarId))
        changed()
    }

    // ——— Привычки ———

    suspend fun addHabit(title: String, kind: String, target: Int, xp: Int) {
        val order = (dao.allHabits().maxOfOrNull { it.sortOrder } ?: 0) + 1
        dao.insertHabit(Habit(title = title, kind = kind, target = target.coerceAtLeast(1), xp = xp, sortOrder = order))
        changed()
    }

    suspend fun updateHabit(h: Habit) { dao.updateHabit(h); changed() }

    suspend fun archiveHabit(h: Habit) { dao.updateHabit(h.copy(archived = true)); changed() }

    /** CHECK — переключить, COUNTER — +1. */
    suspend fun tapHabit(habitId: Long) {
        val h = dao.getHabit(habitId) ?: return
        val date = Days.key(Days.today())
        val cur = dao.getLog(habitId, date)
        if (h.kind == Habit.KIND_CHECK) {
            if (cur != null && cur.value > 0) dao.deleteLog(habitId, date)
            else dao.putLog(HabitLog(id = cur?.id ?: 0, habitId = habitId, date = date, value = 1))
        } else {
            dao.putLog(HabitLog(id = cur?.id ?: 0, habitId = habitId, date = date, value = (cur?.value ?: 0) + 1))
        }
        changed()
    }

    suspend fun decHabit(habitId: Long) {
        val date = Days.key(Days.today())
        val cur = dao.getLog(habitId, date) ?: return
        if (cur.value <= 1) dao.deleteLog(habitId, date) else dao.putLog(cur.copy(value = cur.value - 1))
        changed()
    }

    // ——— Категории ———

    suspend fun addCategory(name: String, colorHex: String): Long {
        val order = (dao.allCategories().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = dao.insertCategory(Category(name = name, colorHex = colorHex, sortOrder = order))
        changed()
        return id
    }

    // ——— Таймер ———

    suspend fun lastCategoryId(): Long? =
        dao.getActive()?.categoryId ?: dao.lastFinished()?.categoryId ?: dao.allCategories().firstOrNull()?.id

    suspend fun startTimer(categoryId: Long, note: String) {
        val now = System.currentTimeMillis()
        dao.getActive()?.let { finish(it, now) }
        dao.insertSession(Session(categoryId = categoryId, note = note.trim(), startedAt = now, lastConfirmAt = now))
        timerChanged()
    }

    suspend fun pauseTimer(at: Long = System.currentTimeMillis()) {
        val s = dao.getActive() ?: return
        if (s.pausedAt != null) return
        dao.updateSession(s.copy(pausedAt = at, askedAt = null))
        timerChanged()
    }

    suspend fun resumeTimer() {
        val s = dao.getActive() ?: return
        val p = s.pausedAt ?: return
        val now = System.currentTimeMillis()
        dao.updateSession(s.copy(pausedMs = s.pausedMs + (now - p), pausedAt = null, lastConfirmAt = now, askedAt = null))
        timerChanged()
    }

    suspend fun togglePause() {
        val s = dao.getActive() ?: return
        if (s.pausedAt == null) pauseTimer() else resumeTimer()
    }

    /** Возвращает XP сохранённой сессии (0, если сессия была короче минуты). */
    suspend fun stopTimer(): Long {
        val s = dao.getActive() ?: return 0
        val xp = finish(s, System.currentTimeMillis())
        timerChanged()
        return xp
    }

    private suspend fun finish(s: Session, now: Long): Long {
        val closed = if (s.pausedAt != null) {
            s.copy(endedAt = s.pausedAt, askedAt = null)
        } else s.copy(endedAt = now, askedAt = null)
        val ms = closed.activeMs(now)
        return if (ms < Rules.MIN_SESSION_MS) {
            dao.deleteSession(s.id); 0
        } else {
            dao.updateSession(closed)
            ru.dedtime.app.domain.sessionBaseXp(ms)
        }
    }

    suspend fun confirmPresence() {
        val s = dao.getActive() ?: return
        dao.updateSession(s.copy(lastConfirmAt = System.currentTimeMillis(), askedAt = null))
        timerChanged()
    }

    /** Проверка «Ты тут?». Возвращает true, если нужно показать вопрос. */
    suspend fun presenceTick(now: Long = System.currentTimeMillis()): Boolean {
        val s = dao.getActive() ?: return false
        if (s.pausedAt != null) return false
        val asked = s.askedAt
        if (asked == null) {
            if (now - s.lastConfirmAt >= Rules.PRESENCE_CHECK_MS) {
                dao.updateSession(s.copy(askedAt = now))
                return true
            }
            return false
        }
        if (now - asked >= Rules.PRESENCE_TIMEOUT_MS) {
            // Время после вопроса не засчитывается
            dao.updateSession(s.copy(pausedAt = asked, askedAt = null))
            timerChanged()
            return false
        }
        return true
    }

    private fun timerChanged() {
        TimerService.sync(context)
        changed()
    }

    // ——— Награды ———

    suspend fun buy(reward: Reward): Boolean {
        val st = snapshot()
        if (st.coins < reward.price) return false
        dao.insertPurchase(Purchase(rewardId = reward.id, code = reward.code, title = reward.title, price = reward.price, at = System.currentTimeMillis()))
        changed()
        return true
    }

    suspend fun addCustomReward(title: String, price: Int) {
        dao.insertReward(Reward(title = title, price = price.coerceAtLeast(1), isCustom = true))
    }

    suspend fun deleteCustomReward(id: Long) = dao.deleteCustomReward(id)

    suspend fun recordUnlocks(state: GameState) {
        val fresh = state.newlyReached
        if (fresh.isEmpty()) return
        val now = System.currentTimeMillis()
        fresh.forEach { dao.insertUnlock(AchievementUnlock(it.def.id, now)) }
        changed()
    }

    // ——— Резервные копии ———

    suspend fun exportJson(): String {
        val file = BackupFile(
            schemaVersion = AppDb.VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = System.currentTimeMillis(),
            profile = dao.getProfile(),
            categories = dao.allCategories(),
            sessions = dao.allSessions(),
            habits = dao.allHabits(),
            habitLogs = dao.allLogs(),
            rewards = dao.allRewards(),
            purchases = dao.allPurchases(),
            unlocks = dao.allUnlocks(),
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Разбирает файл и считает превью. Бросает IllegalArgumentException с понятным текстом. */
    fun parseBackup(text: String): BackupPreview {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Это не файл копии Дедтайма")
        }
        if (file.schemaVersion > AppDb.VERSION) {
            throw IllegalArgumentException("Копия сделана более новой версией. Обнови приложение.")
        }
        val st = Engine.compute(file.sessions, file.habits, file.habitLogs, file.purchases, file.unlocks, file.categories)
        return BackupPreview(
            file = file,
            name = file.profile?.name ?: "Без имени",
            avatarId = file.profile?.avatarId ?: "toaster",
            createdAt = file.createdAt,
            level = st.level,
            streak = st.streak,
            sessions = st.sessionsCount,
            coins = st.coins,
        )
    }

    suspend fun restore(file: BackupFile) {
        db.withTransaction {
            dao.clearUnlocks(); dao.clearPurchases(); dao.clearRewards(); dao.clearLogs()
            dao.clearHabits(); dao.clearSessions(); dao.clearCategories(); dao.clearProfile()
            file.profile?.let { dao.upsertProfile(it) }
            dao.insertCategories(file.categories)
            dao.insertSessions(file.sessions)
            dao.insertHabits(file.habits)
            dao.insertLogs(file.habitLogs)
            dao.insertRewards(file.rewards)
            dao.insertPurchases(file.purchases)
            dao.insertUnlocks(file.unlocks)
        }
        timerChanged()
    }

    companion object {
        fun backupFileName(now: Long = System.currentTimeMillis()): String {
            val d = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
            return "dedtime_backup_$d.json"
        }
    }
}
