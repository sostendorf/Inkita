package net.dom53.inkita.ui.seriesdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import net.dom53.inkita.core.downloadv2.DownloadManagerV2
import net.dom53.inkita.core.downloadv2.DownloadRequestV2
import net.dom53.inkita.core.downloadv2.strategies.ChapterImageArchiveDownloadStrategyV2
import net.dom53.inkita.core.downloadv2.strategies.DownloadApiStrategyV2
import net.dom53.inkita.core.downloadv2.strategies.EpubDownloadStrategyV2
import net.dom53.inkita.core.downloadv2.strategies.PdfDownloadStrategyV2
import net.dom53.inkita.core.logging.LoggingManager
import net.dom53.inkita.core.network.KavitaApiFactory
import net.dom53.inkita.core.network.NetworkUtils
import net.dom53.inkita.core.storage.AppConfig
import net.dom53.inkita.core.storage.AppPreferences
import net.dom53.inkita.data.api.dto.ReaderProgressDto
import net.dom53.inkita.data.local.db.InkitaDatabase
import net.dom53.inkita.data.local.db.entity.DownloadJobV2Entity
import net.dom53.inkita.data.mapper.flattenToc
import net.dom53.inkita.domain.model.Format
import net.dom53.inkita.domain.repository.ReaderRepository
import net.dom53.inkita.ui.common.DownloadState
import net.dom53.inkita.ui.common.DownloadStateResolver
import net.dom53.inkita.ui.common.chapterCoverUrl
import net.dom53.inkita.ui.common.seriesCoverUrl
import net.dom53.inkita.ui.common.volumeCoverUrl
import net.dom53.inkita.ui.reader.model.ReaderReturn
import net.dom53.inkita.ui.seriesdetail.utils.cleanHtml

@Suppress("CyclomaticComplexMethod")
@Composable
fun VolumeDetailScreenV2(
    volumeId: Int,
    appPreferences: AppPreferences,
    readerRepository: ReaderRepository,
    readerReturn: ReaderReturn? = null,
    onConsumeReaderReturn: () -> Unit = {},
    onOpenReader: (chapterId: Int, page: Int, seriesId: Int, volumeId: Int, formatId: Int?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val downloadDao = remember(context.applicationContext) { InkitaDatabase.getInstance(context.applicationContext).downloadV2Dao() }
    val chapterDownloadStates = remember { mutableStateMapOf<Int, DownloadState>() }
    val payload = remember(volumeId) { VolumeDetailCache.get(volumeId) }
    val config by appPreferences.configFlow.collectAsState(
        initial = AppConfig(serverUrl = "", apiKey = "", imageApiKey = "", userId = 0),
    )
    var coverExpanded by remember { mutableStateOf(false) }
    var summaryExpanded by remember { mutableStateOf(false) }
    val offlineMode by appPreferences.offlineModeFlow.collectAsState(initial = false)
    val format = Format.fromId(payload?.formatId)
    val isEpub = format == Format.Epub
    val isImage = format == Format.Image
    val isArchive = format == Format.Archive
    val isSingleFile = format == Format.Pdf || isImage || isArchive
    val haptics = LocalHapticFeedback.current
    var selectedChapter by remember(volumeId) { mutableStateOf<net.dom53.inkita.data.api.dto.ChapterDto?>(null) }
    var selectedChapterIndex by remember(volumeId) { mutableStateOf<Int?>(null) }
    val pageTitleCache = remember(volumeId) { mutableStateMapOf<Int, Map<Int, String>>() }
    val scope = rememberCoroutineScope()
    val selectedChapterId = selectedChapter?.id
    val downloadManagerV2 =
        remember(context.applicationContext) {
            val epubStrategy =
                EpubDownloadStrategyV2(
                    appContext = context.applicationContext,
                    downloadDao = downloadDao,
                    appPreferences = appPreferences,
                )
            val pdfStrategy =
                PdfDownloadStrategyV2(
                    appContext = context.applicationContext,
                    downloadDao = downloadDao,
                    appPreferences = appPreferences,
                )
            val imageStrategy =
                ChapterImageArchiveDownloadStrategyV2(
                    appContext = context.applicationContext,
                    downloadDao = downloadDao,
                    appPreferences = appPreferences,
                    key = ChapterImageArchiveDownloadStrategyV2.FORMAT_IMAGE,
                )
            val archiveStrategy =
                ChapterImageArchiveDownloadStrategyV2(
                    appContext = context.applicationContext,
                    downloadDao = downloadDao,
                    appPreferences = appPreferences,
                    key = ChapterImageArchiveDownloadStrategyV2.FORMAT_ARCHIVE,
                )
            val downloadStrategy =
                DownloadApiStrategyV2(
                    appContext = context.applicationContext,
                    downloadDao = downloadDao,
                    appPreferences = appPreferences,
                )
            DownloadManagerV2(
                appContext = context.applicationContext,
                downloadDao = downloadDao,
                strategies =
                    listOf(
                        epubStrategy,
                        pdfStrategy,
                        imageStrategy,
                        archiveStrategy,
                        downloadStrategy,
                    ).associateBy { it.key },
            )
        }

    if (payload == null) {
        if (LoggingManager.isDebugEnabled()) {
            LoggingManager.d("VolumeDetailV2", "Missing payload for volume=$volumeId")
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = net.dom53.inkita.R.string.volume_detail_not_found),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    var volumeState by remember(volumeId) { mutableStateOf(payload.volume) }
    LaunchedEffect(readerReturn) {
        if (readerReturn != null) {
            if (LoggingManager.isDebugEnabled()) {
                LoggingManager.d("VolumeDetailV2", "Reader return; refresh volume=$volumeId")
            }
            applyLocalProgress(volumeState, readerRepository)?.let { updated ->
                volumeState = updated
                VolumeDetailCache.put(payload.copy(volume = updated))
            }
            if (offlineMode) {
                onConsumeReaderReturn()
                return@LaunchedEffect
            }
            val cfg = appPreferences.configFlow.first()
            if (cfg.isConfigured) {
                val api = KavitaApiFactory.createAuthenticated(cfg.serverUrl, cfg.apiKey)
                val resp = api.getVolumeById(volumeId)
                if (resp.isSuccessful) {
                    resp.body()?.let { updated ->
                        val merged =
                            updated.copy(
                                name = payload.volume.name,
                                title = payload.volume.title,
                            )
                        volumeState = merged
                        VolumeDetailCache.put(payload.copy(volume = merged))
                        if (LoggingManager.isDebugEnabled()) {
                            LoggingManager.d("VolumeDetailV2", "Volume refreshed from API volume=$volumeId")
                        }
                    }
                }
            }
            onConsumeReaderReturn()
        }
    }
    LaunchedEffect(volumeId, offlineMode) {
        if (offlineMode) {
            if (LoggingManager.isDebugEnabled()) {
                LoggingManager.d("VolumeDetailV2", "Offline mode; applying local progress volume=$volumeId")
            }
            applyLocalProgress(volumeState, readerRepository)?.let { updated ->
                volumeState = updated
                VolumeDetailCache.put(payload.copy(volume = updated))
            }
        }
    }
    LaunchedEffect(selectedChapterId, offlineMode, config.serverUrl, config.apiKey) {
        val chapter = selectedChapter ?: return@LaunchedEffect
        if (offlineMode) return@LaunchedEffect
        if (!isEpub) return@LaunchedEffect
        if (pageTitleCache.containsKey(chapter.id)) return@LaunchedEffect
        if (!config.isConfigured) return@LaunchedEffect
        val pages = chapter.pages ?: 0
        if (pages <= 0) return@LaunchedEffect
        val api = KavitaApiFactory.createAuthenticated(config.serverUrl, config.apiKey)
        val tocResponse = api.getBookChapters(chapter.id)
        if (!tocResponse.isSuccessful) return@LaunchedEffect
        val tocItems = tocResponse.body().orEmpty().flatMap { flattenToc(it) }
        pageTitleCache[chapter.id] = buildPageTitleMap(context, pages, tocItems)
    }

    val volume = volumeState
    val coverUrl = volumeCoverUrl(config, volume.id) ?: seriesCoverUrl(config, payload.seriesId)
    val chapterList = volume.chapters.orEmpty()
    val downloadedItemsByVolume =
        downloadDao
            .observeItemsForVolume(volume.id)
            .collectAsState(initial = emptyList())
    val downloadedItemsForChapterFlow =
        remember(selectedChapterId) {
            if (selectedChapterId != null) {
                downloadDao.observeItemsForChapter(selectedChapterId)
            } else {
                flowOf(emptyList())
            }
        }
    val downloadedItemsForChapter = downloadedItemsForChapterFlow.collectAsState(initial = emptyList())
    val summary = chapterList.firstOrNull { !it.summary.isNullOrBlank() }?.summary
    val releaseYear =
        chapterList
            .firstOrNull { !it.releaseDate.isNullOrBlank() }
            ?.releaseDate
            ?.let { formatYear(it) }
    val wordCount =
        chapterList
            .mapNotNull { it.wordCount }
            .sum()
            .takeIf { it > 0 }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CoverImage(
                        coverUrl = coverUrl,
                        context = context,
                        modifier =
                            Modifier
                                .width(140.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { coverExpanded = true },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val title =
                            volume.name?.takeIf { it.isNotBlank() }
                                ?: stringResource(
                                    id = net.dom53.inkita.R.string.series_detail_volume_fallback,
                                    volumeId,
                                )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text =
                                stringResource(
                                    id = net.dom53.inkita.R.string.volume_detail_time_label,
                                    formatHoursRangeInt(volume.minHoursToRead, volume.maxHoursToRead) ?: "-",
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text =
                                "${stringResource(id = net.dom53.inkita.R.string.general_words)}: " +
                                    (wordCount?.let { formatCount(it) } ?: "-"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text =
                                stringResource(
                                    id = net.dom53.inkita.R.string.volume_detail_release_year_label,
                                    releaseYear ?: "-",
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SummarySectionV2(
                    summary = cleanHtml(summary) ?: summary,
                    genres = emptyList(),
                    tags = emptyList(),
                    expanded = summaryExpanded,
                    onToggle = { summaryExpanded = !summaryExpanded },
                )
                val volText =
                    volumeNumberText(volume)?.let {
                        stringResource(id = net.dom53.inkita.R.string.series_detail_vol_short, it)
                    }
                        ?: stringResource(id = net.dom53.inkita.R.string.series_detail_vol_short_plain)
                val pagesRead = volume.pagesRead ?: 0
                val pagesTotal = volume.pages
                // A volume's pagesRead is cumulative across its issues, so walk the
                // issues to find which one holds that page and how far into it we are.
                // The button's tap handler wants the same answer, so it is worked out
                // once here rather than twice.
                val resumeTarget =
                    remember(chapterList, pagesRead) {
                        val safe = pagesRead.coerceAtLeast(0)
                        if (chapterList.isEmpty()) {
                            null
                        } else if (safe <= 0) {
                            chapterList.first() to 0
                        } else {
                            var cumulative = 0
                            val found =
                                chapterList.firstOrNull { ch ->
                                    val count = ch.pages ?: 0
                                    val next = cumulative + count
                                    val within = count > 0 && safe < next
                                    if (!within) cumulative = next
                                    within
                                }
                            if (found != null) found to (safe - cumulative) else chapterList.first() to safe
                        }
                    }
                val resumeIssueNumber =
                    resumeTarget?.first?.let { ch ->
                        ch.number?.takeIf { it.isNotBlank() } ?: ch.range?.takeIf { it.isNotBlank() }
                    }
                val buttonLabel =
                    when {
                        pagesTotal != null && pagesRead >= pagesTotal ->
                            stringResource(id = net.dom53.inkita.R.string.volume_detail_re_read)
                        pagesRead <= 0 ->
                            stringResource(
                                id = net.dom53.inkita.R.string.volume_detail_start_reading,
                                volText,
                            )
                        resumeIssueNumber != null ->
                            stringResource(
                                id =
                                    if (isEpub) {
                                        net.dom53.inkita.R.string.volume_detail_continue_chapter
                                    } else {
                                        net.dom53.inkita.R.string.volume_detail_continue_issue
                                    },
                                resumeIssueNumber,
                                (resumeTarget?.second ?: 0) + 1,
                            )
                        else ->
                            stringResource(
                                id = net.dom53.inkita.R.string.volume_detail_continue_label,
                                volText,
                                pagesRead + 1,
                            )
                    }
                Button(
                    onClick = {
                        val chapters = chapterList
                        if (chapters.isEmpty()) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(net.dom53.inkita.R.string.general_not_implemented),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            return@Button
                        }
                        val target = resumeTarget
                        val chapter = target?.first
                        val page = target?.second
                        if (chapter == null || page == null) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(net.dom53.inkita.R.string.general_not_implemented),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            return@Button
                        }
                        onOpenReader(
                            chapter.id,
                            page,
                            payload.seriesId,
                            volume.id,
                            payload.formatId,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = buttonLabel)
                }
                val tabs =
                    listOf(
                        TabItem(SeriesDetailTab.Chapters, chapterList.size),
                    ).filter { it.count > 0 }
                if (selectedChapter != null) {
                    val pdfDownloaded =
                        isSingleFile &&
                            downloadedItemsForChapter.value.any { item ->
                                item.type == net.dom53.inkita.data.local.db.entity.DownloadedItemV2Entity.TYPE_FILE &&
                                    item.status == net.dom53.inkita.data.local.db.entity.DownloadedItemV2Entity.STATUS_COMPLETED &&
                                    isItemPathPresent(item.localPath)
                            }
                    val downloadedPages =
                        downloadedItemsForChapter.value
                            .filter { it.status == net.dom53.inkita.data.local.db.entity.DownloadedItemV2Entity.STATUS_COMPLETED }
                            .filter { item -> isItemPathPresent(item.localPath) }
                            .mapNotNull { it.page }
                            .toSet()
                    ChapterPagesSection(
                        chapter = selectedChapter,
                        chapterIndex = selectedChapterIndex ?: 0,
                        downloadedPages = downloadedPages,
                        pageTitles = selectedChapter?.id?.let { pageTitleCache[it] },
                        onTogglePageDownload = { chapter, page, isDownloaded ->
                            if (offlineMode) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(net.dom53.inkita.R.string.general_offline_mode),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@ChapterPagesSection
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isDownloaded) {
                                scope.launch {
                                    downloadDao.deleteItemsForChapterPage(chapter.id, page)
                                }
                            } else {
                                if (!isEpub) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.general_not_implemented),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@ChapterPagesSection
                                }
                                scope.launch {
                                    val request =
                                        DownloadRequestV2(
                                            type = DownloadJobV2Entity.TYPE_CHAPTER,
                                            format = formatKeyForVolume(payload.formatId),
                                            seriesId = payload.seriesId,
                                            volumeId = volume.id,
                                            chapterId = chapter.id,
                                            pageIndex = page,
                                        )
                                    downloadManagerV2.enqueue(request)
                                }
                            }
                        },
                        onUpdateProgress = { chapter, pageNum ->
                            if (offlineMode) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(net.dom53.inkita.R.string.general_offline_mode),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@ChapterPagesSection
                            }
                            if (!config.isConfigured) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(net.dom53.inkita.R.string.general_no_server_logged_in),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@ChapterPagesSection
                            }
                            val libraryId = payload.libraryId
                            if (libraryId == null) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(net.dom53.inkita.R.string.general_error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@ChapterPagesSection
                            }
                            val markRead = pageNum > (chapter.pagesRead ?: 0)
                            scope.launch {
                                val api = KavitaApiFactory.createAuthenticated(config.serverUrl, config.apiKey)
                                val resp =
                                    api.setReaderProgress(
                                        ReaderProgressDto(
                                            libraryId = libraryId,
                                            seriesId = payload.seriesId,
                                            volumeId = volume.id,
                                            chapterId = chapter.id,
                                            pageNum = pageNum,
                                            bookScrollId = null,
                                        ),
                                    )
                                if (resp.isSuccessful) {
                                    val updatedChapters =
                                        volumeState.chapters.orEmpty().map {
                                            if (it.id == chapter.id) it.copy(pagesRead = pageNum) else it
                                        }
                                    val updatedPagesRead =
                                        updatedChapters.sumOf { it.pagesRead ?: 0 }
                                    val updatedVolume =
                                        volumeState.copy(
                                            chapters = updatedChapters,
                                            pagesRead = updatedPagesRead,
                                        )
                                    volumeState = updatedVolume
                                    selectedChapter =
                                        updatedChapters.firstOrNull { it.id == chapter.id }
                                    VolumeDetailCache.put(payload.copy(volume = updatedVolume))
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(
                                                if (markRead) {
                                                    net.dom53.inkita.R.string.general_mark_read
                                                } else {
                                                    net.dom53.inkita.R.string.general_mark_unread
                                                },
                                            ),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.general_error),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        },
                        onOpenPage = { chapter, page ->
                            if ((offlineMode || !NetworkUtils.isOnline(context)) && !pdfDownloaded && !downloadedPages.contains(page)) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(net.dom53.inkita.R.string.reader_page_not_downloaded),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@ChapterPagesSection
                            }
                            onOpenReader(
                                chapter.id,
                                page,
                                payload.seriesId,
                                volume.id,
                                payload.formatId,
                            )
                        },
                        onBack = {
                            selectedChapter = null
                            selectedChapterIndex = null
                        },
                    )
                } else if (tabs.isNotEmpty()) {
                    var selectedTab by remember { mutableStateOf(tabs.first().id) }
                    LaunchedEffect(chapterList, downloadedItemsByVolume.value) {
                        val items = downloadedItemsByVolume.value
                        val grouped = items.groupBy { it.chapterId }
                        chapterList.forEach { chapter ->
                            val list = grouped[chapter.id].orEmpty()
                            val state =
                                DownloadStateResolver.resolveChapterState(
                                    format = format,
                                    chapter = chapter,
                                    items = list,
                                )
                            chapterDownloadStates[chapter.id] = state
                        }
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            SectionTab(
                                label = tab.id.labelFor(isBook = isEpub),
                                count = tab.count,
                                selected = selectedTab == tab.id,
                                onClick = { selectedTab = tab.id },
                            )
                        }
                    }
                    if (selectedTab == SeriesDetailTab.Chapters) {
                        IssueGrid(
                            chapters = chapterList,
                            isBook = isEpub,
                            coverUrlFor = { chapter ->
                                chapterCoverUrl(config, chapter.id)
                                    ?: volumeCoverUrl(config, volume.id)
                            },
                            downloadStates = chapterDownloadStates,
                            onChapterClick = { chapter, index ->
                                selectedChapter = chapter
                                selectedChapterIndex = index
                            },
                            onToggleDownload = onToggleDownload@{ chapter, isDownloaded ->
                                if (offlineMode) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.general_offline_mode),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@onToggleDownload
                                }
                                if (isDownloaded) {
                                    scope.launch {
                                        downloadDao.deleteItemsForChapter(chapter.id)
                                        downloadDao.deleteJobsForChapter(chapter.id)
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(
                                                    net.dom53.inkita.R.string.settings_downloads_clear_downloaded_toast,
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                    return@onToggleDownload
                                }
                                val pages = if (isSingleFile) 1 else chapter.pages ?: 0
                                if (!isSingleFile && pages <= 0) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.series_detail_pages_unavailable),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@onToggleDownload
                                }
                                scope.launch {
                                    downloadManagerV2.enqueue(
                                        DownloadRequestV2(
                                            type = DownloadJobV2Entity.TYPE_CHAPTER,
                                            format = formatKeyForVolume(payload.formatId),
                                            seriesId = payload.seriesId,
                                            volumeId = volume.id,
                                            chapterId = chapter.id,
                                            pageCount = pages,
                                        ),
                                    )
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.download_queued),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            },
                            onUpdateProgress = onUpdateProgress@{ chapter, pageNum ->
                                if (offlineMode) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.general_offline_mode),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@onUpdateProgress
                                }
                                // Progress writes need the owning library; without it the
                                // server rejects the call, so bail before the round trip.
                                val progressLibraryId = payload.libraryId
                                if (progressLibraryId == null) {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(net.dom53.inkita.R.string.general_error),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@onUpdateProgress
                                }
                                val markRead = pageNum > (chapter.pagesRead ?: 0)
                                scope.launch {
                                    val api = KavitaApiFactory.createAuthenticated(config.serverUrl, config.apiKey)
                                    val resp =
                                        api.setReaderProgress(
                                            ReaderProgressDto(
                                                libraryId = progressLibraryId,
                                                seriesId = payload.seriesId,
                                                volumeId = volume.id,
                                                chapterId = chapter.id,
                                                pageNum = pageNum,
                                                bookScrollId = null,
                                            ),
                                        )
                                    if (resp.isSuccessful) {
                                        // Keep the cached volume in step so the list and the
                                        // continue button agree without a round trip.
                                        val updatedChapters =
                                            volumeState.chapters.orEmpty().map {
                                                if (it.id == chapter.id) it.copy(pagesRead = pageNum) else it
                                            }
                                        val updatedVolume =
                                            volumeState.copy(
                                                chapters = updatedChapters,
                                                pagesRead = updatedChapters.sumOf { it.pagesRead ?: 0 },
                                            )
                                        volumeState = updatedVolume
                                        VolumeDetailCache.put(payload.copy(volume = updatedVolume))
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(
                                                    if (markRead) {
                                                        net.dom53.inkita.R.string.general_mark_read
                                                    } else {
                                                        net.dom53.inkita.R.string.general_mark_unread
                                                    },
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(net.dom53.inkita.R.string.general_error),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        if (coverExpanded) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { coverExpanded = false },
                contentAlignment = Alignment.Center,
            ) {
                CoverImage(
                    coverUrl = coverUrl,
                    context = context,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { coverExpanded = false },
                )
            }
        }
    }
}

private fun isItemPathPresent(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    return if (path.startsWith("content://")) {
        true
    } else {
        val normalized = path.removePrefix("file://")
        java.io.File(normalized).exists()
    }
}

private suspend fun applyLocalProgress(
    volume: net.dom53.inkita.data.api.dto.VolumeDto,
    readerRepository: ReaderRepository,
): net.dom53.inkita.data.api.dto.VolumeDto? {
    val chapters = volume.chapters.orEmpty()
    if (chapters.isEmpty()) return null
    val chapterIds = chapters.map { it.id }.toSet()
    val local = readerRepository.getLatestLocalProgressForChapters(chapterIds) ?: return null
    val pageInChapter = local.page ?: 0
    var cumulative = 0
    var updatedPagesRead: Int? = null
    val updatedChapters =
        chapters.map { ch ->
            val pages = ch.pages ?: 0
            if (ch.id == local.chapterId) {
                updatedPagesRead = (cumulative + pageInChapter).coerceAtLeast(0)
                ch.copy(pagesRead = pageInChapter)
            } else {
                cumulative += pages
                ch
            }
        }
    if (updatedPagesRead == null) return null
    return volume.copy(
        pagesRead = updatedPagesRead,
        chapters = updatedChapters,
    )
}

private fun formatKeyForVolume(formatId: Int?): String =
    when (
        net.dom53.inkita.domain.model.Format
            .fromId(formatId)
    ) {
        net.dom53.inkita.domain.model.Format.Pdf -> PdfDownloadStrategyV2.FORMAT_PDF
        net.dom53.inkita.domain.model.Format.Epub -> EpubDownloadStrategyV2.FORMAT_EPUB
        net.dom53.inkita.domain.model.Format.Image -> ChapterImageArchiveDownloadStrategyV2.FORMAT_IMAGE
        net.dom53.inkita.domain.model.Format.Archive -> ChapterImageArchiveDownloadStrategyV2.FORMAT_ARCHIVE
        else -> DownloadApiStrategyV2.FORMAT_DOWNLOAD
    }
