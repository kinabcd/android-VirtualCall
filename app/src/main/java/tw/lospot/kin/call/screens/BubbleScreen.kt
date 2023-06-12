package tw.lospot.kin.call.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.ui.CallInfoNoDecorate

@Composable
fun BubbleScreen(callId: Int) {
    val calls by CallList.calls.collectAsStateWithLifecycle()
    val call = calls.firstOrNull { it.id == callId } ?: return
    CallInfoNoDecorate(call = call)
}