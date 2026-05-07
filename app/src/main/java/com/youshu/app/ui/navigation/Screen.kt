package com.youshu.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object SearchCenter : Screen("search-center")
    data object Save : Screen("save?imageUri={imageUri}&mode={mode}") {
        fun createRoute(imageUri: String? = null, mode: String = "append"): String {
            val encodedUri = imageUri ?: ""
            return "save?imageUri=$encodedUri&mode=$mode"
        }
    }
    data object Detail : Screen("detail/{itemId}/{scope}") {
        const val ScopeAll = "all"
        const val ScopeUsedUp = "used_up"
        const val ScopePendingReview = "pending_review"
        const val ScopeReviewed = "reviewed"

        fun createRoute(itemId: Long, scope: String = ScopeAll) = "detail/$itemId/$scope"
    }
    data object Search : Screen("search")
    data object Category : Screen("category")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Expiry : Screen("expiry")
    data object Edit : Screen("edit/{itemId}?imageUri={imageUri}&mode={mode}") {
        fun createRoute(itemId: Long, imageUri: String? = null, mode: String = "append"): String {
            val encodedUri = imageUri ?: ""
            return "edit/$itemId?imageUri=$encodedUri&mode=$mode"
        }
    }
}
