package net.dom53.inkita.ui.reader.renderer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import net.dom53.inkita.R
import net.dom53.inkita.core.storage.ImageReaderMode

/**
 * Full-bleed comic/manga page reader.
 *
 * Pages live in a pager so a swipe turns the page directly instead of firing a
 * reload: the pages on either side are resolved and decoded ahead of the one on
 * screen. Nothing is drawn over the artwork — the chrome appears only when the
 * middle of the screen is tapped.
 */
object ImageReader : BaseReader {
    override val supportsTextSettings: Boolean = false

    /** Fraction of screen width at each edge that turns the page when tapped. */
    private const val EDGE_TAP_FRACTION = 0.28f
    private const val MAX_ZOOM = 5f
    private const val DOUBLE_TAP_ZOOM = 2.5f
    private const val ZOOM_EPSILON = 1.01f

    @Composable
    override fun Content(
        params: ReaderRenderParams,
        callbacks: ReaderRenderCallbacks,
    ) {
        val pageCount = params.uiState.pageCount
        val isRtl = params.imageReaderMode == ImageReaderMode.RightToLeft
        val isVertical = params.imageReaderMode == ImageReaderMode.Vertical

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (pageCount <= 0) {
                // Page count unknown (still loading, or offline without a manifest):
                // show whatever single page the view model resolved.
                SinglePage(params = params, callbacks = callbacks)
                return@Box
            }

            val pagerState =
                rememberPagerState(
                    initialPage = params.uiState.pageIndex.coerceIn(0, pageCount - 1),
                    pageCount = { pageCount },
                )

            // Bottom-bar jumps and chapter loads move the index from outside the pager;
            // follow them without animating so the jump is instant.
            LaunchedEffect(params.uiState.pageIndex, pageCount) {
                val target = params.uiState.pageIndex.coerceIn(0, pageCount - 1)
                if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
                    pagerState.scrollToPage(target)
                }
            }

            // Report the settled page so reading progress syncs back to Kavita.
            LaunchedEffect(pagerState, pageCount) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect { settled -> callbacks.onPageSettled(settled) }
            }

            var zoomed by remember { mutableStateOf(false) }

            if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !zoomed,
                ) { page ->
                    ComicPage(
                        pageIndex = page,
                        params = params,
                        callbacks = callbacks,
                        pagerState = pagerState,
                        isRtl = false,
                        onZoomChanged = { zoomed = it },
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    reverseLayout = isRtl,
                    userScrollEnabled = !zoomed,
                ) { page ->
                    ComicPage(
                        pageIndex = page,
                        params = params,
                        callbacks = callbacks,
                        pagerState = pagerState,
                        isRtl = isRtl,
                        onZoomChanged = { zoomed = it },
                    )
                }
            }
        }
    }

    /**
     * One page: artwork at full size, pinch and double-tap zoom, and the tap zones
     * that turn pages or reveal the chrome.
     */
    @Composable
    private fun ComicPage(
        pageIndex: Int,
        params: ReaderRenderParams,
        callbacks: ReaderRenderCallbacks,
        pagerState: PagerState,
        isRtl: Boolean,
        onZoomChanged: (Boolean) -> Unit,
    ) {
        val provider = params.pageUrlProvider
        val fallbackUrl = params.uiState.imageUrl.takeIf { pageIndex == params.uiState.pageIndex }
        val imageUrl by produceState(initialValue = fallbackUrl, pageIndex, provider) {
            value = runCatching { provider(pageIndex) }.getOrNull() ?: fallbackUrl
        }

        var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
        var offsetX by remember(pageIndex) { mutableFloatStateOf(0f) }
        var offsetY by remember(pageIndex) { mutableFloatStateOf(0f) }
        val isZoomed by rememberUpdatedState(scale > ZOOM_EPSILON)

        // A page scrolled off screen should not stay zoomed in.
        LaunchedEffect(pagerState.settledPage) {
            if (pagerState.settledPage != pageIndex && scale != 1f) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            }
        }
        LaunchedEffect(isZoomed, pagerState.settledPage) {
            if (pagerState.settledPage == pageIndex) onZoomChanged(isZoomed)
        }

        val animatedScale by animateFloatAsState(targetValue = scale, label = "page-zoom")

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Taps: edges turn the page, the middle toggles the chrome.
                    .pointerInput(pageIndex, isRtl) {
                        detectTapGestures(
                            onDoubleTap = { tap ->
                                if (scale > ZOOM_EPSILON) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = DOUBLE_TAP_ZOOM
                                    offsetX = (size.width / 2f - tap.x) * (DOUBLE_TAP_ZOOM - 1f)
                                    offsetY = (size.height / 2f - tap.y) * (DOUBLE_TAP_ZOOM - 1f)
                                }
                            },
                            onTap = { tap ->
                                if (scale > ZOOM_EPSILON) {
                                    callbacks.onToggleOverlay()
                                    return@detectTapGestures
                                }
                                val edge = size.width * EDGE_TAP_FRACTION
                                val forward = if (isRtl) tap.x < edge else tap.x > size.width - edge
                                val backward = if (isRtl) tap.x > size.width - edge else tap.x < edge
                                when {
                                    forward -> callbacks.onSwipeNext()
                                    backward -> callbacks.onSwipePrev()
                                    else -> callbacks.onToggleOverlay()
                                }
                            },
                        )
                    }
                    // Zoom and pan. Deliberately ignores single-finger drags while the
                    // page is at rest, so those reach the pager and turn the page.
                    .pointerInput(pageIndex) {
                        detectZoomAndPan(isZoomed = { scale > ZOOM_EPSILON }) { pan, zoom ->
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            scale = newScale
                            if (newScale > 1f) {
                                val boundX = (newScale - 1f) * size.width / 2f
                                val boundY = (newScale - 1f) * size.height / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-boundX, boundX)
                                offsetY = (offsetY + pan.y).coerceIn(-boundY, boundY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            when {
                imageUrl.isNullOrBlank() && params.uiState.isLoading ->
                    CircularProgressIndicator(color = Color.White)

                imageUrl.isNullOrBlank() ->
                    Text(
                        text = params.uiState.error ?: stringResource(R.string.general_error),
                        color = MaterialTheme.colorScheme.error,
                    )

                else ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                    )
            }
        }
    }

    /** Shown before the page count is known: a single image with edge-tap paging. */
    @Composable
    private fun SinglePage(
        params: ReaderRenderParams,
        callbacks: ReaderRenderCallbacks,
    ) {
        val imageUrl = params.uiState.imageUrl
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(params.uiState.pageIndex) {
                        detectTapGestures(
                            onTap = { tap ->
                                val edge = size.width * EDGE_TAP_FRACTION
                                when {
                                    tap.x > size.width - edge -> callbacks.onSwipeNext()
                                    tap.x < edge -> callbacks.onSwipePrev()
                                    else -> callbacks.onToggleOverlay()
                                }
                            },
                        )
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                if (params.uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = params.uiState.error ?: stringResource(R.string.general_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Zoom/pan detector that leaves single-finger drags alone unless the page is
 * already zoomed in.
 *
 * Compose's stock `detectTransformGestures` consumes one-finger drags, which would
 * stop the pager ever seeing a swipe. This version only claims the gesture once a
 * second finger is down or the page is zoomed, so at rest a swipe turns the page
 * and a pinch still zooms.
 */
private suspend fun PointerInputScope.detectZoomAndPan(
    isZoomed: () -> Boolean,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var claimed = false
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed && !claimed }
            if (canceled) break

            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount > 1 || isZoomed()) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                if (zoom != 1f || pan != Offset.Zero) {
                    claimed = true
                    onTransform(pan, zoom)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
