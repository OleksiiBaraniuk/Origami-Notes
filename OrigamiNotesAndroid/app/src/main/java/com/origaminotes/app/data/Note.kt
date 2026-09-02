package com.origaminotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    /**
     * Plain-text mirror of the body. Kept authoritative for search, grid previews and export,
     * so those paths never have to parse [blocks].
     */
    val content: String,
    /** Block structure as JSON — see [NoteBlocks]. Null for notes written before the block model. */
    val blocks: String? = null,
    /** Which editor this note opens in. Stored as [NoteType.name]. */
    val type: String = NoteType.DEFAULT.name,
    val folderId: Long? = null,
    val isPinned: Boolean = false,
    val isFullWidth: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    /** Manual position, set once at creation and rewritten only by Assign Mode drags. */
    val sortOrder: Long = System.currentTimeMillis(),
    /** Touched every time the note is opened in the editor; drives [NoteSortMode.LAST_OPEN]. */
    val lastOpenedAt: Long = System.currentTimeMillis()
)
