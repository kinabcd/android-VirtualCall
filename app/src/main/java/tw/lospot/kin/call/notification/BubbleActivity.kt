package tw.lospot.kin.call.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import tw.lospot.kin.call.screens.BubbleScreen
import tw.lospot.kin.call.ui.theme.ToolkitTheme

class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callId = intent.getIntExtra("callId", -1)
        setContent {
            ToolkitTheme {
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BubbleScreen(callId)
                }
            }
        }
    }
}