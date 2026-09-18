package net.dom53.inkita.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.ui.graphics.vector.ImageVector
import net.dom53.inkita.ui.library.LibraryV2Section

/**
 * Destinations reachable from the app shell.
 *
 * The four content tabs are all the same library destination viewed through a
 * different [section]; only Settings is a separate route. Anything not in
 * [items] is still a real destination, just reached from the drawer rather than
 * the bottom bar.
 */
sealed class MainScreen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val section: LibraryV2Section? = null,
) {
    /** Landing screen: on-deck, recently added and recently updated rails. */
    object Home : MainScreen(LIBRARY_ROUTE, "Home", Icons.Filled.Home, LibraryV2Section.Home)

    /** Every series on the server — Kavita's "All Series". */
    object Library : MainScreen(LIBRARY_ROUTE, "Library", Icons.Filled.LibraryBooks, LibraryV2Section.LibrarySeries)

    /** Curated collections from Kavita. */
    object Collections : MainScreen(LIBRARY_ROUTE, "Collections", Icons.Filled.CollectionsBookmark, LibraryV2Section.Collections)

    /** Kavita's Want To Read list — the to-read pile. */
    object Reading : MainScreen(LIBRARY_ROUTE, "Reading", Icons.Filled.AutoStories, LibraryV2Section.WantToRead)

    object Settings : MainScreen("settings", "Settings", Icons.Filled.Settings)

    // Reachable from the drawer and deep links, but no longer in the bottom bar.
    object LibraryV2 : MainScreen(LIBRARY_ROUTE, "Library", Icons.Filled.LibraryBooks)

    object Updates : MainScreen("updates", "Updates", Icons.Filled.Update)

    object History : MainScreen("history", "History", Icons.Filled.History)

    object Browse : MainScreen("browse", "Browse", Icons.Filled.Language)

    object Downloads : MainScreen("downloads", "Downloads", Icons.Filled.Download)

    companion object {
        const val LIBRARY_ROUTE = "library_v2"

        /** What the bottom bar shows, in order. */
        val items = listOf(Home, Library, Collections, Reading, Settings)

        /** Every top-level destination; the bottom bar stays visible on all of them. */
        val allDestinations = listOf(LibraryV2, Updates, History, Browse, Downloads, Settings)
    }
}
