package com.mono.launcher.data

import java.text.Normalizer

/**
 * Small, allocation-light fuzzy matcher for app names.
 * Higher scores are better; null means no match.
 */
object FuzzyMatcher {
    private val diacritics = "\\p{Mn}+".toRegex()

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

    fun score(query: String, target: String): Int? {
        val q = normalize(query.trim())
        if (q.isEmpty()) return null
        val t = normalize(target)
        if (t == q) return 1000
        if (t.startsWith(q)) return 900 - (t.length - q.length).coerceAtMost(100)

        val words = t.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
        if (words.any { it.startsWith(q) }) return 800 - (t.length - q.length).coerceAtMost(100)

        val initials = words.joinToString("") { it.take(1) }
        if (q.length >= 2 && initials.startsWith(q)) return 700

        val idx = t.indexOf(q)
        if (idx >= 0) return 600 - idx.coerceAtMost(100)

        return subsequenceScore(q, t)
    }

    /** Characters of [q] appear in order in [t]; tighter matches score higher. */
    private fun subsequenceScore(q: String, t: String): Int? {
        var ti = 0
        var gaps = 0
        var last = -1
        for (c in q) {
            if (c == ' ') continue
            val found = t.indexOf(c, ti)
            if (found < 0) return null
            if (last >= 0) gaps += found - last - 1
            last = found
            ti = found + 1
        }
        return (400 - gaps * 10).coerceAtLeast(1)
    }

    fun <T> rank(query: String, items: List<T>, label: (T) -> String, tieBreak: (T) -> Int = { 0 }): List<T> =
        items.mapNotNull { item -> score(query, label(item))?.let { item to it } }
            .sortedWith(compareByDescending<Pair<T, Int>> { it.second }.thenByDescending { tieBreak(it.first) })
            .map { it.first }
}
