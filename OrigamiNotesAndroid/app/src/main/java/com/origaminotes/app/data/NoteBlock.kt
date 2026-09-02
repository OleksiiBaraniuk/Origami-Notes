package com.origaminotes.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A note body is a list of blocks, not one string. Only this shape lets a full-width card sit
 * *between* two runs of text — a single rich-text editor renders text, not arbitrary content.
 *
 * Persisted in [Note.blocks] as JSON. [Note.content] is kept as a plain-text mirror so search,
 * grid previews and export keep working unchanged.
 */
sealed interface NoteBlock {
    val id: String

    /**
     * A run of rich text, stored as the HTML that RichTextState round-trips.
     *
     * Links live *inside* this HTML as `<a>` spans, not as blocks of their own — they are
     * inline pills that sit in the flow of a sentence.
     */
    data class Text(override val id: String = newId(), val html: String = "") : NoteBlock

    /**
     * One entry on a task-list note's day grid.
     *
     * [startMinute] is minutes from midnight, so the whole grid is plain integer arithmetic with
     * no timezone or date involved — a task list is a shape for a day, not a calendar entry.
     */
    data class Task(
        override val id: String = newId(),
        val text: String = "",
        val startMinute: Int = 9 * 60,
        val durationMinutes: Int = 60,
        val done: Boolean = false
    ) : NoteBlock {
        val endMinute: Int get() = (startMinute + durationMinutes).coerceAtMost(DAY_MINUTES)
    }

    companion object {
        const val DAY_MINUTES = 24 * 60
        fun newId(): String = UUID.randomUUID().toString()

        /** Internal note links use this scheme; [linkTo] and [noteIdFrom] are the only encoders. */
        const val NOTE_LINK_SCHEME = "origaminote"

        fun linkTo(noteId: Long): String = "$NOTE_LINK_SCHEME://$noteId"

        /** Returns the note id if [url] is an internal note link, else null. */
        fun noteIdFrom(url: String): Long? = url
            .takeIf { it.startsWith("$NOTE_LINK_SCHEME://") }
            ?.removePrefix("$NOTE_LINK_SCHEME://")
            ?.trimEnd('/')
            ?.toLongOrNull()
    }
}

/** JSON codec for [NoteBlock] lists, plus the plain-text projection used outside the editor. */
object NoteBlocks {

    private const val VERSION = 1
    private const val TYPE_TEXT = "text"
    private const val TYPE_TASK = "task"

    private const val KEY_TIME_TABLE = "timeTable"

    /**
     * Whether a task list lays its tasks on the hour grid.
     *
     * Absent means "written before the option existed", which was always a timed grid — so the
     * default is true and no stored note changes behaviour.
     */
    fun timeTableOf(json: String?): Boolean {
        if (json.isNullOrBlank()) return false
        return try {
            JSONObject(json).optBoolean(KEY_TIME_TABLE, true)
        } catch (e: Exception) {
            true
        }
    }

    fun encode(blocks: List<NoteBlock>, timeTable: Boolean = true): String {
        val array = JSONArray()
        blocks.forEach { block ->
            array.put(
                JSONObject().apply {
                    put("id", block.id)
                    when (block) {
                        is NoteBlock.Text -> {
                            put("t", TYPE_TEXT); put("html", block.html)
                        }
                        is NoteBlock.Task -> {
                            put("t", TYPE_TASK)
                            put("text", block.text)
                            put("start", block.startMinute)
                            put("dur", block.durationMinutes)
                            put("done", block.done)
                        }
                    }
                }
            )
        }
        return JSONObject().apply {
            put("v", VERSION)
            put(KEY_TIME_TABLE, timeTable)
            put("blocks", array)
        }.toString()
    }

    /**
     * Decodes [Note.blocks]. Falls back to a single text block built from [plainFallback] for
     * notes written before the block model, and for anything that fails to parse — a malformed
     * row must not cost the user their text.
     */
    fun decode(json: String?, plainFallback: String): List<NoteBlock> {
        if (json.isNullOrBlank()) return listOf(fromPlainText(plainFallback))
        return try {
            val array = JSONObject(json).getJSONArray("blocks")
            val blocks = buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val id = o.optString("id").ifBlank { NoteBlock.newId() }
                    when (o.optString("t")) {
                        TYPE_TEXT -> add(NoteBlock.Text(id, o.optString("html")))
                        TYPE_TASK -> add(
                            NoteBlock.Task(
                                id = id,
                                text = o.optString("text"),
                                // Clamped on read: a corrupt row must not place a task off-grid.
                                startMinute = o.optInt("start", 0)
                                    .coerceIn(0, NoteBlock.DAY_MINUTES - 1),
                                durationMinutes = o.optInt("dur", 60)
                                    .coerceIn(5, NoteBlock.DAY_MINUTES),
                                done = o.optBoolean("done", false)
                            )
                        )
                    }
                }
            }
            blocks.ifEmpty { listOf(NoteBlock.Text()) }
        } catch (e: Exception) {
            listOf(fromPlainText(plainFallback))
        }
    }

    /** Wraps legacy plain text as one HTML block, preserving line breaks. */
    private fun fromPlainText(text: String): NoteBlock.Text =
        NoteBlock.Text(html = escapeHtml(text).replace("\n", "<br>"))

    /**
     * HTML for read-only rendering (grid previews), so a card shows the note the way the editor
     * does — bold, colours and links included — instead of a flattened string.
     */
    fun previewHtml(json: String?, plainFallback: String): String =
        decode(json, plainFallback)
            .filterIsInstance<NoteBlock.Text>()
            .joinToString("") { it.html }

    /** Tasks in grid order — the day reads top to bottom regardless of insertion order. */
    fun tasksOf(json: String?): List<NoteBlock.Task> =
        decode(json, "")
            .filterIsInstance<NoteBlock.Task>()
            .sortedBy { it.startMinute }

    /**
     * The one task worth showing on a Dashboard card: what is coming up.
     *
     * Prefers the next unfinished task that has not already ended; failing that, any unfinished
     * task (the day's overdue work still matters more than finished work); failing that, the last
     * task, so a fully completed day shows how it ended rather than showing nothing.
     *
     * [tasks] is expected in start order, as [tasksOf] returns it.
     */
    fun nextTask(tasks: List<NoteBlock.Task>, nowMinute: Int): NoteBlock.Task? =
        tasks.firstOrNull { !it.done && it.endMinute > nowMinute }
            ?: tasks.firstOrNull { !it.done }
            ?: tasks.lastOrNull()

    /**
     * Plain-text projection written back to [Note.content] on every save, so search and the
     * Dashboard previews never see markup or JSON.
     */
    fun toPlainText(blocks: List<NoteBlock>): String =
        blocks.joinToString("\n") { block ->
            when (block) {
                is NoteBlock.Text -> stripHtml(block.html)
                // Reads the way the grid does, so search and previews stay useful.
                is NoteBlock.Task ->
                    "${if (block.done) "✓" else "○"} ${formatTime(block.startMinute)}  ${block.text}"
            }
        }.trim()

    /** 24-hour `H:MM`, matching the labels down the side of the day grid. */
    fun formatTime(minuteOfDay: Int): String {
        val m = minuteOfDay.coerceIn(0, DAY_MINUTES)
        return "%d:%02d".format(m / 60, m % 60)
    }

    private const val DAY_MINUTES = NoteBlock.DAY_MINUTES

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /**
     * Fallback HTML → text, used where no live editor state exists.
     *
     * Handles tags and *numeric* entities. It cannot handle the full HTML5 named set the
     * rich-text encoder emits (`&Ncy;`, `&period;`, `&sol;`, …) — for that the editor's own
     * `toText()` is the source of truth, see EditorScreen.currentPlainText.
     */
    private fun stripHtml(html: String): String = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .let(::decodeNumericEntities)
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        // Last: otherwise "&amp;lt;" would decode twice into "<".
        .replace("&amp;", "&")
        .trim()

    /** `&#1053;` and `&#x41D;` both become their character; anything malformed is left as-is. */
    private fun decodeNumericEntities(text: String): String =
        Regex("&#(x?)([0-9a-fA-F]+);").replace(text) { match ->
            val radix = if (match.groupValues[1].isEmpty()) 10 else 16
            val code = match.groupValues[2].toIntOrNull(radix)
            if (code != null && code in 1..0x10FFFF) String(Character.toChars(code))
            else match.value
        }
}
