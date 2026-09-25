package ru.dedtime.app.domain

import ru.dedtime.app.data.AchievementUnlock
import ru.dedtime.app.data.Category
import ru.dedtime.app.data.Habit
import ru.dedtime.app.data.HabitLog
import ru.dedtime.app.data.Purchase
import ru.dedtime.app.data.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/** Правила экономики из ТЗ, раздел 4. Все числа — здесь. */
object Rules {
    const val DAY_BOUNDARY_HOURS = 4L
    const val BLOCK_MS = 25 * 60_000L
    const val XP_PER_MINUTE = 1
    const val BLOCK_XP = 10
    const val BLOCK_COINS = 2
    const val HABIT_COINS = 1
    const val ALL_HABITS_XP = 30
    const val ALL_HABITS_COINS = 5
    const val QUEST_XP = 50
    const val QUEST_COINS = 10
    const val ACHIEVEMENT_COINS = 25
    const val STREAK_FOCUS_MIN = 25
    const val DOUBLE_XP_MINUTES = 60
    const val PRESENCE_CHECK_MS = 3 * 60 * 60_000L
    const val PRESENCE_TIMEOUT_MS = 10 * 60_000L
    const val MIN_SESSION_MS = 60_000L

    /** Суммарный XP, нужный, чтобы перейти с уровня n на n+1 (n ≥ 1). */
    fun levelThreshold(n: Int): Long {
        val raw = 150.0 * n.toDouble().pow(1.4)
        return (raw / 10).roundToLong() * 10
    }

    fun levelFor(xp: Long): Int {
        var level = 1
        while (xp >= levelThreshold(level)) level++
        return level
    }
}

object Days {
    var zone: ZoneId = ZoneId.systemDefault()

    fun of(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).minusHours(Rules.DAY_BOUNDARY_HOURS).toLocalDate()

    fun today(now: Long = System.currentTimeMillis()): LocalDate = of(now)

    fun hourOf(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(zone).hour

    fun key(d: LocalDate): String = d.toString()
}

fun Session.activeMs(now: Long): Long {
    val end = pausedAt ?: endedAt ?: now
    return (end - startedAt - pausedMs).coerceAtLeast(0)
}

val Session.isRunning: Boolean get() = endedAt == null && pausedAt == null

/** XP и монеты одной сессии без бонуса «Двойной XP». */
fun sessionBaseXp(activeMs: Long): Long {
    val minutes = activeMs / 60_000L
    val blocks = activeMs / Rules.BLOCK_MS
    return minutes * Rules.XP_PER_MINUTE + blocks * Rules.BLOCK_XP
}

fun sessionCoins(activeMs: Long): Long = (activeMs / Rules.BLOCK_MS) * Rules.BLOCK_COINS

data class Quest(val title: String, val done: Boolean, val progress: String)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val progress: (Facts) -> Pair<Int, Int>,
)

data class AchievementState(
    val def: AchievementDef,
    val current: Int,
    val target: Int,
    val unlockedAt: Long?,
) {
    val unlocked: Boolean get() = unlockedAt != null
    val reached: Boolean get() = current >= target
}

data class Facts(
    val sessionsCount: Int,
    val anyBefore8: Boolean,
    val anyAfter23: Boolean,
    val bestStreak: Int,
    val longestUninterruptedMin: Int,
    val readingHours: Int,
    val sportSessions: Int,
    val phoenix: Boolean,
    val coins: Long,
    val purchases: Int,
    val focusSundays: Int,
)

object Achievements {
    private fun flag(b: Boolean) = (if (b) 1 else 0) to 1

    val all: List<AchievementDef> = listOf(
        AchievementDef("first_pancake", "Первый блин", "Первая сессия таймера") { flag(it.sessionsCount > 0) },
        AchievementDef("early_bird", "Ранняя пташка", "Сессия до 8:00") { flag(it.anyBefore8) },
        AchievementDef("night_owl", "Ночная сова", "Сессия после 23:00") { flag(it.anyAfter23) },
        AchievementDef("week", "Неделя в строю", "Серия 7 дней") { minOf(it.bestStreak, 7) to 7 },
        AchievementDef("month_fire", "Огонь месяца", "Серия 30 дней") { minOf(it.bestStreak, 30) to 30 },
        AchievementDef("deep_dive", "Глубокое погружение", "2 часа без пауз") { minOf(it.longestUninterruptedMin, 120) to 120 },
        AchievementDef("bookworm", "Книжный червь", "10 часов чтения") { minOf(it.readingHours, 10) to 10 },
        AchievementDef("iron", "Железный", "20 тренировок") { minOf(it.sportSessions, 20) to 20 },
        AchievementDef("phoenix", "Феникс", "Вернулся после сгоревшей серии") { flag(it.phoenix) },
        AchievementDef("miser", "Скупердяй", "Накопить 1000 монет") { minOf(it.coins, 1000L).toInt() to 1000 },
        AchievementDef("shopaholic", "Шопоголик", "10 покупок в магазине") { minOf(it.purchases, 10) to 10 },
        AchievementDef("sunday", "Воскресный воин", "4 воскресенья с фокусом") { minOf(it.focusSundays, 4) to 4 },
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

data class GameState(
    val now: Long,
    val today: LocalDate,
    val xp: Long,
    val coins: Long,
    val level: Int,
    val levelFloor: Long,
    val levelCeil: Long,
    val streak: Int,
    val bestStreak: Int,
    val freezesLeft: Int,
    val doubleMinutesLeft: Int,
    /** день → (категория → минуты) */
    val focus: Map<LocalDate, Map<Long, Int>>,
    /** день → XP */
    val dayXp: Map<LocalDate, Long>,
    val habitsDone: Map<LocalDate, Int>,
    val habitsTotal: Int,
    val quest: Quest,
    val achievements: List<AchievementState>,
    val sessionsCount: Int,
) {
    val todayFocus: Map<Long, Int> get() = focus[today] ?: emptyMap()
    val todayFocusMin: Int get() = todayFocus.values.sum()
    val todayHabitsDone: Int get() = habitsDone[today] ?: 0
    fun focusMin(d: LocalDate): Int = focus[d]?.values?.sum() ?: 0
    val newlyReached: List<AchievementState> get() = achievements.filter { it.reached && !it.unlocked }
}

object Engine {

    fun habitDone(h: Habit, value: Int): Boolean =
        if (h.kind == Habit.KIND_COUNTER) value >= h.target else value >= 1

    fun compute(
        sessions: List<Session>,
        habits: List<Habit>,
        logs: List<HabitLog>,
        purchases: List<Purchase>,
        unlocks: List<AchievementUnlock>,
        categories: List<Category>,
        now: Long = System.currentTimeMillis(),
    ): GameState {
        val today = Days.today(now)
        var xp = 0L
        var coins = 0L
        val dayXp = HashMap<LocalDate, Long>()
        val focus = HashMap<LocalDate, HashMap<Long, Int>>()
        fun addXp(d: LocalDate, v: Long) { xp += v; dayXp[d] = (dayXp[d] ?: 0L) + v }

        // Сессии и «Двойной XP»
        val doubles = purchases.filter { it.code == "double" }.sortedBy { it.at }
        var doubleIdx = 0
        var doublePool = 0L
        var counted = 0
        val sorted = sessions.sortedBy { it.startedAt }
        for (s in sorted) {
            val ms = s.activeMs(now)
            if (s.endedAt != null && ms < Rules.MIN_SESSION_MS) continue
            while (doubleIdx < doubles.size && doubles[doubleIdx].at <= s.startedAt) {
                doublePool += Rules.DOUBLE_XP_MINUTES; doubleIdx++
            }
            val minutes = ms / 60_000L
            val bonus = minOf(doublePool, minutes)
            doublePool -= bonus
            val d = Days.of(s.startedAt)
            addXp(d, sessionBaseXp(ms) + bonus)
            coins += sessionCoins(ms)
            if (minutes > 0) {
                val m = focus.getOrPut(d) { HashMap() }
                m[s.categoryId] = (m[s.categoryId] ?: 0) + minutes.toInt()
                counted++
            }
        }
        // Покупки «Двойного XP» после последней сессии ещё не потрачены
        while (doubleIdx < doubles.size) { doublePool += Rules.DOUBLE_XP_MINUTES; doubleIdx++ }

        // Привычки
        val activeHabits = habits.filter { !it.archived }
        val habitById = habits.associateBy { it.id }
        val habitsDone = HashMap<LocalDate, Int>()
        for (l in logs) {
            val h = habitById[l.habitId] ?: continue
            if (!habitDone(h, l.value)) continue
            val d = LocalDate.parse(l.date)
            addXp(d, h.xp.toLong())
            coins += Rules.HABIT_COINS
            if (!h.archived) habitsDone[d] = (habitsDone[d] ?: 0) + 1
        }
        val total = activeHabits.size
        if (total > 0) {
            for ((d, n) in habitsDone) if (n >= total) {
                addXp(d, Rules.ALL_HABITS_XP.toLong()); coins += Rules.ALL_HABITS_COINS
            }
        }

        // Задания дня (за каждый день с активностью)
        val activeDays = (focus.keys + habitsDone.keys).toSortedSet()
        for (d in activeDays) {
            if (quest(d, focus[d] ?: emptyMap(), habitsDone[d] ?: 0, total, sorted, categories).done) {
                addXp(d, Rules.QUEST_XP.toLong()); coins += Rules.QUEST_COINS
            }
        }

        // Серии
        fun dayCounted(d: LocalDate): Boolean {
            val f = focus[d]?.values?.sum() ?: 0
            val h = habitsDone[d] ?: 0
            return f >= Rules.STREAK_FOCUS_MIN || (total > 0 && h * 2 >= total)
        }
        val first = activeDays.firstOrNull()
        val freezes = purchases.count { it.code == "freeze" }
        var freezesLeft = freezes
        var streak = 0
        if (first != null) {
            var d = if (dayCounted(today)) today else today.minusDays(1)
            while (!d.isBefore(first)) {
                if (dayCounted(d)) streak++
                else if (freezesLeft > 0) { freezesLeft--; streak++ }
                else break
                d = d.minusDays(1)
            }
        }
        // Серии без заморозок — для рекорда и «Феникса»
        val runs = ArrayList<Int>()
        if (first != null) {
            var d: LocalDate = first
            var run = 0
            while (!d.isAfter(today)) {
                if (dayCounted(d)) run++ else { if (run > 0) runs.add(run); run = 0 }
                d = d.plusDays(1)
            }
            if (run > 0) runs.add(run)
        }
        val bestStreak = maxOf(runs.maxOrNull() ?: 0, streak)
        var phoenix = false
        var seenBig = false
        for ((i, r) in runs.withIndex()) {
            if (seenBig && r >= 3) phoenix = true
            if (r >= 7 && i < runs.size - 1) seenBig = true
        }

        // Факты для ачивок
        val catName = categories.associate { it.id to it.name }
        val reading = sorted.filter { catName[it.categoryId] == "Чтение" }.sumOf { it.activeMs(now) }
        val sport = sorted.count { catName[it.categoryId] == "Спорт" && it.activeMs(now) >= Rules.MIN_SESSION_MS }
        val real = sorted.filter { it.activeMs(now) >= Rules.MIN_SESSION_MS }
        val sundays = focus.count { (d, m) -> d.dayOfWeek == DayOfWeek.SUNDAY && m.values.sum() >= 60 }
        val spent = purchases.sumOf { it.price.toLong() }
        coins += unlocks.size.toLong() * Rules.ACHIEVEMENT_COINS
        coins -= spent

        val facts = Facts(
            sessionsCount = real.size,
            anyBefore8 = real.any { Days.hourOf(it.startedAt) < 8 && Days.hourOf(it.startedAt) >= Rules.DAY_BOUNDARY_HOURS },
            anyAfter23 = real.any {
                val end = it.endedAt ?: now
                Days.hourOf(end) >= 23 || Days.hourOf(it.startedAt) >= 23 || Days.of(end) != Days.of(it.startedAt) ||
                    Days.hourOf(end) < Rules.DAY_BOUNDARY_HOURS
            },
            bestStreak = bestStreak,
            longestUninterruptedMin = (real.filter { it.pausedMs == 0L && it.pausedAt == null }
                .maxOfOrNull { it.activeMs(now) } ?: 0L).div(60_000L).toInt(),
            readingHours = (reading / 3_600_000L).toInt(),
            sportSessions = sport,
            phoenix = phoenix,
            coins = coins,
            purchases = purchases.size,
            focusSundays = sundays,
        )
        val unlockMap = unlocks.associate { it.achievementId to it.unlockedAt }
        val ach = Achievements.all.map { def ->
            val (cur, target) = def.progress(facts)
            val ua = unlockMap[def.id]
            AchievementState(def, if (ua != null) target else cur, target, ua)
        }

        val level = Rules.levelFor(xp)
        return GameState(
            now = now,
            today = today,
            xp = xp,
            coins = coins,
            level = level,
            levelFloor = if (level == 1) 0 else Rules.levelThreshold(level - 1),
            levelCeil = Rules.levelThreshold(level),
            streak = streak,
            bestStreak = bestStreak,
            freezesLeft = freezesLeft,
            doubleMinutesLeft = doublePool.toInt(),
            focus = focus,
            dayXp = dayXp,
            habitsDone = habitsDone,
            habitsTotal = total,
            quest = quest(today, focus[today] ?: emptyMap(), habitsDone[today] ?: 0, total, sorted, categories),
            achievements = ach,
            sessionsCount = real.size,
        )
    }

    /** Задание дня детерминировано по дате. */
    fun quest(
        d: LocalDate,
        focus: Map<Long, Int>,
        habitsDone: Int,
        habitsTotal: Int,
        sessions: List<Session>,
        categories: List<Category>,
    ): Quest {
        val focusMin = focus.values.sum()
        return when (Math.floorMod(d.toEpochDay(), 5L).toInt()) {
            0 -> {
                val h = minOf(habitsDone, 3)
                val f = minOf(focusMin, 120)
                Quest("3 привычки + 2 часа фокуса", h >= 3 && f >= 120, "${h}/3 · ${f / 60} ч ${f % 60} м / 2 ч")
            }
            1 -> {
                val ok = sessions.any {
                    Days.of(it.startedAt) == d && Days.hourOf(it.startedAt) < 10 &&
                        it.activeMs(System.currentTimeMillis()) >= Rules.BLOCK_MS
                }
                Quest("Блок 25 минут до 10:00", ok, if (ok) "1/1" else "0/1")
            }
            2 -> {
                val readingId = categories.firstOrNull { it.name == "Чтение" }?.id
                val m = minOf(readingId?.let { focus[it] } ?: 0, 60)
                Quest("Час на чтение", m >= 60, "$m/60 мин")
            }
            3 -> {
                val ok = habitsTotal > 0 && habitsDone >= habitsTotal
                Quest("Закрой все привычки дня", ok, "$habitsDone/$habitsTotal")
            }
            else -> {
                val f = minOf(focusMin, 180)
                Quest("3 часа фокуса", f >= 180, "${f / 60} ч ${f % 60} м / 3 ч")
            }
        }
    }

    fun minutesLabel(min: Int): String = "${min / 60} ч ${(min % 60).toString().padStart(2, '0')}"

    /** Минуты по категориям за произвольный набор дней. */
    fun sumFocus(state: GameState, days: Iterable<LocalDate>): Map<Long, Int> {
        val out = HashMap<Long, Int>()
        for (d in days) state.focus[d]?.forEach { (k, v) -> out[k] = (out[k] ?: 0) + v }
        return out
    }

    fun lastDays(today: LocalDate, n: Int): List<LocalDate> = (n - 1 downTo 0).map { today.minusDays(it.toLong()) }

    fun lastMonths(today: LocalDate, n: Int): List<YearMonth> {
        val ym = YearMonth.from(today)
        return (n - 1 downTo 0).map { ym.minusMonths(it.toLong()) }
    }

    fun daysOf(ym: YearMonth): List<LocalDate> = (1..ym.lengthOfMonth()).map { ym.atDay(it) }

    @Suppress("unused")
    fun roundDown(v: Double) = floor(v)
}
