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
import com.myalbum.app.ui.theme.AppColors
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
            val systemIsDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemIsDark) }
            var gridSize by remember { mutableIntStateOf(3) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val mode = getThemeMode(applicationContext)
                    val size = getGridSize(applicationContext)
                    withContext(Dispatchers.Main) {
                        when (mode) {
                            "dark" -> darkTheme = true
                            "light" -> darkTheme = false
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
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08080F),
                        Color(0xFF10101E),
                        Color(0xFF0C1528)
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
            // Premium logo with animated glow
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.GradientStart.copy(alpha = 0.4f),
                                AppColors.GradientMid.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(65.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier
                        .size(68.dp)
                        .scale(scale)
                        .graphicsLayer { this.alpha = alpha },
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "MyAlbum",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Album anh & video dang cap nhat",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Premium button with gradient
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AppColors.GradientStart,
                                    AppColors.GradientEnd
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Text("Cap quyen truy cap", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Branding
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.5f))
                        Text("v4.0.1", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Created by MT Studio", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.35f))
                    Text("Protected by VenCA", style = MaterialTheme.typography.labelSmall, color = AppColors.GradientStart.copy(alpha = 0.5f))
                }
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
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == item.route) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (currentRoute == item.route) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("gallery") { saveState = true }
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
