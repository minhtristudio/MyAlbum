---
Task ID: 1
Agent: Super Z (Main)
Task: Fix gap, dark mode, grid size, photo swipe, replace emojis, upgrade UI for MyAlbum v3.1.0

Work Log:
- Cloned repo and read all source files
- Fixed gap between top bar and content: Added `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to all inner Scaffolds (GalleryScreen, AlbumsScreen, SettingsScreen, FavoritesScreen, AlbumMediaScreen)
- Fixed dark mode not working: Read theme mode from DataStore in MainActivity via LaunchedEffect, pass `darkTheme` state directly to `MyAlbumTheme` and `SettingsScreen` via callbacks - no longer relies on AppCompatDelegate
- Fixed grid size not applying: Read grid size from DataStore in MainActivity, pass `gridSize` to all gallery screens (GalleryScreen, AlbumMediaScreen, FavoritesScreen)
- Fixed photo swipe: Removed `transformable` modifier from `PhotoViewerPage` that was blocking HorizontalPager gestures. Replaced with `combinedClickable` for tap and double-tap zoom
- Replaced all emojis with Material Icons in StatsBar (📸→Icons.Outlined.Image, 🎬→Icons.Outlined.Videocam, 📁→Icons.Outlined.Folder)
- Changed title style from `headlineMedium` to `titleLarge` for better TopAppBar fit
- Updated SettingsScreen to accept `darkTheme`, `onDarkThemeChanged`, `gridSize`, `onGridSizeChanged` parameters
- Updated version to 3.1.0
- Committed, pushed, waited for GitHub Actions build (success)
- Downloaded signed APK and uploaded to GitHub Release v3.1.0

Stage Summary:
- Release: https://github.com/minhtristudio/MyAlbum/releases/download/v3.1.0/MyAlbum-v3.1.0-signed-release.apk
- All 5 issues fixed: gap, dark mode, grid size, photo swipe, emojis
- APK size: 8.8MB signed release
