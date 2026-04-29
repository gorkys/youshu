package com.youshu.app.ui.navigation

import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.youshu.app.ui.screen.camera.CameraScreen
import com.youshu.app.ui.screen.category.CategoryScreen
import com.youshu.app.ui.screen.detail.DetailScreen
import com.youshu.app.ui.screen.edit.EditScreen
import com.youshu.app.ui.screen.expiry.ExpiryScreen
import com.youshu.app.ui.screen.home.HomeScreen
import com.youshu.app.ui.screen.profile.ProfileScreen
import com.youshu.app.ui.screen.save.SaveScreen
import com.youshu.app.ui.screen.search.SearchScreen
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Category.route, "分类", Icons.Filled.Category, Icons.Outlined.Category),
    BottomNavItem(Screen.Camera.route, "拍照", Icons.Filled.CameraAlt, Icons.Filled.CameraAlt),
    BottomNavItem(Screen.Search.route, "搜索", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.Profile.route, "我的", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Category.route,
        Screen.Search.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                YouShuBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.Camera.route) {
                            navController.navigate(Screen.Camera.route)
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToExpiry = { navController.navigate(Screen.Expiry.route) }
                )
            }

            composable(Screen.Camera.route) {
                CameraScreen(
                    onBack = { navController.popBackStack() },
                    onPhotoTaken = { uri ->
                        val encoded = Uri.encode(uri.toString())
                        navController.navigate(Screen.Save.createRoute(encoded)) {
                            popUpTo(Screen.Camera.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Save.route,
                arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
                val uri = Uri.parse(Uri.decode(encodedUri))
                SaveScreen(
                    imageUri = uri,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                DetailScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.Edit.createRoute(id)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            composable(Screen.Category.route) {
                CategoryScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            composable(Screen.Expiry.route) {
                ExpiryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Edit.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                EditScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun YouShuBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Frosted glass effect: blurred white background behind the nav bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.65f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }

        NavigationBar(
            containerColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Color.Transparent
            else
                Color.White.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                if (index == 2) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Box(modifier = Modifier.height(28.dp)) },
                        label = { },
                        enabled = false,
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Transparent,
                            unselectedTextColor = Color.Transparent,
                            indicatorColor = Color.Transparent
                        )
                    )
                } else {
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(item.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangeStart,
                            selectedTextColor = OrangeStart,
                            unselectedIconColor = Color(0xFF8C8C8C),
                            unselectedTextColor = Color(0xFF8C8C8C),
                            indicatorColor = OrangeStart.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }

        // Floating center camera button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Color.White, CircleShape)
                .clickable { onNavigate(Screen.Camera.route) },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(OrangeStart, OrangeEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
