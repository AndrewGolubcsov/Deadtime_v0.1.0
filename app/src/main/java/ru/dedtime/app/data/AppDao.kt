package ru.dedtime.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Профиль
    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): Profile?

    @Upsert
    suspend fun upsertProfile(p: Profile)

    // Категории
    @Query("SELECT * FROM category WHERE archived = 0 ORDER BY sortOrder, id")
    fun categories(): Flow<List<Category>>

    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    suspend fun allCategories(): List<Category>

    @Insert
    suspend fun insertCategory(c: Category): Long

    // Сессии
    @Query("SELECT * FROM session ORDER BY startedAt")
    fun sessions(): Flow<List<Session>>

    @Query("SELECT * FROM session ORDER BY startedAt")
    suspend fun allSessions(): List<Session>

    @Query("SELECT * FROM session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<Session?>

    @Query("SELECT * FROM session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): Session?

    @Query("SELECT * FROM session WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun lastFinished(): Session?

    @Insert
    suspend fun insertSession(s: Session): Long

    @Update
    suspend fun updateSession(s: Session)

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // Привычки
    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY sortOrder, id")
    fun habits(): Flow<List<Habit>>

    @Query("SELECT * FROM habit ORDER BY sortOrder, id")
    suspend fun allHabits(): List<Habit>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun getHabit(id: Long): Habit?

    @Insert
    suspend fun insertHabit(h: Habit): Long

    @Update
    suspend fun updateHabit(h: Habit)

    @Query("SELECT * FROM habit_log")
    fun logs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_log")
    suspend fun allLogs(): List<HabitLog>

    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND date = :date")
    suspend fun getLog(habitId: Long, date: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLog(l: HabitLog)

    @Query("DELETE FROM habit_log WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: Long, date: String)

    // Награды
    @Query("SELECT * FROM reward ORDER BY isCustom, price")
    fun rewards(): Flow<List<Reward>>

    @Query("SELECT * FROM reward")
    suspend fun allRewards(): List<Reward>

    @Insert
    suspend fun insertReward(r: Reward): Long

    @Query("DELETE FROM reward WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomReward(id: Long)

    @Query("SELECT * FROM purchase ORDER BY at")
    fun purchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchase ORDER BY at")
    suspend fun allPurchases(): List<Purchase>

    @Insert
    suspend fun insertPurchase(p: Purchase)

    @Query("SELECT * FROM achievement_unlock")
    fun unlocks(): Flow<List<AchievementUnlock>>

    @Query("SELECT * FROM achievement_unlock")
    suspend fun allUnlocks(): List<AchievementUnlock>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(u: AchievementUnlock)

    // Для восстановления из копии
    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Query("DELETE FROM category")
    suspend fun clearCategories()

    @Query("DELETE FROM session")
    suspend fun clearSessions()

    @Query("DELETE FROM habit")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_log")
    suspend fun clearLogs()

    @Query("DELETE FROM reward")
    suspend fun clearRewards()

    @Query("DELETE FROM purchase")
    suspend fun clearPurchases()

    @Query("DELETE FROM achievement_unlock")
    suspend fun clearUnlocks()

    @Insert
    suspend fun insertCategories(items: List<Category>)

    @Insert
    suspend fun insertSessions(items: List<Session>)

    @Insert
    suspend fun insertHabits(items: List<Habit>)

    @Insert
    suspend fun insertLogs(items: List<HabitLog>)

    @Insert
    suspend fun insertRewards(items: List<Reward>)

    @Insert
    suspend fun insertPurchases(items: List<Purchase>)

    @Insert
    suspend fun insertUnlocks(items: List<AchievementUnlock>)
}
