package ru.dedtime.app.work

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.dedtime.app.app
import ru.dedtime.app.data.Repository
import ru.dedtime.app.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.updateAllNow(applicationContext)
        return Result.success()
    }
}

class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.app
        val s = app.settings.current()
        val tree = s.backupTree ?: return Result.success()
        if (!s.autoBackup) return Result.success()
        return try {
            Backups.writeToTree(applicationContext, Uri.parse(tree), app.repo.exportJson())
            app.settings.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object Backups {
    private const val KEEP = 4

    fun writeToTree(context: Context, tree: Uri, text: String) {
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeDocId)
        val name = Repository.backupFileName()
        val doc = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("Не удалось создать файл")
        resolver.openOutputStream(doc, "wt")?.use { it.write(text.toByteArray()) }
        prune(context, tree, treeDocId)
    }

    private fun prune(context: Context, tree: Uri, treeDocId: String) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeDocId)
        val found = ArrayList<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                if (name.startsWith("dedtime_backup_")) found.add(id to name)
            }
        }
        found.sortedByDescending { it.second }.drop(KEEP).forEach { (id, _) ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
        }
    }
}

object Work {
    fun scheduleWidgetRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "widget_refresh", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build(),
        )
    }

    fun setAutoBackup(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            wm.enqueueUniquePeriodicWork(
                "auto_backup", ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build(),
            )
        } else wm.cancelUniqueWork("auto_backup")
    }
}
