package com.origaminotes.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlDetectorTest {

    private fun urls(text: String) = UrlDetector.detect(text).map { it.url }

    // ── Should link ─────────────────────────────────────────────────────────

    @Test fun `explicit scheme`() {
        assertEquals(listOf("https://example.com/page"), urls("see https://example.com/page ok"))
    }

    @Test fun `bare host gets a scheme`() {
        assertEquals(listOf("https://example.com"), urls("go to example.com now"))
    }

    @Test fun `www host`() {
        assertEquals(listOf("https://www.example.co.uk"), urls("www.example.co.uk"))
    }

    @Test fun `host with path and query`() {
        assertEquals(listOf("https://site.org/a/b?x=1"), urls("site.org/a/b?x=1"))
    }

    @Test fun `several in one line`() {
        assertEquals(listOf("https://a.com", "https://b.dev"), urls("a.com and b.dev"))
    }

    // ── Should NOT link ─────────────────────────────────────────────────────

    @Test fun `sentence abbreviation is not a url`() {
        assertTrue(urls("e.g. this is fine").isEmpty())
    }

    @Test fun `version number is not a url`() {
        assertTrue(urls("upgrade to v1.2 today").isEmpty())
    }

    @Test fun `email is not linked`() {
        assertTrue(urls("mail me at bob@example.com").isEmpty())
    }

    @Test fun `plain prose is left alone`() {
        assertTrue(urls("buy milk, eggs and bread").isEmpty())
    }

    @Test fun `decimal number is not a url`() {
        assertTrue(urls("that costs 12.50 total").isEmpty())
    }

    // ── Boundaries ──────────────────────────────────────────────────────────

    @Test fun `trailing sentence punctuation is excluded from the span`() {
        val found = UrlDetector.detect("visit example.com.")
        assertEquals(1, found.size)
        assertEquals("https://example.com", found[0].url)
        // the final period must stay outside the link
        assertEquals("visit ".length, found[0].start)
        assertEquals("visit example.com".length, found[0].endExclusive)
    }

    @Test fun `closing bracket is excluded`() {
        assertEquals(listOf("https://example.com"), urls("(see example.com)"))
    }

    @Test fun `span covers exactly the url`() {
        val found = UrlDetector.detect("a https://x.io/p b").single()
        assertEquals(2, found.start)
        assertEquals("a https://x.io/p".length, found.endExclusive)
    }
}
