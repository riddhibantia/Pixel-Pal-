package com.pixelpal.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pixelpal.app.R
import com.pixelpal.app.data.local.db.PixelPalDatabase
import com.pixelpal.app.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TasksWidgetEntryPoint {
    fun database(): PixelPalDatabase
}

class TasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TASK = "com.pixelpal.app.widget.TOGGLE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_ADD_TASK = "com.pixelpal.app.widget.ADD_TASK"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TasksWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, TasksWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
            // Also update home widget
            HomeWidgetProvider.updateAllWidgets(context)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) handleToggle(context, taskId)
            }
        }
    }

    private fun handleToggle(context: Context, taskId: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext, TasksWidgetEntryPoint::class.java
                )
                val db = entryPoint.database()
                val taskDao = db.taskDao()
                val bondDao = db.bondDao()
                val companionDao = db.companionDao()
                val activityDao = db.activityEventDao()

                // Get task to know companionId and current state — query directly
                // Use a simple approach: toggle via DAO
                val companion = companionDao.getPrimaryDirect() ?: return@launch
                // Check current state by querying tasks
                val tasks = mutableListOf<com.pixelpal.app.data.local.db.entity.TaskEntity>()
                // We need to find the task — use a direct query via taskDao is limited.
                // Workaround: fetch all tasks for companion and find by id
                // TaskDao.getTasks is a Flow, we need a suspend direct query. Add one if missing.
                // For now, try to toggle: attempt markDone, if already done markUndone will be called via toggle logic.
                // Simpler: we store isDone in the widget via task state at render time. But onReceive we don't know.
                // Let's add a direct query method — for now use reflection-style: try both
                // We'll query via database directly
                val cursor = db.openHelper.readableDatabase.query(
                    "SELECT isDone, companionId FROM tasks WHERE id = $taskId"
                )
                var isDone = false
                var companionId = companion.id
                if (cursor.moveToFirst()) {
                    isDone = cursor.getInt(0) != 0
                    companionId = cursor.getLong(1)
                }
                cursor.close()

                if (isDone) {
                    taskDao.markUndone(taskId)
                } else {
                    taskDao.markDone(taskId, System.currentTimeMillis())
                    // Award bond progress — same as CompleteTaskUseCase
                    val bond = bondDao.getBondDirect(companionId)
                    if (bond != null) {
                        val newLevel = (bond.level + 2).coerceAtMost(100)
                        bondDao.insertOrUpdate(
                            bond.copy(
                                level = newLevel,
                                totalInteractions = bond.totalInteractions + 1,
                                lastInteractionTime = System.currentTimeMillis()
                            )
                        )
                        // Milestone check
                        if (newLevel > bond.level && newLevel % 5 == 0) {
                            activityDao.insert(
                                com.pixelpal.app.data.local.db.entity.ActivityEventEntity(
                                    companionId = companionId,
                                    type = "BOND_LEVEL_UP",
                                    title = "${companion.name} reached Bond Level $newLevel"
                                )
                            )
                        }
                    }
                    activityDao.insert(
                        com.pixelpal.app.data.local.db.entity.ActivityEventEntity(
                            companionId = companionId,
                            type = "TASK_COMPLETED",
                            title = "Task completed"
                        )
                    )
                }
                // Refresh widgets
                updateAllWidgets(context)
            } catch (_: Exception) { }
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        Timber.d("TasksWidget: onUpdate called for widget $appWidgetId")
        scope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext, TasksWidgetEntryPoint::class.java
                )
                val db = entryPoint.database()
                val companion = db.companionDao().getPrimaryDirect()
                Timber.d("TasksWidget: companion loaded: ${companion?.name} id=${companion?.id}")
                val companionId = companion?.id ?: run {
                    Timber.w("TasksWidget: no companion found, showing empty state for widget $appWidgetId")
                    val views = RemoteViews(context.packageName, R.layout.widget_pixelpal_tasks)
                    for (i in 0..5) views.setViewVisibility(getRowId(i), android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty, "No companion yet.\nOpen PixelPal!")
                    views.setViewVisibility(R.id.widget_summary, android.view.View.GONE)
                    manager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                // Use direct DB query to avoid Flow
                val cursor = db.openHelper.readableDatabase.query(
                    "SELECT id, title, isDone FROM tasks WHERE companionId = $companionId ORDER BY isDone ASC, createdAt DESC LIMIT 6"
                )
                val tasks = mutableListOf<Triple<Long, String, Boolean>>()
                while (cursor.moveToNext()) {
                    tasks.add(Triple(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) != 0))
                }
                cursor.close()
                Timber.d("TasksWidget: found ${tasks.size} tasks for companion $companionId for widget $appWidgetId")

                val views = RemoteViews(context.packageName, R.layout.widget_pixelpal_tasks)

                // Add task button -> open app
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val openPending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_add_task, openPending)
                // Also make whole header clickable
                views.setOnClickPendingIntent(R.id.widget_task_row_0, openPending)

                if (tasks.isEmpty()) {
                    for (i in 0..5) {
                        views.setViewVisibility(getRowId(i), android.view.View.GONE)
                    }
                    views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_summary, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
                    val pendingCount = tasks.count { !it.third }
                    views.setViewVisibility(R.id.widget_summary, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_summary, "$pendingCount tasks remaining")

                    for (i in 0..5) {
                        if (i < tasks.size) {
                            val (taskId, title, isDone) = tasks[i]
                            views.setViewVisibility(getRowId(i), android.view.View.VISIBLE)
                            views.setTextViewText(getTitleId(i), title)
                            views.setTextViewText(getCheckboxId(i), if (isDone) "X" else " ")
                            // Dim completed tasks via text color
                            views.setTextColor(getTitleId(i), if (isDone) android.graphics.Color.parseColor("#991A1A2E") else android.graphics.Color.parseColor("#FF1A1A2E"))

                            val toggleIntent = Intent(context, TasksWidgetProvider::class.java).apply {
                                action = ACTION_TOGGLE_TASK
                                putExtra(EXTRA_TASK_ID, taskId)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context, taskId.toInt(), toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(getCheckboxId(i), togglePending)
                            // Also make row clickable
                            views.setOnClickPendingIntent(getRowId(i), togglePending)
                        } else {
                            views.setViewVisibility(getRowId(i), android.view.View.GONE)
                        }
                    }
                }

                manager.updateAppWidget(appWidgetId, views)
                Timber.d("TasksWidget: RemoteViews update succeeded for widget $appWidgetId")
            } catch (e: Exception) {
                Timber.e(e, "TasksWidget: update failed for widget $appWidgetId")
            }
        }
    }

    private fun getRowId(index: Int): Int = when (index) {
        0 -> R.id.widget_task_row_0
        1 -> R.id.widget_task_row_1
        2 -> R.id.widget_task_row_2
        3 -> R.id.widget_task_row_3
        4 -> R.id.widget_task_row_4
        else -> R.id.widget_task_row_5
    }

    private fun getCheckboxId(index: Int): Int = when (index) {
        0 -> R.id.widget_task_checkbox_0
        1 -> R.id.widget_task_checkbox_1
        2 -> R.id.widget_task_checkbox_2
        3 -> R.id.widget_task_checkbox_3
        4 -> R.id.widget_task_checkbox_4
        else -> R.id.widget_task_checkbox_5
    }

    private fun getTitleId(index: Int): Int = when (index) {
        0 -> R.id.widget_task_title_0
        1 -> R.id.widget_task_title_1
        2 -> R.id.widget_task_title_2
        3 -> R.id.widget_task_title_3
        4 -> R.id.widget_task_title_4
        else -> R.id.widget_task_title_5
    }
}
