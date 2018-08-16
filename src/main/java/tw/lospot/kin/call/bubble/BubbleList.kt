package tw.lospot.kin.call.bubble

import android.content.Context
import android.provider.Settings
import android.util.TypedValue
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.connection.CallList
import java.util.*

/**
 * Created by Kin_Lo on 2018/2/27.
 */

class BubbleList(val context: Context) : CallList.Listener {
    private val bubbles = HashMap<Call, Bubble>()
    override fun onCallListChanged() {
        val liveCalls = CallList.getAllCalls()
        val iterator = bubbles.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.key.hasParent || !liveCalls.contains(it.key)) {
                it.value.hide()
                iterator.remove()
            }
        }

        if (Settings.canDrawOverlays(context)) {
            liveCalls.forEach {
                if (!it.hasParent && !bubbles.containsKey(it)) {
                    bubbles[it] = Bubble(context, it)
                    bubbles[it]!!.show()
                }
            }
        }
    }

    fun setUp() {
        CallList.addListener(this)
    }

    fun cleanUp() {
        CallList.removeListener(this)
    }
}