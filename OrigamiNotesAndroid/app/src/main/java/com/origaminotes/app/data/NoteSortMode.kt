package com.origaminotes.app.data

/**
 * Order the Dashboard lists notes in. Every mode is newest-first.
 *
 * The Branches tree deliberately ignores this and stays on [Note.sortOrder] — that is the
 * manual arrangement made in Assign Mode, and a sort mode would hide it.
 */
enum class NoteSortMode(val label: String) {
    LAST_CREATED("Last Created"),
    LAST_CHANGED("Last Changed"),
    LAST_OPEN("Last Open");

    /** Key this mode ranks by; larger sorts first. */
    fun keyOf(note: Note): Long = when (this) {
        LAST_CREATED -> note.createdAt
        LAST_CHANGED -> note.modifiedAt
        LAST_OPEN    -> note.lastOpenedAt
    }

    companion object {
        val DEFAULT = LAST_CREATED

        /** Tolerates unknown/absent stored values so a renamed enum can't crash startup. */
        fun fromName(name: String?): NoteSortMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
