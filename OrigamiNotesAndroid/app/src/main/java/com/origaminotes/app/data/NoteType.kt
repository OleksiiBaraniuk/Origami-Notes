package com.origaminotes.app.data

/**
 * What kind of body a note carries. Five are planned (see CLAUDE.md); two exist so far.
 *
 * Stored by [name], and [fromName] falls back to [TEXT] — an unknown value must still open as
 * something readable rather than crash.
 */
enum class NoteType(val label: String) {
    TEXT("Note"),
    TASK_LIST("Task list");

    companion object {
        val DEFAULT = TEXT

        fun fromName(name: String?): NoteType =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
