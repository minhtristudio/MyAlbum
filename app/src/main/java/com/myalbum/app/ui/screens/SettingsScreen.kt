package com.myalbum.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageInfo = remember {
        try { pm.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            // About section
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

            // Display section
            SectionHeader("Hiển thị")

            SettingItem(
                icon = Icons.Default.GridView,
                title = "Cột hiển thị",
                subtitle = "3 cột"
            )

            SettingItem(
                icon = Icons.Default.Style,
                title = "Giao diện",
                subtitle = "Material You (M3)"
            )

            // Storage section
            SectionHeader("Bộ nhớ")

            SettingItem(
                icon = Icons.Default.Storage,
                title = "Quản lý bộ nhớ",
                subtitle = "Xem dung lượng media"
            )

            SettingItem(
                icon = Icons.Default.Cached,
                title = "Xóa bộ nhớ cache",
                subtitle = "Giải phóng không gian"
            )

            // Permissions section
            SectionHeader("Quyền")

            SettingItem(
                icon = Icons.Default.Security,
                title = "Quyền truy cập",
                subtitle = "Quản lý quyền ứng dụng"
            )

            Spacer(modifier = Modifier.height(32.dp))
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

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
