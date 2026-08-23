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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeWidgetEntryPoint {
    fun database(): PixelPalDatabase
}

class HomeWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HomeWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, HomeWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext, HomeWidgetEntryPoint::class.java
                )
                val db = entryPoint.database()
                val companion = db.companionDao().getPrimaryDirect()
                val companionId = companion?.id

                val views = RemoteViews(context.packageName, R.layout.widget_pixelpal_home)

                views.setTextViewText(R.id.widget_home_name, companion?.name ?: "Pixel")
                val bond = companionId?.let { db.bondDao().getBondDirect(it) }
                val level = bond?.level ?: 0
                val streak = bond?.streakDays ?: 0
                views.setTextViewText(R.id.widget_home_status, "Bond Lv $level • ${streak}d streak")

                // Tasks summary
                var pendingCount = 0
                var nextTask: String? = null
                if (companionId != null) {
                    val cursor = db.openHelper.readableDatabase.query(
                        "SELECT title FROM tasks WHERE companionId = $companionId AND isDone = 0 ORDER BY createdAt DESC LIMIT 1"
                    )
                    val countCursor = db.openHelper.readableDatabase.query(
                        "SELECT COUNT(*) FROM tasks WHERE companionId = $companionId AND isDone = 0"
                    )
                    if (countCursor.moveToFirst()) pendingCount = countCursor.getInt(0)
                    countCursor.close()
                    if (cursor.moveToFirst()) nextTask = cursor.getString(0)
                    cursor.close()
                }
                views.setTextViewText(R.id.widget_home_tasks, if (pendingCount == 0) "All caught up!" else "☐ $pendingCount tasks remaining")

                // Reminder
                var reminderText: String? = null
                if (companionId != null) {
                    val cursor = db.openHelper.readableDatabase.query(
                        "SELECT title, triggerTime FROM reminders WHERE companionId = $companionId AND status = 'PENDING' ORDER BY triggerTime ASC LIMIT 1"
                    )
                    if (cursor.moveToFirst()) {
                        reminderText = cursor.getString(0)
                    }
                    cursor.close()
                }
                if (reminderText != null) {
                    views.setViewVisibility(R.id.widget_home_reminder, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_home_reminder, "🔔 $reminderText")
                } else {
                    views.setViewVisibility(R.id.widget_home_reminder, android.view.View.GONE)
                }

                // Agent
                var agentStatus: String? = null
                if (companionId != null) {
                    val cursor = db.openHelper.readableDatabase.query(
                        "SELECT currentStatus, connectionStatus FROM agent_connection WHERE companionId = $companionId LIMIT 1"
                    )
                    if (cursor.moveToFirst()) {
                        val status = cursor.getString(0)
                        val conn = cursor.getString(1)
                        agentStatus = when {
                            conn == "ERROR" -> "● Agent needs attention"
                            status == "WORKING" -> "● Working"
                            status == "CONNECTED" || status == "ONLINE" -> "● Connected"
                            else -> null
                        }
                    }
                    cursor.close()
                }
                if (agentStatus != null) {
                    views.setViewVisibility(R.id.widget_home_agent, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_home_agent, agentStatus)
                } else {
                    views.setViewVisibility(R.id.widget_home_agent, android.view.View.GONE)
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_home_open, pending)
                // Make entire widget clickable
                views.setOnClickPendingIntent(R.id.widget_home_name, pending)

                manager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) { }
        }
    }
}
