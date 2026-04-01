package com.myalbum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAlbumTheme {
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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
                textAlign = TextAlign.Center
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

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("gallery", "Thư viện", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary),
    BottomNavItem("album_list", "Album", Icons.Outlined.PhotoAlbum, Icons.Filled.PhotoAlbum),
    BottomNavItem("favorites", "Yêu thích", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    BottomNavItem("settings", "Cài đặt", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
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
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
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
