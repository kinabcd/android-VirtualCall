package tw.lospot.kin.call.ui

import androidx.compose.runtime.staticCompositionLocalOf
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager

val LocalPhoneAccountManager = staticCompositionLocalOf<PhoneAccountManager> {
    error("CompositionLocal LocalPhoneAccountManager not present")
}