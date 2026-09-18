package net.dom53.inkita.domain.model

data class Library(
    val id: Int,
    val name: String,
    /** Kavita's raw library type code; see [LibraryType]. */
    val type: Int? = null,
)
