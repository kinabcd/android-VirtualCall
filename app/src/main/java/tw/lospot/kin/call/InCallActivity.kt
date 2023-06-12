package tw.lospot.kin.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import tw.lospot.kin.call.navigation.SetupNavGraph
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager
import tw.lospot.kin.call.ui.LocalPhoneAccountManager
import tw.lospot.kin.call.ui.theme.ToolkitTheme

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            CompositionLocalProvider(
                LocalPhoneAccountManager provides PhoneAccountManager(context),
            ) {
                ToolkitTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(Modifier.systemBarsPadding()) {
                            SetupNavGraph(navController)
                        }
                    }
                }
            }
        }
    }
}
