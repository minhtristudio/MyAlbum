package com.myalbum.app.ui.navigation

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Albums : Screen("albums/{bucketId}/{bucketName}") {
        fun createRoute(bucketId: String, bucketName: String) = "albums/$bucketId/$bucketName"
    }
    data object AlbumList : Screen("album_list")
    data object Favorites : Screen("favorites")
    data object Viewer : Screen("viewer/{mediaIndex}") {
        fun createRoute(mediaIndex: Int) = "viewer/$mediaIndex"
    }
    data object AlbumViewer : Screen("album_viewer/{mediaIndex}/{bucketId}") {
        fun createRoute(mediaIndex: Int, bucketId: String) = "album_viewer/$mediaIndex/$bucketId"
    }
    data object Settings : Screen("settings")
}
