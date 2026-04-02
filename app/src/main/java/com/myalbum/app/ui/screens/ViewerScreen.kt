package com.myalbum.app.ui.screens

import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.myalbum.app.data.MediaItem as AppMediaItem
import com.myalbum.app.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    items: List<AppMediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onItemDeleted: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var isSystemUiVisible by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(initialIndex) }
    var isSlideShowActive by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.release()
        }
    }

    LaunchedEffect(currentPage) {
        val currentItem = items.getOrNull(currentPage)
        if (currentItem != null && currentItem.isVideo) {
            exoPlayer.setMediaItem(MediaItem.fromUri(currentItem.uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
        }
    }

    // Control system bars
    LaunchedEffect(isSystemUiVisible) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isSystemUiVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    BackHandler {
        if (showInfoSheet) {
            showInfoSheet = false
        } else if (isSlideShowActive) {
            isSlideShowActive = false
        } else {
            onBack()
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Khong co media", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val safeIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    LaunchedEffect(isSlideShowActive) {
        if (isSlideShowActive) {
            while (true) {
                delay(3000)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    if (showInfoSheet) {
        val currentItem = items.getOrNull(currentPage)
        if (currentItem != null) {
            InfoBottomSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Xoa media", fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "Ban co chac muon xoa \"${items.getOrNull(currentPage)?.name}\"?\n\nHanh dong khong the hoan tac.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val itemToDelete = items.getOrNull(currentPage) ?: return@TextButton
                        val contentUri = itemToDelete.contentUri
                        try {
                            val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
                            if (rowsDeleted > 0) {
                                Toast.makeText(context, "Da xoa thanh cong", Toast.LENGTH_SHORT).show()
                                onItemDeleted?.invoke(currentPage)
                                if (items.size <= 1) onBack()
                            } else {
                                Toast.makeText(context, "Khong the xoa", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Loi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Xoa", color = AppColors.DangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Huy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> items[index].id }
        ) { page ->
            val item = items[page]
            ViewerPage(
                item = item,
                exoPlayer = exoPlayer,
                onTap = { isSystemUiVisible = !isSystemUiVisible }
            )
        }

        // ==================== Top Controls ====================
        AnimatedVisibility(
            visible = isSystemUiVisible,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Quay lai",
                                tint = Color.White
                            )
                        }
                    }

                    // Title
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        val currentItem = items.getOrNull(currentPage)
                        if (currentItem != null) {
                            Text(
                                currentItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${currentPage + 1} / ${items.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Actions
                    ViewerActionButton(
                        icon = if (isSlideShowActive) Icons.Default.Close else Icons.Default.Repeat,
                        tint = if (isSlideShowActive) AppColors.SlideShowActive else Color.White,
                        onClick = {
                            isSlideShowActive = !isSlideShowActive
                            isSystemUiVisible = false
                        }
                    )

                    val isFav = items.getOrNull(currentPage)?.isFavorite == true
                    ViewerActionButton(
                        icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        tint = if (isFav) AppColors.FavoriteActive else Color.White,
                        onClick = { }
                    )

                    ViewerActionButton(
                        icon = Icons.Default.Share,
                        onClick = {
                            val shareItem = items.getOrNull(currentPage) ?: return@ViewerActionButton
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = shareItem.mimeType
                                putExtra(android.content.Intent.EXTRA_STREAM, shareItem.uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Chia se"))
                        }
                    )

                    ViewerActionButton(
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteDialog = true }
                    )

                    ViewerActionButton(
                        icon = Icons.Default.Info,
                        onClick = { showInfoSheet = true }
                    )
                }
            }
        }

        // ==================== Bottom Page Indicator ====================
        AnimatedVisibility(
            visible = isSystemUiVisible && !isSlideShowActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            PageIndicator(
                currentPage = currentPage,
                totalPages = items.size,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Slideshow badge
        AnimatedVisibility(
            visible = isSlideShowActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Surface(
                modifier = Modifier.padding(end = 16.dp, top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = AppColors.SlideShowActive.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Trinh chieu", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(item: AppMediaItem, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text("Thong tin chi tiet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Ten file", value = item.name)
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (item.width > 0 && item.height > 0) {
                InfoRow(label = "Do phan giai", value = "${item.width} x ${item.height} px")
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            InfoRow(label = "Kich thuoc", value = item.formattedSize)
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val dateStr = try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                sdf.format(Date(item.dateAdded * 1000L))
            } catch (e: Exception) { "${item.dateAdded}" }

            InfoRow(label = "Ngay them", value = dateStr)
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (item.isVideo && item.duration > 0) {
                InfoRow(label = "Thoi luong", value = item.formattedDuration)
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            InfoRow(label = "Loai file", value = item.mimeType)
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(label = "Loai media", value = if (item.isVideo) "Video" else "Anh")
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (item.bucketName.isNotEmpty()) {
                InfoRow(label = "Album", value = item.bucketName)
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
}

@Composable
fun PageIndicator(currentPage: Int, totalPages: Int, modifier: Modifier = Modifier) {
    if (totalPages <= 1) return

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val maxDots = 7
                val dotsToShow = if (totalPages <= maxDots) {
                    (0 until totalPages).toList()
                } else {
                    val start = (currentPage - 3).coerceAtLeast(0)
                    val end = (currentPage + 3).coerceAtMost(totalPages - 1)
                    (start..end).toList()
                }

                for (i in dotsToShow) {
                    val isSelected = i == currentPage
                    val dotSize by animateFloatAsState(
                        targetValue = if (isSelected) 10f else 6f,
                        animationSpec = tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }

                if (totalPages > maxDots) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${currentPage + 1}/${totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ViewerPage(
    item: AppMediaItem,
    exoPlayer: ExoPlayer?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item.isVideo && exoPlayer != null) {
        VideoPlayerView(exoPlayer = exoPlayer, onTap = onTap, modifier = modifier.fillMaxSize())
    } else {
        PhotoViewerPage(item = item, onTap = onTap, modifier = modifier)
    }
}

@Composable
fun VideoPlayerView(exoPlayer: ExoPlayer, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnTap = rememberUpdatedState(onTap)
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 3000
                    controllerHideOnTouch = true
                    var startX = 0f
                    var startY = 0f
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> { startX = event.rawX; startY = event.rawY }
                            MotionEvent.ACTION_UP -> {
                                val dx = Math.abs(event.rawX - startX)
                                val dy = Math.abs(event.rawY - startY)
                                if (dx < 10f && dy < 10f) currentOnTap.value()
                            }
                        }
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PhotoViewerPage(item: AppMediaItem, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTap() },
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High
        )

        // Subtle vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val vignetteBrush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                        center = center,
                        radius = size.width * 0.8f
                    )
                    drawRect(brush = vignetteBrush)
                }
        )
    }
}
