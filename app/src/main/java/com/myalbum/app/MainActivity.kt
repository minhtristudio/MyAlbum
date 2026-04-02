package com.myalbum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.myalbum.app.data.MediaItem
import com.myalbum.app.ui.screens.AlbumMediaScreenWithNavigation
import com.myalbum.app.ui.screens.AlbumListScreen
import com.myalbum.app.ui.screens.FavoritesScreenWithNavigation
import com.myalbum.app.ui.screens.GalleryScreenWithNavigation
import com.myalbum.app.ui.screens.SettingsScreen
import com.myalbum.app.ui.screens.ViewerScreen
import com.myalbum.app.ui.theme.MyAlbumTheme
import kotlinx.coroutines.Dispatchers
import com.myalbum.app.ui.screens.getThemeMode
import com.myalbum.app.ui.screens.getGridSize
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Dark theme state
            val systemIsDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemIsDark) }

            // Grid size state
            var gridSize by remember { mutableIntStateOf(3) }

            // Read saved settings on first composition
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val mode = getThemeMode(applicationContext)
                    val size = getGridSize(applicationContext)
                    withContext(Dispatchers.Main) {
                        darkTheme = when (mode) {
                            "dark" -> true
                            "light" -> false
                            else -> isSystemInDarkTheme()
                        }
                        gridSize = size
                    }
                }
            }

            MyAlbumTheme(darkTheme = darkTheme) {
                val imagePermissionState = rememberPermissionState(
                    if (android.os.Build.VERSION.SDK_INT >= 33)
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    else
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val videoPermissionState = rememberPermissionState(
                    if (android.os.Build.VERSION.SDK_INT >= 33)
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    else
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                )

                var hasPermissions by remember {
                    mutableStateOf(
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            imagePermissionState.status.isGranted && videoPermissionState.status.isGranted
                        } else {
                            imagePermissionState.status.isGranted
                        }
                    )
                }

                LaunchedEffect(imagePermissionState.status, videoPermissionState.status) {
                    hasPermissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        imagePermissionState.status.isGranted && videoPermissionState.status.isGranted
                    } else {
                        imagePermissionState.status.isGranted
                    }
                }

                if (!hasPermissions) {
                    PermissionRequestScreen(
                        onRequestPermissions = {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                imagePermissionState.launchPermissionRequest()
                                videoPermissionState.launchPermissionRequest()
                            } else {
                                imagePermissionState.launchPermissionRequest()
                            }
                        }
                    )
                } else {
                    MainNavigation(
                        gridSize = gridSize,
                        darkTheme = darkTheme,
                        onGridSizeChanged = { newSize -> gridSize = newSize },
                        onDarkThemeChanged = { isDark -> darkTheme = isDark }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0E12),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                        .graphicsLayer { this.alpha = alpha },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "MyAlbum",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Ung dung can quyen truy cap thu vien anh\nva video tren thiet bi cua ban.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cap quyen truy cap",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Color.White.copy(alpha = 0.05f)
                        ),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            "v3.2.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Created by MT Studio",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("gallery", "Thu vien", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary),
    BottomNavItem("album_list", "Album", Icons.Outlined.PhotoAlbum, Icons.Filled.PhotoAlbum),
    BottomNavItem("favorites", "Yeu thich", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    BottomNavItem("settings", "Cai dat", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    gridSize: Int,
    darkTheme: Boolean,
    onGridSizeChanged: (Int) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var galleryItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var albumItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val showBottomBar = currentRoute in listOf(
        "gallery", "album_list", "favorites", "settings"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == item.route) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("gallery") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "gallery",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("gallery") {
                GalleryScreenWithNavigation(
                    navController = navController,
                    onItemsLoaded = { galleryItems = it },
                    gridSize = gridSize
                )
            }

            composable("album_list") {
                AlbumListScreen(
                    onAlbumClick = { bucketId, bucketName ->
                        navController.navigate("album_media/$bucketId/$bucketName")
                    }
                )
            }

            composable(
                route = "album_media/{bucketId}/{bucketName}",
                arguments = listOf(
                    navArgument("bucketId") { type = NavType.StringType },
                    navArgument("bucketName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bucketId = backStackEntry.arguments?.getString("bucketId") ?: return@composable
                val bucketName = backStackEntry.arguments?.getString("bucketName") ?: ""
                AlbumMediaScreenWithNavigation(
                    bucketId = bucketId,
                    bucketName = bucketName,
                    navController = navController,
                    onItemsLoaded = { albumItems = it },
                    gridSize = gridSize
                )
            }

            composable("favorites") {
                FavoritesScreenWithNavigation(
                    navController = navController,
                    onItemsLoaded = { favoriteItems = it },
                    gridSize = gridSize
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.navigateUp() },
                    darkTheme = darkTheme,
                    onDarkThemeChanged = onDarkThemeChanged,
                    gridSize = gridSize,
                    onGridSizeChanged = onGridSizeChanged
                )
            }

            composable(
                route = "viewer/{source}/{index}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("index") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString("source") ?: return@composable
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                val items = when (source) {
                    "gallery" -> galleryItems
                    "album" -> albumItems
                    "favorites" -> favoriteItems
                    else -> emptyList()
                }
                ViewerScreen(
                    items = items,
                    initialIndex = index,
                    onBack = { navController.navigateUp() },
                    onItemDeleted = { deletedIndex ->
                        when (source) {
                            "gallery" -> galleryItems = galleryItems.toMutableList().also {
                                if (deletedIndex < it.size) it.removeAt(deletedIndex)
                            }
                            "album" -> albumItems = albumItems.toMutableList().also {
                                if (deletedIndex < it.size) it.removeAt(deletedIndex)
                            }
                            "favorites" -> favoriteItems = favoriteItems.toMutableList().also {
                                if (deletedIndex < it.size) it.removeAt(deletedIndex)
                            }
                        }
                    }
                )
            }
        }
    }
}
