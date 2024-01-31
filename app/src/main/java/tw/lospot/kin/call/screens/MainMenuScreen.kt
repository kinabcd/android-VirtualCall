package tw.lospot.kin.call.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.combine
import tw.lospot.kin.call.BuildConfig
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.connection.CallSnapshot
import tw.lospot.kin.call.navigation.APP_INFO
import tw.lospot.kin.call.phoneaccount.PhoneAccountSnapshot
import tw.lospot.kin.call.ui.AccountAddCallAction
import tw.lospot.kin.call.ui.AccountEditAction
import tw.lospot.kin.call.ui.AccountInfo
import tw.lospot.kin.call.ui.CallInfo
import tw.lospot.kin.call.ui.IconButton
import tw.lospot.kin.call.ui.LocalPhoneAccountManager

private val requiredPermission = arrayListOf(
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.CAMERA,
).apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        add(Manifest.permission.READ_PHONE_NUMBERS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        add(Manifest.permission.RECORD_AUDIO)
    }
}.toTypedArray()

@Composable
fun MainMenuScreen(navController: NavController) {
    val context = LocalContext.current
    val phoneAccountManager = LocalPhoneAccountManager.current
    var missedPermission by remember { mutableStateOf(requiredPermission) }
    fun updateMissedPermission() {
        missedPermission = requiredPermission.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    val requestMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateMissedPermission() }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        updateMissedPermission()
        if (missedPermission.isNotEmpty()) {
            requestMultiplePermissions.launch(missedPermission)
        }
    }
    if (missedPermission.isNotEmpty()) return

    var isEditing by remember { mutableStateOf(false) }
    BackHandler(isEditing) { isEditing = false }
    val accounts by remember {
        combine(
            phoneAccountManager.allAccounts, CallList.rootCalls
        ) { accounts, rootCalls ->
            accounts.map { account ->
                AccountModel(
                    account,
                    rootCalls.sortedBy { it.id }.filter { it.accountHandle.id == account.id }
                )
            }
        }
    }.collectAsStateWithLifecycle(emptyList())
    Column(modifier = Modifier.fillMaxSize()) {
        AccountList(accounts, modifier = Modifier.weight(1f), isEditing)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountList(
    accounts: List<AccountModel>,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        accounts.forEach { account ->
            stickyHeader(key = account.meta.id) {
                AccountInfo(account, Modifier.animateItemPlacement())
            }
            item(key = account.meta.id + ":ACTION") {
                AnimatedContent(
                    modifier = Modifier
                        .animateItemPlacement()
                        .fillMaxWidth(),
                    targetState = isEditing,
                    label = account.meta.id + ":ACTION"
                ) { isEditing ->
                    if (isEditing) {
                        AccountEditAction(account)
                    } else {
                        Column(Modifier.animateContentSize()) {
                            account.calls.forEach { CallInfo(call = it) }
                            AccountAddCallAction(account)
                        }
                    }
                }
            }
            item(key = account.meta.id + ":SPACE") { Spacer(modifier = Modifier.height(8.dp)) }
        }
        if (isEditing) {
            stickyHeader(key = "NEW") { NewAccountPanelHeader(Modifier.animateItemPlacement()) }
            item(key = "NEW:ACTION") { NewAccountPanel(Modifier.animateItemPlacement()) }
            item(key = "NEW:SPACE") { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun NewAccountPanelHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp)
            .then(modifier)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            text = "New account"
        )
    }
}

@Composable
private fun NewAccountPanel(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .then(modifier)
    ) {
        val phoneAccountManager = LocalPhoneAccountManager.current
        val allIds = phoneAccountManager.allIds.collectAsStateWithLifecycle(emptyList())
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
            enabled = newId.isNotBlank() && newId !in allIds.value,
            onClick = { phoneAccountManager.add(newId) }
        )
    }
}

data class AccountModel(
    val meta: PhoneAccountSnapshot,
    val calls: List<CallSnapshot>
)

