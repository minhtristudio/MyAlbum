package com.myalbum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.*
import com.myalbum.app.data.MediaItem
import com.myalbum.app.ui.screens.*
import com.myalbum.app.ui.theme.MyAlbumTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAlbumTheme {
                // Permission handling
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
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "MyAlbum cần quyền truy cập",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ứng dụng cần quyền truy cập thư viện ảnh và video trên thiết bị của bạn để hiển thị và quản lý media.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cấp quyền truy cập")
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Gallery : BottomNavItem("gallery", "Thư viện", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary)
    data object Albums : BottomNavItem("album_list", "Album", Icons.Outlined.PhotoAlbum, Icons.Filled.PhotoAlbum)
    data object Favorites : BottomNavItem("favorites", "Yêu thích", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    data object Settings : BottomNavItem("settings", "Cài đặt", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Track media items for viewer navigation
    var galleryItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var albumItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val showBottomBar = currentRoute in listOf(
        "gallery", "album_list", "favorites", "settings"
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == item.route) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(BottomNavItem.Gallery.route) {
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "gallery",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("gallery") {
                GalleryScreenWithNavigation(
                    navController = navController,
                    onItemsLoaded = { galleryItems = it }
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
                    onItemsLoaded = { albumItems = it }
                )
            }

            composable("favorites") {
                FavoritesScreenWithNavigation(
                    navController = navController,
                    onItemsLoaded = { favoriteItems = it }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.navigateUp() }
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
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
}
