package com.origaminotes.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.origaminotes.app.data.Note
import com.origaminotes.app.data.NoteSortMode
import com.origaminotes.app.data.NoteType

/** How many notes fit a widget before scrolling stops being the point. */
private const val MAX_NOTES = 12

/**
 * Home screen list of recent notes. Tapping one opens it; tapping the header opens the app.
 *
 * Shows text notes only — task lists have their own widget, and mixing them would make neither
 * readable at widget size.
 */
class NotesWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = repositoryOf(context).getAllNotesOnce()
            .filter { NoteType.fromName(it.type) == NoteType.TEXT }
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { NoteSortMode.LAST_CHANGED.keyOf(it) }
            )
            .take(MAX_NOTES)

        provideContent {
            GlanceTheme {
                WidgetFrame(title = "Notes", count = notes.size) {
                    if (notes.isEmpty()) {
                        EmptyHint("No notes yet")
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(notes, itemId = { it.id }) { note ->
                                NoteRow(note)
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
private fun NoteRow(note: Note) {
    val context = androidx.glance.LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionStartActivity(openAppIntent(context, note.id)))
    ) {
        Text(
            text = note.title.ifBlank { "(Untitled)" },
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        val body = note.content.replace('\n', ' ').trim()
        if (body.isNotEmpty()) {
            Text(
                text = body,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 2
            )
        }
    }
}

/** Shared chrome so both widgets read as the same app. */
@Composable
internal fun WidgetFrame(
    title: String,
    count: Int,
    content: @Composable () -> Unit
) {
    val context = androidx.glance.LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .padding(10.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openAppIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = count.toString(),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        content()
    }
}

@Composable
internal fun EmptyHint(message: String) {
    Text(
        text = message,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
    )
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
