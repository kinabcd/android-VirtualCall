package tw.lospot.kin.call.bubble

import android.graphics.drawable.Icon

data class BubbleAction(val icon: Icon? = null, val text: String? = null, val callback: () -> Unit)