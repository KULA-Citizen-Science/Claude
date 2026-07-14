package com.kula.shadowroutines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kula.shadowroutines.ui.AppViewModel
import com.kula.shadowroutines.ui.flip.FlipRewardHost
import com.kula.shadowroutines.ui.home.HomeScreen
import com.kula.shadowroutines.ui.routine.RoutineScreen
import com.kula.shadowroutines.ui.theme.ShadowRoutinesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShadowRoutinesTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val reward by viewModel.reward.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        // The flip host wraps the whole navigation graph (front face) and the reward (back).
        FlipRewardHost(
            reward = reward,
            onDismiss = viewModel::dismissReward,
            onToggleFavorite = viewModel::toggleFavorite,
        ) {
            AppNavHost(viewModel)
        }

        // Snackbars live outside the flip so import/rescan messages stay upright and legible.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AppNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenRoutine = { id -> navController.navigate("routine/$id") },
            )
        }
        composable(
            route = "routine/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: return@composable
            RoutineScreen(
                viewModel = viewModel,
                routineId = routineId,
                onBack = { navController.popBackStack() },
                onCompleted = { navController.popBackStack() },
            )
        }
    }
}
