package ru.dedtime.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class UiSettings(
    /** light, warm, dark, system */
    val theme: String = "system",
    val accent: Long = 0xFFC2410C,
    /** manrope, onest, serif */
    val font: String = "manrope",
    val textScale: Float = 1f,
    val radius: Int = 14,
    val anim: Boolean = true,
    val compact: Boolean = false,
    val autoBackup: Boolean = false,
    val backupTree: String? = null,
    val lastBackupAt: Long = 0,
)

class SettingsStore(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val accent = longPreferencesKey("accent")
        val font = stringPreferencesKey("font")
        val scale = floatPreferencesKey("text_scale")
        val radius = intPreferencesKey("radius")
        val anim = booleanPreferencesKey("anim")
        val compact = booleanPreferencesKey("compact")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val tree = stringPreferencesKey("backup_tree")
        val lastBackup = longPreferencesKey("last_backup")
    }

    val flow: Flow<UiSettings> = context.dataStore.data.map { p -> toSettings(p) }

    suspend fun current(): UiSettings = flow.first()

    private fun toSettings(p: Preferences) = UiSettings(
        theme = p[K.theme] ?: "system",
        accent = p[K.accent] ?: 0xFFC2410C,
        font = p[K.font] ?: "manrope",
        textScale = p[K.scale] ?: 1f,
        radius = p[K.radius] ?: 14,
        anim = p[K.anim] ?: true,
        compact = p[K.compact] ?: false,
        autoBackup = p[K.autoBackup] ?: false,
        backupTree = p[K.tree],
        lastBackupAt = p[K.lastBackup] ?: 0,
    )

    suspend fun update(block: (UiSettings) -> UiSettings) {
        context.dataStore.edit { p ->
            val n = block(toSettings(p))
            p[K.theme] = n.theme
            p[K.accent] = n.accent
            p[K.font] = n.font
            p[K.scale] = n.textScale
            p[K.radius] = n.radius
            p[K.anim] = n.anim
            p[K.compact] = n.compact
            p[K.autoBackup] = n.autoBackup
            val tree = n.backupTree
            if (tree != null) p[K.tree] = tree else p.remove(K.tree)
            p[K.lastBackup] = n.lastBackupAt
        }
    }
}
