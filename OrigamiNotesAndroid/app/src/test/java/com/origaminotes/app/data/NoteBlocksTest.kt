package com.origaminotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteBlocksTest {

    private fun plainOf(html: String) =
        NoteBlocks.toPlainText(listOf(NoteBlock.Text(html = html)))

    @Test fun `strips paragraph wrappers`() {
        assertEquals("Hello", plainOf("<p>Hello</p>"))
    }

    @Test fun `strips inline formatting`() {
        assertEquals("bold and italic", plainOf("<p><b>bold</b> and <i>italic</i></p>"))
    }

    @Test fun `strips span styles`() {
        assertEquals(
            "colored",
            plainOf("<p><span style=\"color: #13ECA4;\">colored</span></p>")
        )
    }

    @Test fun `keeps link text but drops markup`() {
        assertEquals(
            "see Recipes here",
            plainOf("<p>see <a href=\"origaminote://4\">Recipes</a> here</p>")
        )
    }

    @Test fun `multiple paragraphs become newlines`() {
        assertEquals("one\ntwo", plainOf("<p>one</p><p>two</p>"))
    }

    @Test fun `unescapes entities`() {
        assertEquals("a < b & c", plainOf("<p>a &lt; b &amp; c</p>"))
    }

    @Test fun `nbsp becomes a space`() {
        assertEquals("a b", plainOf("<p>a&nbsp;b</p>"))
    }

    // ── Task blocks ─────────────────────────────────────────────────────────

    @Test fun `task survives an encode decode round trip`() {
        val task = NoteBlock.Task(
            text = "Standup", startMinute = 9 * 60 + 30, durationMinutes = 45, done = true
        )
        val decoded = NoteBlocks.decode(NoteBlocks.encode(listOf(task)), "")
        assertEquals(listOf(task), decoded)
    }

    @Test fun `mixed text and task blocks round trip in order`() {
        val blocks = listOf(
            NoteBlock.Text(html = "<p>hi</p>"),
            NoteBlock.Task(text = "Call", startMinute = 60, durationMinutes = 30)
        )
        assertEquals(blocks, NoteBlocks.decode(NoteBlocks.encode(blocks), ""))
    }

    @Test fun `out of range start is clamped on read`() {
        val json = """{"v":1,"blocks":[{"id":"a","t":"task","text":"x","start":99999,"dur":30}]}"""
        val task = NoteBlocks.decode(json, "").single() as NoteBlock.Task
        assertEquals(NoteBlock.DAY_MINUTES - 1, task.startMinute)
    }

    @Test fun `task end never runs past midnight`() {
        val task = NoteBlock.Task(startMinute = 23 * 60, durationMinutes = 600)
        assertEquals(NoteBlock.DAY_MINUTES, task.endMinute)
    }

    @Test fun `tasks are returned in time order`() {
        val late = NoteBlock.Task(text = "late", startMinute = 18 * 60)
        val early = NoteBlock.Task(text = "early", startMinute = 7 * 60)
        val json = NoteBlocks.encode(listOf(late, early))
        assertEquals(listOf("early", "late"), NoteBlocks.tasksOf(json).map { it.text })
    }

    @Test fun `task plain text reads like the grid`() {
        val task = NoteBlock.Task(text = "Gym", startMinute = 7 * 60 + 5, done = false)
        assertEquals("○ 7:05  Gym", NoteBlocks.toPlainText(listOf(task)))
    }

    // ── Nearest task ────────────────────────────────────────────────────────

    private fun task(text: String, hour: Int, done: Boolean = false) =
        NoteBlock.Task(text = text, startMinute = hour * 60, durationMinutes = 60, done = done)

    @Test fun `picks the next task still ahead`() {
        val tasks = listOf(task("morning", 8), task("noon", 12), task("evening", 18))
        assertEquals("noon", NoteBlocks.nextTask(tasks, 11 * 60)?.text)
    }

    @Test fun `a task in progress is still the next one`() {
        val tasks = listOf(task("standup", 9), task("lunch", 13))
        // 09:30 — inside the standup, which has not ended
        assertEquals("standup", NoteBlocks.nextTask(tasks, 9 * 60 + 30)?.text)
    }

    @Test fun `skips completed tasks`() {
        val tasks = listOf(task("done thing", 8, done = true), task("real one", 9))
        assertEquals("real one", NoteBlocks.nextTask(tasks, 7 * 60)?.text)
    }

    @Test fun `falls back to overdue work when nothing is ahead`() {
        val tasks = listOf(task("missed", 8), task("finished", 9, done = true))
        assertEquals("missed", NoteBlocks.nextTask(tasks, 23 * 60)?.text)
    }

    @Test fun `all done shows the last task`() {
        val tasks = listOf(task("a", 8, done = true), task("b", 9, done = true))
        assertEquals("b", NoteBlocks.nextTask(tasks, 23 * 60)?.text)
    }

    @Test fun `empty list has no next task`() {
        assertEquals(null, NoteBlocks.nextTask(emptyList(), 12 * 60))
    }

    @Test fun `done task is marked in plain text`() {
        val task = NoteBlock.Task(text = "Gym", startMinute = 7 * 60, done = true)
        assertEquals("✓ 7:00  Gym", NoteBlocks.toPlainText(listOf(task)))
    }
}
