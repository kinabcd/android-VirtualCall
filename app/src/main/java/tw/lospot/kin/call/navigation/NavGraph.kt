package tw.lospot.kin.call.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import tw.lospot.kin.call.screens.*


@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = MAIN_MENU) {
        composable(MAIN_MENU) { MainMenuScreen(navController) }
        composable(APP_INFO) { AppInfoScreen(navController) }
    }
}