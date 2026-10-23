package app.smriti.domain

import kotlinx.serialization.Serializable

/**
 * One page of the Memory Book. Sample pages ship with the app and are always labelled as samples;
 * personal pages are added by a caregiver and live only in app-private storage on this device.
 */
@Serializable
data class MemoryPage(
    val id: String,
    val kind: Kind,
    /** For SAMPLE pages: stable key used to look up the bundled image and localised caption. */
    val sampleKey: String? = null,
    /** For PERSONAL pages: caregiver-written caption, shown exactly as typed. */
    val caption: String? = null,
    /** For PERSONAL pages: file name inside the app's private memory-photo folder, if any. */
    val photoFile: String? = null,
) {
    @Serializable
    enum class Kind { SAMPLE, PERSONAL }

    val isSample: Boolean get() = kind == Kind.SAMPLE
}

@Serializable
data class MemoryBookState(
    val pages: List<MemoryPage> = DEFAULT_SAMPLE_PAGES,
    val currentIndex: Int = 0,
) {
    val currentPage: MemoryPage? get() = pages.getOrNull(currentIndex)
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex < pages.lastIndex

    fun next(): MemoryBookState = if (hasNext) copy(currentIndex = currentIndex + 1) else this
    fun previous(): MemoryBookState = if (hasPrevious) copy(currentIndex = currentIndex - 1) else this

    fun addPersonal(id: String, caption: String, photoFile: String?): MemoryBookState {
        val page = MemoryPage(id = id, kind = MemoryPage.Kind.PERSONAL, caption = caption.trim(), photoFile = photoFile)
        // Personal pages come before samples so the person's own content is seen first.
        val firstSample = pages.indexOfFirst { it.isSample }.let { if (it < 0) pages.size else it }
        val newPages = pages.toMutableList().apply { add(firstSample, page) }
        return copy(pages = newPages, currentIndex = firstSample)
    }

    fun editCaption(id: String, caption: String): MemoryBookState =
        copy(pages = pages.map { if (it.id == id && !it.isSample) it.copy(caption = caption.trim()) else it })

    fun remove(id: String): MemoryBookState {
        val newPages = pages.filterNot { it.id == id }
        return copy(pages = newPages, currentIndex = currentIndex.coerceIn(0, (newPages.size - 1).coerceAtLeast(0)))
    }

    fun removeSamples(): MemoryBookState {
        val newPages = pages.filterNot { it.isSample }
        return copy(pages = newPages, currentIndex = 0)
    }

    fun restoreSamples(): MemoryBookState {
        val missing = DEFAULT_SAMPLE_PAGES.filter { s -> pages.none { it.id == s.id } }
        return copy(pages = pages + missing)
    }

    /** Keeps the index valid after loading from disk. */
    fun normalized(): MemoryBookState =
        copy(currentIndex = currentIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))

    companion object {
        val DEFAULT_SAMPLE_PAGES = listOf(
            MemoryPage(id = "sample_kolam", kind = MemoryPage.Kind.SAMPLE, sampleKey = "kolam"),
            MemoryPage(id = "sample_coffee", kind = MemoryPage.Kind.SAMPLE, sampleKey = "coffee"),
            MemoryPage(id = "sample_jasmine", kind = MemoryPage.Kind.SAMPLE, sampleKey = "jasmine"),
        )
    }
}
