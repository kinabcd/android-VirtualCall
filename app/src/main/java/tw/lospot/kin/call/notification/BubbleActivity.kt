package tw.lospot.kin.call.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import tw.lospot.kin.call.screens.BubbleScreen

class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callId = intent.getIntExtra("callId", -1)
        setContent {
            BubbleScreen(callId)
        }
    }
}