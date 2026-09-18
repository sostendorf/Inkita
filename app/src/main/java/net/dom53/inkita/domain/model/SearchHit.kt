package net.dom53.inkita.domain.model

/** A series matched by a search, with just enough to draw a result card. */
data class SearchHit(
    val seriesId: Int,
    val name: String,
    val libraryName: String?,
    val format: Int?,
)
