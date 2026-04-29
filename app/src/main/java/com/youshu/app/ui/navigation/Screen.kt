package com.youshu.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object Save : Screen("save/{imageUri}") {
        fun createRoute(imageUri: String) = "save/$imageUri"
    }
    data object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: Long) = "detail/$itemId"
    }
    data object Search : Screen("search")
    data object Category : Screen("category")
    data object Profile : Screen("profile")
    data object Expiry : Screen("expiry")
}
