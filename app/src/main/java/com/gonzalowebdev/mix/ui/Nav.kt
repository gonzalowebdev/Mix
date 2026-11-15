package com.gonzalowebdev.mix.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MixRoot(vm: MixViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                vm = vm,
                onPlay = { nav.navigate("player") },
                onPlaylists = { nav.navigate("playlists") }
            )
        }
        composable("playlists") {
            PlaylistsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate("playlist/$id") }
            )
        }
        composable(
            route = "playlist/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            PlaylistDetailScreen(
                vm = vm,
                playlistId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
                onPlay = { nav.navigate("player") }
            )
        }
        composable("player") {
            PlayerScreen(vm = vm, onClose = { nav.popBackStack() })
        }
    }
}
