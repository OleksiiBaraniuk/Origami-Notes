package com.origaminotes.app.ui.components

/** A URL found in plain text, with the exact span it occupies. */
data class DetectedUrl(val start: Int, val endExclusive: Int, val url: String)

/**
 * Finds web addresses in free text so they can be turned into links automatically.
 *
 * Deliberately conservative — a false positive silently turns ordinary prose into a link, which
 * is worse than missing an odd address the user can still link by hand.
 */
object UrlDetector {

    private val TOKEN = Regex("""\S+""")

    /**
     * Either an explicit scheme, or a hostname whose last label looks like a TLD.
     * The TLD rule is what keeps "e.g" and "v1.2" out.
     */
    private val URL_SHAPE = Regex(
        """^(?:https?://\S+|(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?:[/?#]\S*)?)$"""
    )

    /** Punctuation that normally ends a sentence rather than belonging to the address. */
    private const val TRAILING = ".,!?;:)\"'»"

    fun detect(text: String): List<DetectedUrl> =
        TOKEN.findAll(text).mapNotNull { match ->
            val raw = match.value
            val trimmed = raw.trimEnd { it in TRAILING }
            if (trimmed.isEmpty()) return@mapNotNull null
            // Emails look a lot like hosts; linking them as http would just be wrong.
            if (trimmed.contains('@')) return@mapNotNull null
            if (!URL_SHAPE.matches(trimmed)) return@mapNotNull null

            DetectedUrl(
                start = match.range.first,
                endExclusive = match.range.first + trimmed.length,
                url = normalizeUrl(trimmed)
            )
        }.toList()
}

/** Adds the scheme a bare host is missing, so the system handler can open it. */
fun normalizeUrl(url: String): String =
    if (url.contains("://")) url else "https://$url"
