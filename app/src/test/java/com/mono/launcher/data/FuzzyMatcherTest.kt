package com.mono.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {
    @Test fun exactBeatsPrefix() {
        assertTrue(FuzzyMatcher.score("maps", "Maps")!! > FuzzyMatcher.score("maps", "Maps Go")!!)
    }

    @Test fun prefixBeatsWordStart() {
        assertTrue(FuzzyMatcher.score("ca", "Calendar")!! > FuzzyMatcher.score("ca", "Google Calendar")!!)
    }

    @Test fun initialsMatch() {
        assertNotNull(FuzzyMatcher.score("gm", "Google Maps"))
    }

    @Test fun subsequenceMatch() {
        assertNotNull(FuzzyMatcher.score("wtsp", "WhatsApp"))
    }

    @Test fun diacriticsIgnored() {
        assertNotNull(FuzzyMatcher.score("cafe", "Café"))
    }

    @Test fun noMatch() {
        assertNull(FuzzyMatcher.score("xyz", "Camera"))
        assertNull(FuzzyMatcher.score("   ", "Camera"))
    }

    @Test fun rankOrdersByScoreThenTieBreak() {
        // "Clock" and "Crown" score the same, so the tie-break decides.
        val ranked = FuzzyMatcher.rank("c", listOf("Clock", "Crown", "Music"), { it }, { if (it == "Crown") 5 else 0 })
        assertEquals(listOf("Crown", "Clock"), ranked)
    }
}
