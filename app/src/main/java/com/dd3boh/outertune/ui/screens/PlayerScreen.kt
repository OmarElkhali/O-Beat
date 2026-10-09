package com.dd3boh.outertune.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.ui.component.BottomSheetState
import com.dd3boh.outertune.ui.player.BottomSheetPlayer
import com.dd3boh.outertune.ui.player.QueueScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier,
) {
    val playerConnection = LocalPlayerConnection.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        QueueScreen(
            playerBottomSheetState = playerBottomSheetState,
            onTerminate = {
                playerConnection?.service?.queueBoard?.detachedHead = false
            },
            navController = navController
        )
        BottomSheetPlayer(
            state = playerBottomSheetState,
            navController = navController
        )

    }
}








