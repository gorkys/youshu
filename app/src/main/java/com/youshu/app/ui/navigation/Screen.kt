package com.youshu.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object SearchCenter : Screen("search-center")
    data object Save : Screen("save?imageUri={imageUri}&imageUris={imageUris}&mode={mode}") {
        fun createRoute(
            imageUri: String? = null,
            imageUris: List<String> = emptyList(),
            mode: String = "append"
        ): String {
            val encodedUri = imageUri ?: ""
            val encodedUris = imageUris.joinToString(",") { Uri.encode(it) }
            return "save?imageUri=$encodedUri&imageUris=$encodedUris&mode=$mode"
        }
    }

    data object Detail : Screen("detail/{itemId}/{scope}") {
        const val ScopeAll = "all"
        const val ScopeUsedUp = "used_up"
        const val ScopePendingReview = "pending_review"
        const val ScopeReviewed = "reviewed"

        fun createRoute(itemId: Long, scope: String = ScopeAll): String = "detail/$itemId/$scope"
    }

    data object Search : Screen("search")
    data object Category : Screen("category")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Expiry : Screen("expiry")
    data object Edit : Screen("edit/{itemId}?imageUri={imageUri}&imageUris={imageUris}&mode={mode}") {
        fun createRoute(
            itemId: Long,
            imageUri: String? = null,
            imageUris: List<String> = emptyList(),
            mode: String = "append"
        ): String {
            val encodedUri = imageUri ?: ""
            val encodedUris = imageUris.joinToString(",") { Uri.encode(it) }
            return "edit/$itemId?imageUri=$encodedUri&imageUris=$encodedUris&mode=$mode"
        }
    }
}
