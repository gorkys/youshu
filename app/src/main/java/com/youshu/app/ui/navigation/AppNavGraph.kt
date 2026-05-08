package com.youshu.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.youshu.app.ui.components.GlassPanel
import com.youshu.app.ui.components.appGlassStyle
import com.youshu.app.ui.screen.camera.CameraScreen
import com.youshu.app.ui.screen.category.CategoryScreen
import com.youshu.app.ui.screen.detail.DetailScreen
import com.youshu.app.ui.screen.edit.EditScreen
import com.youshu.app.ui.screen.expiry.ExpiryScreen
import com.youshu.app.ui.screen.home.HomeScreen
import com.youshu.app.ui.screen.profile.ProfileScreen
import com.youshu.app.ui.screen.save.SavePhotoMode
import com.youshu.app.ui.screen.save.SaveScreen
import com.youshu.app.ui.screen.search.SearchScreen
import com.youshu.app.ui.screen.settings.SettingsScreen
import com.youshu.app.ui.screen.trash.TrashScreen
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.viewmodel.LibraryStatusFilter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Category.route, "分类", Icons.Filled.Category, Icons.Outlined.Category),
    BottomNavItem(Screen.Search.route, "库房", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.Profile.route, "我的", Icons.Filled.Person, Icons.Outlined.Person)
)

internal const val EDIT_RESULT_IMAGE_URI = "edit_result_image_uri"
internal const val EDIT_RESULT_IMAGE_URIS = "edit_result_image_uris"
internal const val EDIT_RESULT_MODE = "edit_result_mode"

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var cameraExitGuard by remember { mutableStateOf(false) }
    var pendingCameraReturn by remember { mutableStateOf<CameraReturnTarget?>(null) }
    val hazeState = remember { HazeState() }

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openCamera: (CameraReturnTarget) -> Unit = { target ->
        pendingCameraReturn = target
        navController.navigate(Screen.Camera.route)
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Camera.route) {
            cameraExitGuard = true
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Category.route,
        Screen.Search.route,
        Screen.Profile.route
    ) && !cameraExitGuard

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.Edit.createRoute(id)) },
                        onNavigateToLibrary = { navigateToTopLevel(Screen.Search.route) },
                        onNavigateToSearchCenter = { navController.navigate(Screen.SearchCenter.route) },
                        onNavigateToExpiry = { navController.navigate(Screen.Expiry.route) }
                    )
                }

                composable(Screen.Camera.route) {
                    CameraScreen(
                        onBack = { navController.popBackStack() },
                        onDisposed = { cameraExitGuard = false },
                        onSkipPhoto = {
                            when (val target = pendingCameraReturn) {
                                CameraReturnTarget.NewSave,
                                is CameraReturnTarget.SaveDraft,
                                null -> {
                                    navController.navigate(Screen.Save.createRoute()) {
                                        popUpTo(Screen.Camera.route) { inclusive = true }
                                    }
                                }

                                is CameraReturnTarget.EditItem -> {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(EDIT_RESULT_MODE, target.mode.name.lowercase())
                                    navController.popBackStack()
                                }
                            }
                            pendingCameraReturn = null
                        },
                        onPhotoTaken = { uris ->
                            val encoded = uris.map { it.toString() }
                            val firstUri = encoded.firstOrNull()
                            when (val target = pendingCameraReturn) {
                                CameraReturnTarget.NewSave -> {
                                    navController.navigate(Screen.Save.createRoute(firstUri, encoded)) {
                                        popUpTo(Screen.Camera.route) { inclusive = true }
                                    }
                                }

                                is CameraReturnTarget.EditItem -> {
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set(EDIT_RESULT_IMAGE_URI, firstUri)
                                        set(EDIT_RESULT_IMAGE_URIS, encoded)
                                        set(EDIT_RESULT_MODE, target.mode.name.lowercase())
                                    }
                                    navController.popBackStack()
                                }

                                is CameraReturnTarget.SaveDraft -> {
                                    navController.navigate(
                                        Screen.Save.createRoute(
                                            imageUri = firstUri,
                                            imageUris = encoded,
                                            mode = target.mode.name.lowercase()
                                        )
                                    ) {
                                        popUpTo(Screen.Camera.route) { inclusive = true }
                                    }
                                }

                                null -> {
                                    navController.navigate(Screen.Save.createRoute(firstUri, encoded)) {
                                        popUpTo(Screen.Camera.route) { inclusive = true }
                                    }
                                }
                            }
                            pendingCameraReturn = null
                        }
                    )
                }

                composable(Screen.SearchCenter.route) {
                    SearchCenterScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLibrary = { navigateToTopLevel(Screen.Search.route) },
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                    )
                }

                composable(
                    route = Screen.Save.route,
                    arguments = listOf(
                        navArgument("imageUri") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        },
                        navArgument("imageUris") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        },
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = SavePhotoMode.APPEND.name.lowercase()
                        }
                    )
                ) { backStackEntry ->
                    val encodedUri = backStackEntry.arguments?.getString("imageUri").orEmpty()
                    val encodedUris = backStackEntry.arguments?.getString("imageUris").orEmpty()
                    val mode = backStackEntry.arguments?.getString("mode").orEmpty()
                    val uri = encodedUri.takeIf { it.isNotBlank() }?.let { Uri.parse(Uri.decode(it)) }
                    val uris = encodedUris
                        .split(",")
                        .mapNotNull { value ->
                            value.takeIf { it.isNotBlank() }?.let { Uri.parse(Uri.decode(it)) }
                        }
                    SaveScreen(
                        imageUri = uri,
                        pendingImageUris = uris,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onOpenCamera = { photoMode ->
                            openCamera(CameraReturnTarget.SaveDraft(photoMode))
                        }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(
                        navArgument("itemId") { type = NavType.LongType },
                        navArgument("scope") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                    val scope = backStackEntry.arguments?.getString("scope") ?: Screen.Detail.ScopeAll
                    DetailScreen(
                        itemId = itemId,
                        scope = scope,
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Screen.Edit.createRoute(id)) }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToDetail = { id, filter ->
                            navController.navigate(Screen.Detail.createRoute(id, filter.toDetailScope()))
                        },
                        onNavigateToEdit = { id -> navController.navigate(Screen.Edit.createRoute(id)) }
                    )
                }

                composable(Screen.Category.route) {
                    CategoryScreen(
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.Edit.createRoute(id)) }
                    )
                }

                composable(Screen.Expiry.route) {
                    ExpiryScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                    )
                }

                composable(
                    route = Screen.Edit.route,
                    arguments = listOf(
                        navArgument("itemId") { type = NavType.LongType },
                        navArgument("imageUri") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        },
                        navArgument("imageUris") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        },
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = SavePhotoMode.APPEND.name.lowercase()
                        }
                    )
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                    val encodedUri = backStackEntry.arguments?.getString("imageUri").orEmpty()
                    val encodedUris = backStackEntry.arguments?.getString("imageUris").orEmpty()
                    val mode = backStackEntry.arguments?.getString("mode").orEmpty()
                    val resultImageUri by backStackEntry.savedStateHandle
                        .getStateFlow<String?>(EDIT_RESULT_IMAGE_URI, null)
                        .collectAsState()
                    val resultImageUris by backStackEntry.savedStateHandle
                        .getStateFlow(EDIT_RESULT_IMAGE_URIS, emptyList<String>())
                        .collectAsState()
                    val resultMode by backStackEntry.savedStateHandle
                        .getStateFlow<String?>(EDIT_RESULT_MODE, null)
                        .collectAsState()
                    val uri = encodedUri.takeIf { it.isNotBlank() }?.let { Uri.parse(Uri.decode(it)) }
                    val uris = encodedUris
                        .split(",")
                        .mapNotNull { value ->
                            value.takeIf { it.isNotBlank() }?.let { Uri.parse(Uri.decode(it)) }
                        }
                    EditScreen(
                        itemId = itemId,
                        pendingImageUri = uri,
                        pendingImageUris = uris,
                        pendingPhotoMode = mode.toSavePhotoMode(),
                        resultImageUri = resultImageUri?.let(Uri::parse),
                        resultImageUris = resultImageUris.map(Uri::parse),
                        resultPhotoMode = resultMode?.toSavePhotoMode(),
                        onConsumePendingResult = {
                            backStackEntry.savedStateHandle.remove<String>(EDIT_RESULT_IMAGE_URI)
                            backStackEntry.savedStateHandle.remove<List<String>>(EDIT_RESULT_IMAGE_URIS)
                            backStackEntry.savedStateHandle.remove<String>(EDIT_RESULT_MODE)
                        },
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onOpenCamera = { photoMode ->
                            openCamera(CameraReturnTarget.EditItem(itemId, photoMode))
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onOpenExpiry = { navController.navigate(Screen.Expiry.route) },
                        onOpenTrash = { navController.navigate(Screen.Trash.route) },
                        onOpenSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Trash.route) {
                    TrashScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showBottomBar) {
            YouShuBottomBar(
                currentRoute = currentRoute,
                onNavigate = navigateToTopLevel,
                onCameraClick = { openCamera(CameraReturnTarget.NewSave) },
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private fun LibraryStatusFilter.toDetailScope(): String {
    return when (this) {
        LibraryStatusFilter.ALL -> Screen.Detail.ScopeAll
        LibraryStatusFilter.USED_UP -> Screen.Detail.ScopeUsedUp
        LibraryStatusFilter.PENDING_REVIEW -> Screen.Detail.ScopePendingReview
        LibraryStatusFilter.REVIEWED -> Screen.Detail.ScopeReviewed
    }
}

private fun String.toSavePhotoMode(): SavePhotoMode {
    return when (lowercase()) {
        SavePhotoMode.REPLACE_PRIMARY.name.lowercase() -> SavePhotoMode.REPLACE_PRIMARY
        else -> SavePhotoMode.APPEND
    }
}

private sealed interface CameraReturnTarget {
    data object NewSave : CameraReturnTarget
    data class SaveDraft(val mode: SavePhotoMode) : CameraReturnTarget
    data class EditItem(val itemId: Long, val mode: SavePhotoMode) : CameraReturnTarget
}

@Composable
private fun YouShuBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCameraClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp),
            hazeState = hazeState,
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            containerColor = Color.White.copy(alpha = 0.16f),
            borderColor = Color.White.copy(alpha = 0.72f),
            shadowElevation = 20.dp,
            blurAlpha = 0.6f
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.take(2).forEach { item ->
                    BottomItem(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Box(modifier = Modifier.size(70.dp))
                bottomNavItems.takeLast(2).forEach { item ->
                    BottomItem(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 15.dp)
                .size(width = 126.dp, height = 34.dp)
                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                .background(Color.White.copy(alpha = 0f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(72.dp)
                .clip(CircleShape)
                .hazeEffect(
                    state = hazeState,
                    style = appGlassStyle(blurAlpha = 0.6f)
                )
                .background(Color.White.copy(alpha = 0.04f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.68f), shape = CircleShape)
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
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
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) OrangeStart else TextHint
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = item.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
