package tw.lospot.kin.call.ui

import android.content.Intent
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import tw.lospot.kin.call.AccountViewModel
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.CallParameters
import tw.lospot.kin.call.screens.AccountModel

private const val TELECOM_PACKAGE_NAME = "com.android.server.telecom"
private const val ENABLE_ACCOUNT_PREFERENCE =
    "com.android.server.telecom.settings.EnableAccountPreferenceActivity"

@Composable
fun AccountInfo(account: AccountModel) {
    val context = LocalContext.current
    val phoneAccountManager = LocalPhoneAccountManager.current
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.CenterVertically)
                .clickable {
                    val snapshot = phoneAccountManager.register(account.meta.id)
                    if (!snapshot.isSelfManaged) {
                        val intent = Intent()
                            .setClassName(TELECOM_PACKAGE_NAME, ENABLE_ACCOUNT_PREFERENCE)
                        if (context.packageManager.queryIntentActivities(intent, 0).size > 0) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
                        }
                    }
                },
            onDraw = {
                when {
                    !account.meta.isRegistered -> drawCircle(color = Color.Gray)
                    !account.meta.isEnabled -> drawCircle(color = Color.Red)
                    else -> drawCircle(color = Color.Green)
                }
                drawCircle(color = Color.White, style = Stroke(width = 2.dp.toPx()))
            })
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            text = account.meta.id
        )
    }
}

@Composable
fun AccountEditAction(account: AccountModel) {
    val phoneAccountManager = LocalPhoneAccountManager.current
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
    ) {
        IconButton(
            painter = painterResource(id = R.drawable.ic_block),
            contentDescription = null,
            onClick = { phoneAccountManager.unregister(account.meta.id) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            painter = rememberVectorPainter(image = Icons.Default.Delete),
            contentDescription = null,
            onClick = {
                phoneAccountManager.unregister(account.meta.id)
                phoneAccountManager.remove(account.meta.id)
            }
        )
    }

}

@Composable
fun AccountAddCallAction(
    account: AccountModel,
    vm: AccountViewModel = viewModel(AccountViewModel::class.java, key = account.meta.id)
) {
    val phoneAccountManager = LocalPhoneAccountManager.current
    val dialer = remember(phoneAccountManager) { phoneAccountManager.createDialer() }
    val context = LocalContext.current
    val pref = remember {
        context.getSharedPreferences("Connection", ComponentActivity.MODE_PRIVATE)
    }
    val onNumberChange: (String) -> Unit = remember {
        {
            vm.number = it
            pref.edit { putString("last_number", it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        InfoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                        label = { Text("Number") }, value = vm.number,
                        onValueChange = onNumberChange,
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        painter = painterResource(id = R.drawable.incall_audio_outgoing),
                        contentDescription = null,
                        size = 48.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            phoneAccountManager.phoneAccountFor(account.meta.id)?.let {
                                dialer.addOutgoingCall(it, vm.number, vm.para)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        painter = painterResource(id = R.drawable.incall_audio_incoming),
                        contentDescription = null,
                        size = 48.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            phoneAccountManager.phoneAccountFor(account.meta.id)?.let {
                                dialer.addIncomingCall(it, vm.number, vm.para)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterVertically)
                            .clickable {
                                vm.expand = !vm.expand
                            },
                        painter = painterResource(
                            if (!vm.expand)
                                R.drawable.ic_expand_more else R.drawable.ic_expand_less
                        ),
                        contentDescription = null,
                    )
                }
                if (vm.expand) {
                    Row {
                        TextButton(
                            onClick = { onNumberChange("hidden") },
                        ) {
                            Text(text = "Private", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { onNumberChange("unknown") },
                        ) {
                            Text(text = "Unknown", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { onNumberChange("payphone") },
                        ) {
                            Text(text = "Payphone", fontSize = 12.sp)
                        }
                    }
                    AccountAddCallActionDetail(vm.para) { vm.para = it }
                }
            }
        }
    }
}

@Composable
fun AccountAddCallActionDetail(para: CallParameters, onChanged: (para: CallParameters) -> Unit) {
    Column {
        Row {
            Button(
                modifier = Modifier.size(36.dp),
                colors = if (para.isWifi) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                contentPadding = PaddingValues(4.dp),
                onClick = { onChanged(para.copy(isWifi = !para.isWifi)) }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_wifi), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                modifier = Modifier.size(36.dp),
                colors = if (para.isHdAudio) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                contentPadding = PaddingValues(4.dp),
                onClick = { onChanged(para.copy(isHdAudio = !para.isHdAudio)) }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_hd), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                modifier = Modifier.size(36.dp),
                colors = if (para.videoState and VideoProfile.STATE_TX_ENABLED != 0) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                contentPadding = PaddingValues(4.dp),
                onClick = { onChanged(para.copy(videoState = para.videoState xor VideoProfile.STATE_TX_ENABLED)) }
            ) {
                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_videocam),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        "Tx",
                        modifier = Modifier
                            .align(AbsoluteAlignment.CenterLeft)
                            .absolutePadding(left = 5.dp),
                        fontSize = with(LocalDensity.current) { 10.dp.toSp() }
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                modifier = Modifier.size(36.dp),
                colors = if (para.videoState and VideoProfile.STATE_RX_ENABLED != 0) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                contentPadding = PaddingValues(4.dp),
                onClick = { onChanged(para.copy(videoState = para.videoState xor VideoProfile.STATE_RX_ENABLED)) }
            ) {
                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_videocam),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        "Rx",
                        modifier = Modifier
                            .align(AbsoluteAlignment.CenterLeft)
                            .absolutePadding(left = 5.dp),
                        fontSize = with(LocalDensity.current) { 10.dp.toSp() }
                    )
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Answer delay") }, value = para.answerDelay.toString(),
            onValueChange = {
                val new = if (it.isEmpty()) 0L else it.toLongOrNull()
                if (new != null) onChanged(para.copy(answerDelay = new))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reject delay") }, value = para.rejectDelay.toString(),
            onValueChange = {
                val new = if (it.isEmpty()) 0L else it.toLongOrNull()
                if (new != null) onChanged(para.copy(rejectDelay = new))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Disconnect delay") }, value = para.disconnectDelay.toString(),
            onValueChange = {
                val new = if (it.isEmpty()) 0L else it.toLongOrNull()
                if (new != null) onChanged(para.copy(disconnectDelay = new))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}