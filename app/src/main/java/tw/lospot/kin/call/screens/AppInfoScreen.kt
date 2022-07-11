package tw.lospot.kin.call.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import tw.lospot.kin.call.BuildConfig
import tw.lospot.kin.call.R
import tw.lospot.kin.call.ui.PageColumn
import tw.lospot.kin.call.ui.PageContent
import tw.lospot.kin.call.ui.TwoLineInfoCard


@Composable
fun AppInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    PageContent(
        title = stringResource(R.string.application_information),
        backAction = { navController.popBackStack() }) {
        PageColumn {
            TwoLineInfoCard(stringResource(R.string.application_version), content = version)
            TwoLineInfoCard(stringResource(R.string.author), "Kin Lo (kin@lospot.tw)") {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://kin.lospot.tw"))
                )
            }
        }
    }
}