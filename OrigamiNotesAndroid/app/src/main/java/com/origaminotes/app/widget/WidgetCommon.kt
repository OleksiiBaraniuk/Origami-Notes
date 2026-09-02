package com.origaminotes.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.origaminotes.app.MainActivity
import com.origaminotes.app.OrigamiNotesApplication
import com.origaminotes.app.data.NotesRepository

/** Extra carried by a widget tap so the app opens straight on the note that was tapped. */
const val EXTRA_NOTE_ID = "com.origaminotes.app.extra.NOTE_ID"

/**
 * Widgets run outside the Compose UI, so they cannot see the ViewModel. They read Room directly
 * through the same repository the app uses.
 */
fun repositoryOf(context: Context): NotesRepository =
    (context.applicationContext as OrigamiNotesApplication).container.notesRepository

/** Intent that opens [noteId], or the note list when null. */
fun openAppIntent(context: Context, noteId: Long? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        // A distinct action per target: without it the system reuses one PendingIntent for every
        // row and every note would open whichever was created first.
        action = if (noteId != null) "open_note_$noteId" else "open_app"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (noteId != null) putExtra(EXTRA_NOTE_ID, noteId)
    }

/**
 * Redraws both widgets. Called after any note write, because a widget cannot observe a Flow —
 * it is only composed when the system asks it to update.
 */
suspend fun refreshWidgets(context: Context) {
    NotesWidget().updateAll(context)
    TasksWidget().updateAll(context)
    OneNoteWidget().updateAll(context)
    OneTaskWidget().updateAll(context)
    OneFolderWidget().updateAll(context)
}
