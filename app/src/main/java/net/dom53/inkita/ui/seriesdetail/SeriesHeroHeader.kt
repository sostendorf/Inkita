package net.dom53.inkita.ui.seriesdetail

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Detail-screen furniture: a cover-tinted backdrop, a compact title bar, the
 * cover-plus-metadata hero row, and the rails underneath.
 *
 * The layout leans on the cover doing the work — it is the backdrop, the tint
 * for every surface on the screen, and the largest element in the hero row.
 */

/** Corner radius shared by the cover, the tiles and the rail thumbnails. */
private val TileShape = RoundedCornerShape(10.dp)
private val CoverShape = RoundedCornerShape(12.dp)

/**
 * Parses Kavita's cover-derived colour (`#RRGGBB`), which it extracts per series
 * and returns on the series DTO. Null or unparseable falls back to the theme.
 */
fun parseSeriesColor(hex: String?): Color? {
    val cleaned = hex?.trim()?.removePrefix("#") ?: return null
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return runCatching {
        val value = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000 or value)
        } else {
            Color(value)
        }
    }.getOrNull()
}

/**
 * Full-bleed blurred cover behind the whole screen, washed with the cover's own
 * dominant colour and darkened enough to keep text legible over any artwork.
 *
 * `Modifier.blur` is a no-op below API 31, so on older devices the colour wash
 * and scrim carry the effect on their own and the cover is scaled up instead.
 */
@Composable
fun SeriesBackdrop(
    coverUrl: String?,
    accent: Color?,
    modifier: Modifier = Modifier,
) {
    val tint = accent ?: MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(48.dp)
                        } else {
                            Modifier
                        },
                    ),
        )
        // Colour wash, then a top-to-bottom scrim so the foot of the page settles
        // to near-black and the rails below read cleanly.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(tint.copy(alpha = 0.45f)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.35f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
        )
    }
}

/**
 * The bar that stays put while the page scrolls: thumbnail, title, the library
 * it came from, and the at-a-glance numbers on the right.
 */
@Composable
fun SeriesStickyBar(
    title: String,
    subtitle: String?,
    coverUrl: String?,
    ratingText: String?,
    readTimeText: String?,
    chapterCountText: String?,
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onTitleClick() },
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (!ratingText.isNullOrBlank()) {
                Text(
                    text = ratingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!readTimeText.isNullOrBlank()) {
                    Text(
                        text = readTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                if (!chapterCountText.isNullOrBlank()) {
                    Text(
                        text = chapterCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A field tile: small muted label with its value underneath.
 *
 * Left-aligned and sized to its content rather than a fixed height, because the
 * values here are names and lists (writers, genres, tags) that need the room —
 * a centred fixed-height box wasted space and truncated anything interesting.
 */
@Composable
fun MetaTile(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 2,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "--",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Genre / tag pill shown above the hero row. */
@Composable
fun SeriesChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.9f),
        maxLines = 1,
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Cover and primary actions on the left, metadata tiles stacked on the right.
 * The caller supplies the tiles so the screen decides what's worth showing.
 */
@Composable
fun SeriesHeroRow(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    onCoverClick: (() -> Unit)? = null,
    primaryAction: @Composable () -> Unit = {},
    iconActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    tiles: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.46f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(CoverShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .then(
                            if (onCoverClick != null) {
                                Modifier.clickable { onCoverClick() }
                            } else {
                                Modifier
                            },
                        ),
            )
            primaryAction()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                iconActions()
            }
        }
        Column(
            modifier = Modifier.weight(0.54f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tiles()
        }
    }
}

/**
 * Square translucent action button sitting under the cover. Kept icon-only so
 * three fit across the cover's width without crowding.
 */
@Composable
fun HeroIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(44.dp)
                .clip(TileShape)
                .background(Color.White.copy(alpha = if (enabled) 0.12f else 0.05f))
                .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 0.9f else 0.35f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Uppercase accent heading, as used above each rail. */
@Composable
fun SeriesSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

/** One tile in a related-series rail: cover with a category badge in the corner. */
@Composable
fun RelatedTile(
    coverUrl: String?,
    badge: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(96.dp)
                .aspectRatio(0.78f)
                .clip(TileShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable { onClick() },
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (!badge.isNullOrBlank()) {
            Text(
                text = badge.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Horizontal rail of related tiles under an accent heading. */
@Composable
fun RelatedRail(
    title: String,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SeriesSectionHeader(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

/** Spacer used between hero and the first rail. */
@Composable
fun SeriesSectionGap() {
    Box(modifier = Modifier.height(18.dp))
}
