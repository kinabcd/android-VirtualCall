package tw.lospot.kin.call.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import tw.lospot.kin.call.InCallController
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.ui.CallInfoNoDecorate
import tw.lospot.kin.call.ui.OnLifecycleEvent

@Composable
fun BubbleScreen(callId: Int) {
    val rawCall = remember { CallList.getAllCalls().first { it.id == callId } }
    val wrappedCall = remember { InCallController.Call(rawCall) }
    val listener = remember {
        object : Call.Listener {
            override fun onCallStateChanged(call: Call, newState: Int) {
                wrappedCall.updateData()
            }
        }
    }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                rawCall.addListener(listener)
                wrappedCall.updateData()
            }
            Lifecycle.Event.ON_STOP -> {
                rawCall.removeListener(listener)
            }
            else -> {}
        }
    }
    CallInfoNoDecorate(call = wrappedCall)
}