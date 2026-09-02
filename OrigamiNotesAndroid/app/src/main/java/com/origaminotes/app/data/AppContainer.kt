package com.origaminotes.app.data

import android.content.Context

interface AppContainer {
    val notesRepository: NotesRepository
}

/**
 * [onNotesChanged] is supplied from above rather than imported here, so the data layer stays
 * unaware of widgets.
 */
class AppDataContainer(
    private val context: Context,
    private val onNotesChanged: suspend () -> Unit = {}
) : AppContainer {
    override val notesRepository: NotesRepository by lazy {
        val database = AppDatabase.getDatabase(context)
        NotesRepository(database.noteDao(), database.folderDao(), database, onNotesChanged)
    }
}
