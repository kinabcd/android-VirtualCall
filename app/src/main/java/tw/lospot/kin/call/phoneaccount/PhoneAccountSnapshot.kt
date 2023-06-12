package tw.lospot.kin.call.phoneaccount

data class PhoneAccountSnapshot(
    val id: String,
    val state: Int, // 0: unregister, 1: register, 2:enabled
    val isSelfManaged: Boolean,
) {
    val isRegistered get() = state > 0
    val isEnabled get() = state == 2
}
