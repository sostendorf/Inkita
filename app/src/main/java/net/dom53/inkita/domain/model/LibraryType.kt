package net.dom53.inkita.domain.model

/**
 * The kinds of library a Kavita server can hold, grouped the way a reader thinks
 * about them rather than one-to-one with Kavita's codes.
 *
 * Kavita's own type codes are: 0 Manga, 1 Comic, 2 Book, 3 Images,
 * 4 LightNovel, 5 ComicVine. Comic and ComicVine are both comics, and a light
 * novel is a book, so those fold together here.
 */
enum class LibraryType(
    val label: String,
    val kavitaCodes: Set<Int>,
) {
    Comics("Comics", setOf(1, 5)),
    Manga("Manga", setOf(0)),
    Books("Books", setOf(2, 4)),
    ;

    fun matches(code: Int?): Boolean = code != null && code in kavitaCodes

    companion object {
        fun forCode(code: Int?): LibraryType? = entries.firstOrNull { it.matches(code) }
    }
}
