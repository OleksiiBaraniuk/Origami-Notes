package com.origaminotes.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.origaminotes.app.data.NoteBlock
import com.origaminotes.app.data.NoteBlocks
import com.origaminotes.app.data.NoteType
import java.time.LocalTime

/** A task plus the note it lives in — the widget flattens every task list into one day. */
private data class WidgetTask(
    val noteId: Long,
    val noteTitle: String,
    val task: NoteBlock.Task
)

private const val MAX_TASKS = 12

private val NoteIdKey = ActionParameters.Key<Long>("noteId")
private val TaskIdKey = ActionParameters.Key<String>("taskId")

/**
 * Today's tasks across every task-list note, earliest first, already-finished ones dropped once
 * their time has passed.
 *
 * The checkbox writes straight to the database, so a task can be ticked off without opening the
 * app — that is the whole reason to have this on a home screen.
 */
class TasksWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }

        val tasks = repositoryOf(context).getAllNotesOnce()
            .filter { NoteType.fromName(it.type) == NoteType.TASK_LIST }
            .flatMap { note ->
                NoteBlocks.tasksOf(note.blocks).map { WidgetTask(note.id, note.title, it) }
            }
            // Keep anything unfinished plus whatever is still ahead: a completed task that has
            // not happened yet is still useful context, a completed past one is just noise.
            .filter { !it.task.done || it.task.endMinute > nowMinute }
            .sortedBy { it.task.startMinute }
            .take(MAX_TASKS)

        provideContent {
            GlanceTheme {
                WidgetFrame(title = "Tasks", count = tasks.size) {
                    if (tasks.isEmpty()) {
                        EmptyHint("Nothing left today")
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(tasks, itemId = { it.task.id.hashCode().toLong() }) { entry ->
                                TaskRow(entry)
                                Spacer(GlanceModifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(entry: WidgetTask) {
    val context = LocalContext.current
    val task = entry.task

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glance has no Checkbox that survives every launcher, so the marker is a tappable
        // glyph — same meaning, and it works the same everywhere.
        Text(
            text = if (task.done) "☑" else "☐",
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 18.sp),
            modifier = GlanceModifier.clickable(
                actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(NoteIdKey to entry.noteId, TaskIdKey to task.id)
                )
            )
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(openAppIntent(context, entry.noteId)))
        ) {
            Text(
                text = task.text.ifBlank { "(Untitled task)" },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1
            )
            Text(
                text = "${NoteBlocks.formatTime(task.startMinute)}–${NoteBlocks.formatTime(task.endMinute)}" +
                    entry.noteTitle.takeIf { it.isNotBlank() }?.let { "  ·  $it" }.orEmpty(),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

/** Flips a task's done flag from the home screen and redraws the widget. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val taskId = parameters[TaskIdKey] ?: return

        val repository = repositoryOf(context)
        val note = repository.getNoteById(noteId) ?: return
        val blocks = NoteBlocks.decode(note.blocks, note.content)
        // Nothing to write if the note changed underneath us and the task is gone.
        if (blocks.none { it is NoteBlock.Task && it.id == taskId }) return

        val updated = blocks.map { block ->
            if (block is NoteBlock.Task && block.id == taskId) block.copy(done = !block.done)
            else block
        }
        repository.updateNote(
            note.copy(
                blocks = NoteBlocks.encode(updated),
                content = NoteBlocks.toPlainText(updated),
                modifiedAt = System.currentTimeMillis()
            )
        )
        refreshWidgets(context)
    }
}

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}
