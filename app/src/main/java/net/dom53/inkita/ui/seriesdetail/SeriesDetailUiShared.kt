package net.dom53.inkita.ui.seriesdetail

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.dom53.inkita.R
import net.dom53.inkita.core.storage.AppConfig
import net.dom53.inkita.data.mapper.TocItem
import net.dom53.inkita.ui.common.DownloadState
import net.dom53.inkita.ui.common.chapterCoverUrl
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

internal enum class SeriesDetailTab {
    Books,
    Chapters,
    Specials,
    Related,
    Recommendations,
    Reviews,
    ;

    val label: String
        get() = labelFor(isBook = true)

    /**
     * Comics and manga are shelved as volumes of issues; only an EPUB is a book
     * of chapters. Kavita words it the same way, so the tabs follow the format.
     */
    fun labelFor(isBook: Boolean): String =
        when (this) {
            Books -> if (isBook) "Books" else "Volumes"
            Chapters -> if (isBook) "Chapters" else "Issues"
            Specials -> "Specials"
            Related -> "Related"
            Recommendations -> "Recommendations"
            Reviews -> "Reviews"
        }
}

internal data class TabItem(
    val id: SeriesDetailTab,
    val count: Int,
)

@Composable
internal fun SectionChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val labelText = if (count != null) "$label $count" else label
    AssistChip(
        onClick = onClick,
        label = { Text(labelText) },
        colors =
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                labelColor =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    )
}

@Composable
internal fun ChapterListV2(
    chapters: List<net.dom53.inkita.data.api.dto.ChapterDto>,
    config: AppConfig,
    downloadStates: Map<Int, DownloadState> = emptyMap(),
    onChapterClick: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
    onChapterLongPress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
) {
    if (chapters.isEmpty()) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        chapters.forEachIndexed { index, chapter ->
            val coverUrl = chapterCoverUrl(config, chapter.id)
            val title =
                chapter.titleName?.takeIf { it.isNotBlank() }
                    ?: chapter.title?.takeIf { it.isNotBlank() }
                    ?: chapter.range?.takeIf { it.isNotBlank() }
                    ?: "Chapter ${index + 1}"
            val pagesRead = chapter.pagesRead ?: 0
            val pagesTotal = chapter.pages ?: 0
            Column(
                modifier =
                    Modifier
                        .width(140.dp)
                        .combinedClickable(
                            onClick = { onChapterClick(chapter, index) },
                            onLongClick = { onChapterLongPress(chapter, index) },
                        ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box {
                    CoverImage(
                        coverUrl = coverUrl,
                        context = androidx.compose.ui.platform.LocalContext.current,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                    )
                    val downloadState = downloadStates[chapter.id]
                    if (downloadState == DownloadState.Complete || downloadState == DownloadState.Partial) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 6.dp, bottom = 10.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                        shape = MaterialTheme.shapes.small,
                                    ).padding(4.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (downloadState == DownloadState.Complete) {
                                        Icons.Filled.DownloadDone
                                    } else {
                                        Icons.Filled.Downloading
                                    },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (pagesRead == 0) {
                        // Read colour outside the draw scope: MaterialTheme is composable-only.
                        val cornerFlagColor = MaterialTheme.colorScheme.primary
                        Canvas(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f),
                        ) {
                            val sizePx = 26.dp.toPx()
                            val path =
                                Path().apply {
                                    moveTo(size.width - sizePx, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width, sizePx)
                                    close()
                                }
                            drawPath(
                                path = path,
                                color = cornerFlagColor,
                            )
                        }
                    }
                    if (pagesTotal > 0 && pagesRead in 1 until pagesTotal) {
                        val progress =
                            (pagesRead.toFloat() / pagesTotal.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .align(Alignment.BottomStart)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .align(Alignment.BottomStart)
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Ch. ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ChapterCompactList(
    chapters: List<net.dom53.inkita.data.api.dto.ChapterDto>,
    downloadStates: Map<Int, DownloadState> = emptyMap(),
    onChapterClick: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
    onChapterLongPress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
    onToggleDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Boolean) -> Unit = { _, _ -> },
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
) {
    if (chapters.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(10.dp)
        chapters.forEachIndexed { index, chapter ->
            val pagesRead = chapter.pagesRead ?: 0
            val pagesTotal = chapter.pages ?: 0
            val isRead = pagesTotal > 0 && pagesRead >= pagesTotal
            val isCurrent = pagesRead in 1 until pagesTotal
            val downloadState = downloadStates[chapter.id]
            val isDownloaded = downloadState == DownloadState.Complete || downloadState == DownloadState.Partial
            val containerColor =
                if (isRead) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            val textColor =
                when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            val border =
                if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            val title =
                chapter.titleName?.takeIf { it.isNotBlank() }
                    ?: chapter.title?.takeIf { it.isNotBlank() }
                    ?: chapter.range?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.series_detail_chapter_fallback, index + 1)
            val label = stringResource(R.string.series_detail_chapter_fallback, index + 1)
            val density = LocalDensity.current
            val swipeDistance = with(density) { 160.dp.toPx() }
            val swipeState = rememberSwipeableState(initialValue = 0)
            val anchors = remember(swipeDistance) { mapOf(0f to 0, -swipeDistance to -1, swipeDistance to 1) }
            var actionTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(swipeState.currentValue) {
                if (swipeState.currentValue != 0 && !actionTriggered) {
                    actionTriggered = true
                    if (swipeState.currentValue == 1) {
                        val targetPage = if (isRead) 0 else pagesTotal
                        onUpdateProgress(chapter, targetPage)
                    } else {
                        onToggleDownload(chapter, isDownloaded)
                    }
                    swipeState.animateTo(0)
                    actionTriggered = false
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .swipeable(
                            state = swipeState,
                            anchors = anchors,
                            thresholds = { _, _ -> FractionalThreshold(0.25f) },
                            orientation = Orientation.Horizontal,
                        ),
            ) {
                val downloadIcon =
                    if (isDownloaded) {
                        Icons.Filled.Delete
                    } else {
                        Icons.Filled.FileDownload
                    }
                val downloadTint =
                    if (isDownloaded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                val progressIcon =
                    if (isRead) {
                        Icons.Filled.Undo
                    } else {
                        Icons.Filled.Done
                    }
                val progressTint =
                    if (isRead) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = progressIcon,
                            contentDescription = null,
                            tint = progressTint,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                        Icon(
                            imageVector = downloadIcon,
                            contentDescription = null,
                            tint = downloadTint,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .offset { IntOffset(swipeState.offset.value.roundToInt(), 0) }
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(containerColor, shape)
                            .then(
                                if (border != null) {
                                    Modifier.border(border, shape)
                                } else {
                                    Modifier
                                },
                            ).combinedClickable(
                                onClick = { onChapterClick(chapter, index) },
                                onLongClick = { onChapterLongPress(chapter, index) },
                            ).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.width(96.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // Downloading was previously swipe-only and the icon here was
                    // decoration. It is now the control: download when the issue is
                    // absent, delete when it is held locally.
                    IconButton(
                        onClick = { onToggleDownload(chapter, isDownloaded) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector =
                                when (downloadState) {
                                    DownloadState.Complete -> Icons.Filled.Delete
                                    DownloadState.Partial -> Icons.Filled.Downloading
                                    else -> Icons.Filled.Download
                                },
                            contentDescription =
                                stringResource(
                                    if (isDownloaded) {
                                        net.dom53.inkita.R.string.series_detail_delete_download
                                    } else {
                                        net.dom53.inkita.R.string.series_detail_download_issue
                                    },
                                ),
                            tint =
                                if (isDownloaded) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ChapterPagesSection(
    chapter: net.dom53.inkita.data.api.dto.ChapterDto?,
    chapterIndex: Int,
    downloadedPages: Set<Int>,
    pageTitles: Map<Int, String>?,
    onTogglePageDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Int, Boolean) -> Unit,
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit,
    onOpenPage: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val title =
        chapter
            ?.titleName
            ?.takeIf { it.isNotBlank() }
            ?: chapter
                ?.title
                ?.takeIf { it.isNotBlank() }
            ?: chapter
                ?.range
                ?.takeIf { it.isNotBlank() }
            ?: context.getString(net.dom53.inkita.R.string.series_detail_chapter_fallback, chapterIndex + 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    val pages = chapter?.pages ?: 0
    val pagesRead = chapter?.pagesRead ?: 0
    if (pages <= 0) {
        Text(
            text = stringResource(net.dom53.inkita.R.string.series_detail_pages_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(10.dp)
        repeat(pages) { index ->
            val isRead = index < pagesRead
            val isCurrent = pagesRead in 0 until pages && index == pagesRead
            val isDownloaded = downloadedPages.contains(index)
            val containerColor =
                if (isRead) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            val textColor =
                when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            val border =
                if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            val density = LocalDensity.current
            val swipeDistance = with(density) { 160.dp.toPx() }
            val swipeState = rememberSwipeableState(initialValue = 0)
            val anchors = remember(swipeDistance) { mapOf(0f to 0, -swipeDistance to -1, swipeDistance to 1) }
            var actionTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(swipeState.currentValue) {
                if (swipeState.currentValue != 0 && !actionTriggered) {
                    actionTriggered = true
                    chapter?.let {
                        if (swipeState.currentValue == 1) {
                            val pageNum = if (isRead) index else index + 1
                            onUpdateProgress(it, pageNum)
                        } else {
                            onTogglePageDownload(it, index, isDownloaded)
                        }
                    }
                    swipeState.animateTo(0)
                    actionTriggered = false
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .swipeable(
                            state = swipeState,
                            anchors = anchors,
                            thresholds = { _, _ -> FractionalThreshold(0.25f) },
                            orientation = Orientation.Horizontal,
                        ),
            ) {
                val downloadIcon =
                    if (isDownloaded) {
                        Icons.Filled.Delete
                    } else {
                        Icons.Filled.FileDownload
                    }
                val downloadTint =
                    if (isDownloaded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                val progressIcon =
                    if (isRead) {
                        Icons.Filled.Undo
                    } else {
                        Icons.Filled.Done
                    }
                val progressTint =
                    if (isRead) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = progressIcon,
                            contentDescription = null,
                            tint = progressTint,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                        Icon(
                            imageVector = downloadIcon,
                            contentDescription = null,
                            tint = downloadTint,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .offset { IntOffset(swipeState.offset.value.roundToInt(), 0) }
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(containerColor, shape)
                            .then(
                                if (border != null) {
                                    Modifier.border(border, shape)
                                } else {
                                    Modifier
                                },
                            ).clickable { chapter?.let { onOpenPage(it, index) } }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val pageTitle = pageTitles?.get(index)?.takeIf { it.isNotBlank() }
                    Text(
                        text = "${stringResource(net.dom53.inkita.R.string.general_page)} ${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.width(70.dp),
                    )
                    Text(
                        text = pageTitle ?: title,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Filled.DownloadDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoverImage(
    coverUrl: String?,
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    if (coverUrl != null) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = null,
            modifier =
                modifier
                    .clip(
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(8.dp),
                    ),
            contentScale = ContentScale.Crop,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

internal fun formatHours(value: Float?): String? = value?.let { String.format(Locale.US, "%.1f h", it) }

internal fun formatHoursDouble(value: Double?): String? = value?.let { String.format(Locale.US, "%.1f h", it) }

internal fun formatHoursRange(
    min: Double?,
    max: Double?,
): String? {
    if (min == null && max == null) return null
    val minText = min?.let { String.format(Locale.US, "%.1f h", it) }
    val maxText = max?.let { String.format(Locale.US, "%.1f h", it) }
    return when {
        minText != null && maxText != null -> "$minText - $maxText"
        minText != null -> minText
        else -> maxText
    }
}

internal fun formatHoursRangeInt(
    min: Int?,
    max: Int?,
): String? = formatHoursRange(min?.toDouble(), max?.toDouble())

internal fun formatCount(value: Long): String =
    when {
        value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
        value >= 1_000L -> String.format(Locale.US, "%.0fk", value / 1_000f)
        else -> value.toString()
    }

internal fun formatDate(value: String): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val parsed =
        runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrNull()
            ?: runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
    return parsed?.format(formatter) ?: value
}

internal fun formatYear(value: String): String? {
    val parsed =
        runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrNull()
            ?: runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
    return parsed?.year?.toString()
}

internal fun buildPageTitleMap(
    context: Context,
    pagesCount: Int,
    tocItems: List<TocItem>,
): Map<Int, String> {
    if (pagesCount <= 0) return emptyMap()
    val sorted = tocItems.sortedBy { it.page }
    if (sorted.isEmpty()) {
        return (0 until pagesCount).associateWith { idx ->
            "${context.getString(net.dom53.inkita.R.string.general_page)} ${idx + 1}"
        }
    }
    val result = HashMap<Int, String>(pagesCount)
    var cursor = 0
    var currentTitle: String? = null
    for (pageIndex in 0 until pagesCount) {
        while (cursor < sorted.size && sorted[cursor].page <= pageIndex) {
            currentTitle = sorted[cursor].title
            cursor++
        }
        result[pageIndex] =
            currentTitle?.takeIf { it.isNotBlank() }
                ?: "${context.getString(net.dom53.inkita.R.string.general_page)} ${pageIndex + 1}"
    }
    return result
}

internal fun volumeNumberText(volume: net.dom53.inkita.data.api.dto.VolumeDto): String? {
    val volNumber = volume.minNumber ?: volume.maxNumber
    if (volNumber == null) return null
    return if (volNumber % 1f == 0f) volNumber.toInt().toString() else volNumber.toString()
}

@Composable
internal fun CollectionDialogV2(
    collections: List<net.dom53.inkita.domain.model.Collection>,
    isLoading: Boolean,
    error: String?,
    membership: Set<Int>,
    onDismiss: () -> Unit,
    onLoadCollections: () -> Unit,
    onToggle: (net.dom53.inkita.domain.model.Collection, Boolean) -> Unit,
    onCreateCollection: (String) -> Unit,
) {
    var newTitle by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.general_collections)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator()
                } else {
                    if (collections.isEmpty()) {
                        Text(stringResource(R.string.series_detail_no_colls_reload))
                        TextButton(onClick = onLoadCollections) {
                            Text(stringResource(R.string.series_detail_load_colls))
                        }
                    } else {
                        collections.forEach { collection ->
                            val isInCollection = membership.contains(collection.id)
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggle(collection, !isInCollection) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(collection.name)
                                androidx.compose.material3.Switch(
                                    checked = isInCollection,
                                    onCheckedChange = { onToggle(collection, it) },
                                )
                            }
                        }
                    }
                }

                androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(stringResource(R.string.general_create_new_coll))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.general_coll_name)) },
                )
                Button(
                    onClick = {
                        onCreateCollection(newTitle.trim())
                        newTitle = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newTitle.isNotBlank(),
                ) {
                    Text(stringResource(R.string.general_create_and_add))
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.general_close))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SummarySectionV2(
    summary: String?,
    genres: List<Pair<Int?, String>>,
    tags: List<Pair<Int?, String>>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onGenreClick: (Int, String) -> Unit = { _, _ -> },
    onTagClick: (Int, String) -> Unit = { _, _ -> },
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val summaryText = summary?.ifBlank { null }
        if (summaryText != null) {
            Text(
                text = summaryText,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggle) {
                    Text(
                        text = if (expanded) stringResource(R.string.general_less) else stringResource(R.string.general_more),
                    )
                }
            }
        }

        if (expanded) {
            ChipGroup(
                title = stringResource(R.string.general_genres),
                items = genres,
                onItemClick = { id, name ->
                    if (id != null) onGenreClick(id, name)
                },
            )
            ChipGroup(
                title = stringResource(R.string.general_tags),
                items = tags,
                onItemClick = { id, name ->
                    if (id != null) onTagClick(id, name)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGroup(
    title: String,
    items: List<Pair<Int?, String>>,
    onItemClick: (Int?, String) -> Unit = { _, _ -> },
) {
    val trimmed = items.mapNotNull { (id, label) -> label.trim().takeIf { it.isNotEmpty() }?.let { id to it } }
    if (trimmed.isEmpty()) {
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            trimmed.forEach { (id, label) ->
                AssistChip(
                    onClick = { onItemClick(id, label) },
                    label = { Text(label) },
                )
            }
        }
    }
}

/**
 * Tab header: label with its count in a pill, underlined when selected.
 *
 * Replaces the filled-chip treatment, which gave every tab equal visual weight
 * and made the selected one read as a button rather than a position.
 */
@Composable
internal fun SectionTab(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(IntrinsicSize.Max)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
            )
            if (count != null) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
        // Spans the whole tab -- label and count together -- rather than a fixed
        // width that sat short of the count pill.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    ),
        )
    }
}

/**
 * Issues as cover art rather than text rows: two per row, each with its label
 * and an overflow menu carrying the per-issue actions.
 *
 * Laid out as manual rows instead of a lazy grid because this sits inside the
 * detail screen's vertical scroll, and nesting a second vertical scroller there
 * breaks measurement.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IssueGrid(
    chapters: List<net.dom53.inkita.data.api.dto.ChapterDto>,
    coverUrlFor: (net.dom53.inkita.data.api.dto.ChapterDto) -> String?,
    isBook: Boolean = false,
    /** Index to scroll into view; cleared via [onJumpHandled] once done. */
    jumpToIndex: Int? = null,
    onJumpHandled: () -> Unit = {},
    downloadStates: Map<Int, DownloadState> = emptyMap(),
    onChapterClick: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
    onToggleDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Boolean) -> Unit = { _, _ -> },
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit = { _, _ -> },
) {
    if (chapters.isEmpty()) return
    val indexed = chapters.withIndex().toList()
    if (isBook) {
        val rowRequesters = remember(chapters.size) { indexed.associate { it.index to BringIntoViewRequester() } }
        JumpEffect(jumpToIndex, onJumpHandled) { rowRequesters[it] }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            indexed.forEach { (index, chapter) ->
                IssueRow(
                    modifier =
                        rowRequesters[index]?.let { Modifier.bringIntoViewRequester(it) }
                            ?: Modifier,
                    chapter = chapter,
                    index = index,
                    downloadState = downloadStates[chapter.id] ?: DownloadState.None,
                    onClick = { onChapterClick(chapter, index) },
                    onToggleDownload = onToggleDownload,
                    onUpdateProgress = onUpdateProgress,
                )
            }
        }
        return
    }
    // One column per ~150dp of width, so a tablet or landscape phone fills the
    // row instead of stretching two cards across it.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 150.dp).toInt().coerceIn(2, 6)
        val rowRequesters =
            remember(chapters.size, columns) {
                indexed.chunked(columns).indices.associateWith { BringIntoViewRequester() }
            }
        JumpEffect(jumpToIndex, onJumpHandled) { target -> rowRequesters[target / columns] }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            indexed.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                rowRequesters[rowIndex]?.let { Modifier.bringIntoViewRequester(it) }
                                    ?: Modifier,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { (index, chapter) ->
                        IssueCard(
                            chapter = chapter,
                            index = index,
                            coverUrl = coverUrlFor(chapter),
                            downloadState = downloadStates[chapter.id] ?: DownloadState.None,
                            onClick = { onChapterClick(chapter, index) },
                            onToggleDownload = onToggleDownload,
                            onUpdateProgress = onUpdateProgress,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Pads the last row so a stray card keeps its column width
                    // rather than stretching across the gap.
                    repeat(columns - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(
    chapter: net.dom53.inkita.data.api.dto.ChapterDto,
    index: Int,
    coverUrl: String?,
    downloadState: DownloadState,
    onClick: () -> Unit,
    onToggleDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Boolean) -> Unit,
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagesRead = chapter.pagesRead ?: 0
    val pagesTotal = chapter.pages ?: 0
    val isRead = pagesTotal > 0 && pagesRead >= pagesTotal
    val isPartiallyRead = pagesRead in 1 until pagesTotal
    val isDownloaded = downloadState == DownloadState.Complete || downloadState == DownloadState.Partial
    val label = stringResource(R.string.series_detail_chapter_fallback, index + 1)
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // A read issue is dimmed so the unread ones stand out in a long grid.
            if (isRead) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                )
            }
            if (isDownloaded) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(3.dp)
                            .size(14.dp),
                )
            }
            if (isPartiallyRead) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(pagesRead.toFloat() / pagesTotal.toFloat())
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.series_detail_issue_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isRead) {
                                        R.string.series_detail_mark_unread
                                    } else {
                                        R.string.series_detail_mark_read
                                    },
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onUpdateProgress(chapter, if (isRead) 0 else pagesTotal)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isDownloaded) {
                                        R.string.series_detail_delete_download
                                    } else {
                                        R.string.series_detail_download_issue
                                    },
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleDownload(chapter, isDownloaded)
                        },
                    )
                }
            }
        }
    }
}

/**
 * The per-issue actions, shared by the grid card and the book chapter row.
 */
@Composable
private fun IssueActionsMenu(
    chapter: net.dom53.inkita.data.api.dto.ChapterDto,
    isRead: Boolean,
    isDownloaded: Boolean,
    pagesTotal: Int,
    onToggleDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Boolean) -> Unit,
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.series_detail_issue_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isRead) R.string.series_detail_mark_unread else R.string.series_detail_mark_read,
                        ),
                    )
                },
                onClick = {
                    menuOpen = false
                    onUpdateProgress(chapter, if (isRead) 0 else pagesTotal)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isDownloaded) R.string.series_detail_delete_download else R.string.series_detail_download_issue,
                        ),
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggleDownload(chapter, isDownloaded)
                },
            )
        }
    }
}

/**
 * A book chapter: title and state, no cover art, tap to jump straight to it.
 */
@Composable
private fun IssueRow(
    modifier: Modifier = Modifier,
    chapter: net.dom53.inkita.data.api.dto.ChapterDto,
    index: Int,
    downloadState: DownloadState,
    onClick: () -> Unit,
    onToggleDownload: (net.dom53.inkita.data.api.dto.ChapterDto, Boolean) -> Unit,
    onUpdateProgress: (net.dom53.inkita.data.api.dto.ChapterDto, Int) -> Unit,
) {
    val pagesRead = chapter.pagesRead ?: 0
    val pagesTotal = chapter.pages ?: 0
    val isRead = pagesTotal > 0 && pagesRead >= pagesTotal
    val isCurrent = pagesRead in 1 until pagesTotal
    val isDownloaded = downloadState == DownloadState.Complete || downloadState == DownloadState.Partial
    val shape = RoundedCornerShape(10.dp)
    val title =
        chapter.titleName?.takeIf { it.isNotBlank() }
            ?: chapter.title?.takeIf { it.isNotBlank() }
            ?: chapter.range?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.series_detail_chapter_fallback, index + 1)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.series_detail_chapter_fallback, index + 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isDownloaded) {
            Icon(
                imageVector = Icons.Filled.DownloadDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        IssueActionsMenu(
            chapter = chapter,
            isRead = isRead,
            isDownloaded = isDownloaded,
            pagesTotal = pagesTotal,
            onToggleDownload = onToggleDownload,
            onUpdateProgress = onUpdateProgress,
        )
    }
}

/** Scrolls a requested index into view, then clears the request. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JumpEffect(
    jumpToIndex: Int?,
    onJumpHandled: () -> Unit,
    requesterFor: (Int) -> BringIntoViewRequester?,
) {
    LaunchedEffect(jumpToIndex) {
        val target = jumpToIndex ?: return@LaunchedEffect
        requesterFor(target)?.bringIntoView()
        onJumpHandled()
    }
}
