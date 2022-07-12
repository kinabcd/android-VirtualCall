package tw.lospot.kin.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.primarySurface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import tw.lospot.kin.call.navigation.SetupNavGraph
import tw.lospot.kin.call.ui.theme.ToolkitTheme

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            ToolkitTheme {
                val colors by rememberUpdatedState(MaterialTheme.colors)
                val windowInsetsController =
                    remember { WindowInsetsControllerCompat(window, window.peekDecorView()) }
                SideEffect {
                    window.statusBarColor = colors.primarySurface.toArgb()
                    window.navigationBarColor = colors.background.toArgb()
                    windowInsetsController.apply {
                        isAppearanceLightStatusBars = colors.isLight
                        isAppearanceLightNavigationBars = colors.isLight
                    }
                }
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    SetupNavGraph(navController)
                }
            }
        }
    }
}
