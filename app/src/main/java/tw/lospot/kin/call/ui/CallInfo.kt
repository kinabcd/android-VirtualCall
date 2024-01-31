package tw.lospot.kin.call.ui

import android.os.Build
import android.telecom.Connection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.CallSnapshot

@Composable
fun CallInfo(
    call: CallSnapshot,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .then(modifier)
    ) {
        InfoCard(modifier = Modifier.fillMaxWidth()) {
            CallInfoNoDecorate(call)
        }
    }
}

@Composable
fun CallInfoNoDecorate(call: CallSnapshot) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = call.name, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        CallAction(call)
        call.children.forEach {
            Spacer(modifier = Modifier.height(8.dp))
            CallChildInfo(call = it)
        }
    }

}

@Composable
fun CallAction(call: CallSnapshot) {
    Row {
        if (call.state == Connection.STATE_DIALING) {
            IconButton(
                painter = painterResource(R.drawable.ic_answer_call),
                size = 36.dp,
                onClick = { call.answer() },
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(
            painter = painterResource(R.drawable.ic_end_call),
            size = 36.dp,
            onClick = { call.disconnect() },
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (Build.VERSION.SDK_INT >= 25) {
            if (call.isExternal) {
                IconButton(
                    painter = painterResource(R.drawable.ic_download),
                    size = 36.dp,
                    onClick = { call.pull() },
                )
            } else {
                IconButton(
                    painter = painterResource(R.drawable.ic_upload),
                    size = 36.dp,
                    onClick = { call.push() },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(
            painter = painterResource(R.drawable.ic_videocam),
            size = 36.dp,
            onClick = { call.toggleRxVideo() },
        )
    }
}

@Composable
fun CallChildInfo(call: CallSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        Text(
            text = call.name,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        IconButton(
            painter = painterResource(R.drawable.ic_end_call),
            size = 36.dp,
            onClick = { call.disconnect() },
        )
    }
}