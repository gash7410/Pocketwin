package com.pocketwin.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketwin.launcher.ui.PocketWinViewModel
import com.pocketwin.launcher.ui.screens.ContainerDetailScreen
import com.pocketwin.launcher.ui.screens.ContainerListScreen
import com.pocketwin.launcher.ui.theme.PocketWinTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PocketWinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PocketWinTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PocketWinNavHost(viewModel)
                }
            }
        }
    }
}

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{containerId}"

@Composable
private fun PocketWinNavHost(viewModel: PocketWinViewModel) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            ContainerListScreen(
                viewModel = viewModel,
                onOpenContainer = { container -> navController.navigate("detail/${container.id}") },
            )
        }
        composable(ROUTE_DETAIL) { backStackEntry ->
            val containerId = backStackEntry.arguments?.getString("containerId")
            ContainerDetailScreen(
                viewModel = viewModel,
                containerId = containerId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
