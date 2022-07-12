package tw.lospot.kin.call.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import tw.lospot.kin.call.BuildConfig
import tw.lospot.kin.call.InCallController
import tw.lospot.kin.call.R
import tw.lospot.kin.call.navigation.APP_INFO
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager
import tw.lospot.kin.call.ui.*

@Composable
fun MainMenuScreen(navController: NavController) {
    val context = LocalContext.current
    val controller = remember { InCallController(context.applicationContext) }
    val requestMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        controller.refresh()
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> controller.start(requestMultiplePermissions)
            Lifecycle.Event.ON_STOP -> controller.stop()
            else -> {}
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        AccountList(controller.accounts, modifier = Modifier.weight(1f), isEditing)
        Divider()
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                painter = painterResource(R.drawable.ic_layers),
                contentDescription = null,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        )
                    )
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isEditing) {
                IconButton(
                    painter = rememberVectorPainter(image = Icons.Default.Done),
                    contentDescription = stringResource(id = android.R.string.ok),
                    onClick = { isEditing = false },
                )
            } else {
                IconButton(
                    painter = rememberVectorPainter(image = Icons.Default.Edit),
                    contentDescription = null,
                    onClick = { isEditing = true },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                painter = rememberVectorPainter(image = Icons.Default.Info),
                onClick = { navController.navigate(APP_INFO) }
            )
        }
    }
}

@Composable
fun AccountList(
    accounts: MutableList<InCallController.PhoneAccount>,
    modifier: Modifier = Modifier,
    isEditing :Boolean = false,
) {
    LazyColumn(modifier = modifier) {
        accounts.forEach { account ->
            item(key = account.id) { AccountInfo(account) }
            if (!isEditing) {
                items(account.calls, key = { it.id }) { CallInfo(call = it) }
                item { AccountAddCallAction(account) }
            } else {
                item { AccountEditAction(account) }
            }
            item { Spacer(modifier = Modifier.padding(bottom = 8.dp)) }
        }
        if (isEditing) {
            if (accounts.isNotEmpty()) item {
                Divider(modifier = Modifier.padding(start = 12.dp, end = 12.dp))
            }

            item { NewAccountPanel() }
            item { Spacer(modifier = Modifier.padding(bottom = 8.dp)) }
        }
    }
}

@Composable
fun NewAccountPanel() {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        val context = LocalContext.current
        var newId by remember { mutableStateOf("") }
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            label = { Text("Account ID") }, value = newId,
            onValueChange = { newId = it },
            singleLine = true,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            modifier = Modifier.align(Alignment.Bottom),
            painter = rememberVectorPainter(image = Icons.Default.Add),
            contentDescription = stringResource(id = android.R.string.ok),
            onClick = {
                when {
                    newId.isBlank() -> {}
                    PhoneAccountManager.getAllIds(context).contains(newId) -> {}
                    else -> PhoneAccountManager.add(context, newId)
                }
            }
        )
    }
}

