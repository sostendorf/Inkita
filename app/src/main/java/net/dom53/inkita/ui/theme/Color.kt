package net.dom53.inkita.ui.theme

import androidx.compose.ui.graphics.Color

// Original Inkita palette, kept so the light theme and any older references still resolve.
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val DarkBg = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE6E6E6)
val DarkAccent = Color(0xFFAD1457)

/*
 * Shelf palette — a dark, poster-forward scheme in the spirit of self-hosted
 * library dashboards: near-black slate page, two lifted surface tiers so cards
 * read above the background, and a violet accent that stays legible on both.
 */

/** Page background. Near-black with a blue cast so cover art stays the brightest thing on screen. */
val ShelfBackground = Color(0xFF0D1117)

/** Cards, bars and sheets sitting on the background. */
val ShelfSurface = Color(0xFF161B22)

/** Menus, dialogs and anything stacked above a card. */
val ShelfSurfaceRaised = Color(0xFF1C2330)

/** Hairlines, dividers and card borders. */
val ShelfOutline = Color(0xFF2B3444)

/*
 * Blue accent, dark text on it. That pairing is the Material 3 dark-theme
 * convention and it is what the contrast maths wants: white on a mid-tone blue
 * only reaches about 3.2:1, while near-black on this brighter blue reaches
 * 6.4:1. Every pairing below clears 4.5:1 against the surface it sits on.
 */
val ShelfPrimary = Color(0xFF60A5FA)
val ShelfOnPrimary = Color(0xFF06203F)
val ShelfPrimaryBright = Color(0xFF8FC2FF)
val ShelfSecondary = Color(0xFF7FB3E3)

/**
 * Warm amber for section headings and the library subtitle. Sits opposite the
 * blue on the colour wheel, so headings separate from accents instead of
 * blending into them.
 */
val ShelfAccent = Color(0xFFDFA45F)
val ShelfOnAccent = Color(0xFF3A2208)

val ShelfOnSurface = Color(0xFFECEFF4)
val ShelfOnSurfaceMuted = Color(0xFF9AA4B2)

val ShelfError = Color(0xFFFF6B63)
val ShelfSuccess = Color(0xFF3FB950)

/** Reader background — flat black, so page edges disappear against the screen. */
val ReaderBackground = Color(0xFF000000)

// Light counterparts, for when the system theme is light and dynamic color is off.
val ShelfLightBackground = Color(0xFFF2F4F8)
val ShelfLightSurface = Color(0xFFFFFFFF)
val ShelfLightSurfaceRaised = Color(0xFFE4E9F0)
val ShelfLightOutline = Color(0xFFD3DAE4)
val ShelfLightPrimary = Color(0xFF1B62C4)
val ShelfLightOnSurface = Color(0xFF12161D)
val ShelfLightOnSurfaceMuted = Color(0xFF5A6472)
