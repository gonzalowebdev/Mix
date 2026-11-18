package com.gonzalowebdev.mix.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/** Espacio que dejan las pantallas abajo para no quedar tapadas por la barra flotante. */
val LocalNavInset = compositionLocalOf<Dp> { 0.dp }

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MixRoot(
    externalUri: Uri?,
    onExternalHandled: () -> Unit,
    vm: MixViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = remember {
        listOf(
            Tab("library", "Inicio", MixIcons.Mosaic),
            Tab("memories", "Recuerdos", MixIcons.AutoAwesome),
            Tab("playlists", "Listas", MixIcons.QueueMusic),
            Tab("folders", "Carpetas", MixIcons.Folder)
        )
    }
    val showNav = tabs.any { it.route == route }

    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            vm.playExternal(externalUri)
            nav.popBackStack("library", false)
            nav.navigate("player")
            onExternalHandled()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalNavInset provides (if (showNav) 96.dp else 0.dp)) {
            NavHost(
                navController = nav,
                startDestination = "library",
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable("library") {
                    LibraryScreen(
                        vm = vm,
                        onPlay = { nav.navigate("player") },
                        onFolders = { goTab(nav, "folders") }
                    )
                }
                composable("memories") {
                    MemoriesScreen(vm = vm, onPlay = { nav.navigate("player") })
                }
                composable("playlists") {
                    PlaylistsScreen(
                        vm = vm,
                        onOpen = { id -> nav.navigate("playlist/$id") },
                        onPlay = { nav.navigate("player") }
                    )
                }
                composable("folders") {
                    FoldersScreen(vm = vm)
                }
                composable(
                    route = "playlist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { e ->
                    PlaylistDetailScreen(
                        vm = vm,
                        playlistId = e.arguments?.getLong("id") ?: 0L,
                        onBack = { nav.popBackStack() },
                        onPlay = { nav.navigate("player") }
                    )
                }
                composable("player") {
                    PlayerScreen(vm = vm, onClose = { nav.popBackStack() })
                }
            }
        }

        AnimatedVisibility(
            visible = showNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(160)) { it } + fadeOut(tween(160))
        ) {
            FloatingNav(tabs = tabs, current = route) { tab ->
                if (tab.route == "library") vm.closeSearch()
                goTab(nav, tab.route)
            }
        }
    }
}

private fun goTab(nav: androidx.navigation.NavController, route: String) {
    nav.navigate(route) {
        popUpTo("library") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FloatingNav(tabs: List<Tab>, current: String?, onSelect: (Tab) -> Unit) {
    val pink = MixColors.Pink
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(32.dp)
        Row(
            Modifier
                .clip(shape)
                .background(Color(0xF2141417))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == current
                val bg by animateColorAsState(
                    targetValue = if (selected) pink.copy(alpha = 0.16f) else Color.Transparent,
                    label = "navBg"
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(bg)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) pink else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    AnimatedVisibility(visible = selected) {
                        Text(
                            text = tab.label,
                            color = pink,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
