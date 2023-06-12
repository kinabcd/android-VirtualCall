package tw.lospot.kin.call

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import tw.lospot.kin.call.connection.CallParameters

class AccountViewModel : ViewModel() {
    var number by mutableStateOf("0987654321")
    var para by mutableStateOf(CallParameters())
    var expand by mutableStateOf(false)
}