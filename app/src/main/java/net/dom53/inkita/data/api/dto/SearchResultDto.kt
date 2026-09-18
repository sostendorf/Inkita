package net.dom53.inkita.data.api.dto

/**
 * Kavita's /api/Search/search response. The server groups hits by kind; only the
 * series group is modelled here, since that is what the app searches for.
 */
data class SearchResultGroupDto(
    val series: List<SeriesSearchHitDto>? = null,
)

data class SeriesSearchHitDto(
    val seriesId: Int,
    val name: String? = null,
    val originalName: String? = null,
    val localizedName: String? = null,
    val libraryId: Int? = null,
    val libraryName: String? = null,
    val format: Int? = null,
)
