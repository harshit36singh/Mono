package com.mono.launcher.data

import android.icu.text.AlphabeticIndex
import android.icu.text.Collator
import java.util.Locale

data class LetterGroup(val letter: String, val apps: List<AppEntry>) {
    val isEmpty: Boolean get() = apps.isEmpty()
}

/**
 * Locale-aware A–Z grouping. Uses ICU's AlphabeticIndex so non-Latin scripts get their
 * native index letters; apps starting with digits or symbols go into "#".
 */
object LetterIndex {
    const val OTHER = "#"

    fun group(apps: List<AppEntry>, locale: Locale = Locale.getDefault()): List<LetterGroup> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        val index = AlphabeticIndex<AppEntry>(locale).apply {
            if (locale.language != Locale.ENGLISH.language) addLabels(Locale.ENGLISH)
        }
        val labels = index.bucketLabels
        val sorted = apps.sortedWith { a, b -> collator.compare(a.displayLabel, b.displayLabel) }

        val buckets = LinkedHashMap<String, MutableList<AppEntry>>()
        buckets[OTHER] = mutableListOf()
        val immutable = index.buildImmutableIndex()
        for (label in labels) {
            if (label.isNotBlank() && label != "…" && label != "...") buckets.getOrPut(label) { mutableListOf() }
        }
        for (app in sorted) {
            val first = app.displayLabel.firstOrNull()
            val label = if (first == null || !first.isLetter()) {
                OTHER
            } else {
                val bucket = immutable.getBucket(immutable.getBucketIndex(app.displayLabel))
                if (bucket.labelType == AlphabeticIndex.Bucket.LabelType.NORMAL) bucket.label else OTHER
            }
            buckets.getOrPut(label) { mutableListOf() }.add(app)
        }
        return buckets.map { (letter, list) -> LetterGroup(letter, list) }
    }
}
