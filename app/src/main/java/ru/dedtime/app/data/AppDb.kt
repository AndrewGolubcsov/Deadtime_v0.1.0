package ru.dedtime.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * ВАЖНО: при изменении схемы увеличь VERSION, добавь миграцию
 * (autoMigrations или Migration) и тест. fallbackToDestructiveMigration не использовать.
 */
@Database(
    entities = [
        Profile::class, Category::class, Session::class, Habit::class,
        HabitLog::class, Reward::class, Purchase::class, AchievementUnlock::class,
    ],
    version = AppDb.VERSION,
    exportSchema = true,
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        const val VERSION = 1

        fun create(context: Context): AppDb =
            Room.databaseBuilder(context, AppDb::class.java, "dedtime.db").build()
    }
}
