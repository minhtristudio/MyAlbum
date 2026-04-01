package com.myalbum.app.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

suspend fun saveGridSize(context: android.content.Context, size: Int) {
    context.settingsDataStore.edit { prefs ->
        prefs[intPreferencesKey("grid_size")] = size
    }
}

suspend fun getGridSize(context: android.content.Context): Int {
    return context.settingsDataStore.data.map { prefs ->
        prefs[intPreferencesKey("grid_size")] ?: 3
    }.first()
}

suspend fun saveThemeMode(context: android.content.Context, mode: String) {
    context.settingsDataStore.edit { prefs ->
        prefs[stringPreferencesKey("theme_mode")] = mode
    }
}

suspend fun getThemeMode(context: android.content.Context): String {
    return context.settingsDataStore.data.map { prefs ->
        prefs[stringPreferencesKey("theme_mode")] ?: "system"
    }.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pm = context.packageManager
    val packageInfo = remember {
        try {
            pm.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }

    var darkModeEnabled by remember { mutableStateOf(false) }
    var gridSize by remember { mutableIntStateOf(3) }
    var cacheCleared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val themeMode = getThemeMode(context)
        darkModeEnabled = themeMode == "dark"

        val savedGridSize = getGridSize(context)
        gridSize = savedGridSize
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Appearance section
            SectionHeader("Giao diện")

            // Dark mode toggle
            SettingSwitchItem(
                icon = Icons.Default.Brightness6,
                title = "Chế độ tối",
                subtitle = if (darkModeEnabled) "Đang bật" else "Đang tắt",
                checked = darkModeEnabled,
                onCheckedChange = { checked ->
                    darkModeEnabled = checked
                    scope.launch {
                        saveThemeMode(
                            context,
                            if (checked) "dark" else "light"
                        )
                        AppCompatDelegate.setDefaultNightMode(
                            if (checked) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    }
                }
            )

            // Grid size selector
            SettingItem(
                icon = Icons.Default.GridView,
                title = "Số cột hiển thị",
                subtitle = "$gridSize cột",
                onClick = null
            )

            // Grid size radio buttons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    listOf(2, 3, 4, 5).forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (gridSize == size),
                                    onClick = {
                                        gridSize = size
                                        scope.launch {
                                            saveGridSize(context, size)
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (gridSize == size),
                                onClick = null,
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "$size cột",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // Preview mini grid
                            MiniGridPreview(columns = size)
                        }
                    }
                }
            }

            SectionHeader("Bộ nhớ")

            // Clear cache
            SettingItem(
                icon = Icons.Default.DeleteSweep,
                title = "Xóa bộ nhớ cache",
                subtitle = if (cacheCleared) "Đã xóa thành công!" else "Giải phóng không gian ảnh",
                onClick = {
                    try {
                        Coil.imageLoader(context).memoryCache?.clear()
                        cacheCleared = true
                    } catch (_: Exception) {
                    }
                }
            )

            SettingItem(
                icon = Icons.Default.Storage,
                title = "Quản lý bộ nhớ",
                subtitle = "Xem dung lượng media"
            )

            SectionHeader("Giới thiệu")

            SettingItem(
                icon = Icons.Default.Info,
                title = "Phiên bản",
                subtitle = packageInfo?.versionName ?: "1.0.0"
            )

            SettingItem(
                icon = Icons.Default.Android,
                title = "Tên ứng dụng",
                subtitle = "MyAlbum - Quản lý Ảnh & Video"
            )

            SettingItem(
                icon = Icons.Default.Code,
                title = "Android yêu cầu",
                subtitle = "Android 8.0+ (API 26+)"
            )

            SettingItem(
                icon = Icons.Default.Style,
                title = "Thiết kế",
                subtitle = "Material You (M3) - Jetpack Compose"
            )

            SectionHeader("Quyền")

            SettingItem(
                icon = Icons.Default.Security,
                title = "Quyền truy cập",
                subtitle = "Quản lý quyền ứng dụng"
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Footer
            Text(
                "MyAlbum v${packageInfo?.versionName ?: "1.0.0"}\nXây dựng với ❤️ bằng Kotlin & Jetpack Compose",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
fun MiniGridPreview(columns: Int) {
    val dotSize = 4.dp
    val spacing = 2.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(columns) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
