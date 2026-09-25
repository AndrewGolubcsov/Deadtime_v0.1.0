package ru.dedtime.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val avatarId: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "accent" = акцентный цвет темы, иначе #RRGGBB */
    val colorHex: String,
    val sortOrder: Int,
    val archived: Boolean = false,
)

@Serializable
@Entity(tableName = "session", indices = [Index("startedAt")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val note: String = "",
    val startedAt: Long,
    val endedAt: Long? = null,
    val pausedMs: Long = 0,
    /** Начало текущей паузы, если сессия на паузе */
    val pausedAt: Long? = null,
    /** Последнее подтверждение «Ты тут?» (или старт / продолжение) */
    val lastConfirmAt: Long = startedAt,
    /** Когда задан вопрос «Ты тут?» и ответа пока нет */
    val askedAt: Long? = null,
)

@Serializable
@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** CHECK или COUNTER */
    val kind: String = KIND_CHECK,
    val target: Int = 1,
    val xp: Int = 20,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
) {
    companion object {
        const val KIND_CHECK = "CHECK"
        const val KIND_COUNTER = "COUNTER"
    }
}

@Serializable
@Entity(tableName = "habit_log", indices = [Index(value = ["habitId", "date"], unique = true)])
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** yyyy-MM-dd, день по границе 04:00 */
    val date: String,
    val value: Int,
)

@Serializable
@Entity(tableName = "reward")
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val price: Int,
    val isCustom: Boolean,
    /** freeze / double для встроенных товаров */
    val code: String? = null,
)

@Serializable
@Entity(tableName = "purchase")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val code: String? = null,
    val title: String = "",
    val price: Int,
    val at: Long,
)

@Serializable
@Entity(tableName = "achievement_unlock")
data class AchievementUnlock(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
)
