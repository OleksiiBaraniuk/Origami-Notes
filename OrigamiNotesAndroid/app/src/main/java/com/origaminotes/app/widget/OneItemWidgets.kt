package com.origaminotes.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.origaminotes.app.data.NoteBlocks

/** Id of whatever this widget was pointed at. Absent until the user picks something. */
private val TargetIdKey = longPreferencesKey("target_id")

/** What a given widget asks the app to pick. Carried in the intent that opens the picker. */
enum class WidgetPickKind { NOTE, TASK_LIST, FOLDER }

const val EXTRA_PICK_KIND = "com.origaminotes.app.extra.PICK_KIND"
const val EXTRA_WIDGET_ID = "com.origaminotes.app.extra.WIDGET_ID"

/**
 * Stores the chosen target and redraws. Takes a raw `appWidgetId` because that is all the
 * activity has to go on after the picker closes.
 */
suspend fun setWidgetTarget(context: Context, appWidgetId: Int, targetId: Long) {
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    updateAppWidgetState(context, glanceId) { prefs -> prefs[TargetIdKey] = targetId }
    refreshWidgets(context)
}

/** Target id stored for this widget, or null when nothing has been picked yet. */
private suspend fun targetIdOf(context: Context, id: GlanceId): Long? =
    getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TargetIdKey]
        ?.takeIf { it > 0L }

/**
 * Frame shared by the three single-item widgets: a header that re-opens the picker, and either
 * the content or a prompt to choose.
 *
 * Picking happens in the app rather than in a widget configuration activity, so the dialogs built
 * for folder assignment and note linking are reused instead of written twice.
 */
@Composable
private fun OneItemFrame(
    heading: String,
    appWidgetId: Int,
    kind: WidgetPickKind,
    hasTarget: Boolean,
    emptyHint: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val pick = actionStartActivity(pickIntent(context, kind, appWidgetId))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .padding(10.dp)
    ) {
        Text(
            text = heading,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            // Tapping the heading re-opens the picker, which is the only way to retarget a
            // widget once placed.
            modifier = GlanceModifier.fillMaxWidth().clickable(pick)
        )
        Spacer(GlanceModifier.height(8.dp))

        if (hasTarget) {
            content()
        } else {
            // Covers both "never picked" and "the target has since been deleted".
            Text(
                text = emptyHint,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                modifier = GlanceModifier.fillMaxSize().clickable(pick)
            )
        }
    }
}

private fun pickIntent(context: Context, kind: WidgetPickKind, appWidgetId: Int) =
    openAppIntent(context).apply {
        action = "pick_${kind.name}_$appWidgetId"
        putExtra(EXTRA_PICK_KIND, kind.name)
        putExtra(EXTRA_WIDGET_ID, appWidgetId)
    }

// ── One Note ─────────────────────────────────────────────────────────────────

class OneNoteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val note = targetIdOf(context, id)?.let { repositoryOf(context).getNoteById(it) }

        provideContent {
            GlanceTheme {
                OneItemFrame(
                    heading = note?.title?.ifBlank { "(Untitled)" } ?: "One Note",
                    appWidgetId = appWidgetId,
                    kind = WidgetPickKind.NOTE,
                    hasTarget = note != null,
                    emptyHint = "Tap to choose a note"
                ) {
                    val context = LocalContext.current
                    Text(
                        text = note!!.content.ifBlank { "(Empty)" },
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionStartActivity(openAppIntent(context, note.id)))
                    )
                }
            }
        }
    }
}

class OneNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneNoteWidget()
}

// ── One Task ─────────────────────────────────────────────────────────────────

class OneTaskWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val note = targetIdOf(context, id)?.let { repositoryOf(context).getNoteById(it) }
        val showTime = note?.let { NoteBlocks.timeTableOf(it.blocks) } ?: false
        val tasks = note?.let { NoteBlocks.tasksOf(it.blocks) }.orEmpty()

        provideContent {
            GlanceTheme {
                OneItemFrame(
                    heading = note?.title?.ifBlank { "(Untitled)" } ?: "One Task",
                    appWidgetId = appWidgetId,
                    kind = WidgetPickKind.TASK_LIST,
                    hasTarget = note != null,
                    emptyHint = "Tap to choose a task list"
                ) {
                    val context = LocalContext.current
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks yet",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                Text(
                                    text = buildString {
                                        append(if (task.done) "☑ " else "☐ ")
                                        if (showTime) append("${NoteBlocks.formatTime(task.startMinute)}  ")
                                        append(task.text.ifBlank { "(Untitled task)" })
                                    },
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable(
                                            actionStartActivity(openAppIntent(context, note!!.id))
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class OneTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneTaskWidget()
}

// ── One Folder ───────────────────────────────────────────────────────────────

class OneFolderWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repository = repositoryOf(context)
        val folderId = targetIdOf(context, id)
        val folder = folderId?.let { repository.getFolderById(it) }
        val notes = if (folder == null) emptyList() else {
            repository.getAllNotesOnce()
                .filter { it.folderId == folder.id }
                .sortedByDescending { it.modifiedAt }
                .take(12)
        }

        provideContent {
            GlanceTheme {
                OneItemFrame(
                    heading = folder?.name ?: "One Folder",
                    appWidgetId = appWidgetId,
                    kind = WidgetPickKind.FOLDER,
                    hasTarget = folder != null,
                    emptyHint = "Tap to choose a folder"
                ) {
                    val context = LocalContext.current
                    if (notes.isEmpty()) {
                        Text(
                            text = "Folder is empty",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(notes, itemId = { it.id }) { note ->
                                Column(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(GlanceTheme.colors.surfaceVariant)
                                        .cornerRadius(10.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .clickable(
                                            actionStartActivity(openAppIntent(context, note.id))
                                        )
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "(Untitled)" },
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                    val snippet = note.content.replace('\n', ' ').trim()
                                    if (snippet.isNotEmpty()) {
                                        Text(
                                            text = snippet,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onSurfaceVariant,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(GlanceModifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class OneFolderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneFolderWidget()
}
