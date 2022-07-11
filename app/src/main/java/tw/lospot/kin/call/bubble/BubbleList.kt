package tw.lospot.kin.call.bubble

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.connection.CallList
import kotlin.math.min

/**
 * Created by Kin_Lo on 2018/2/27.
 */

class BubbleList(val context: Context) : CallList.Listener {
    private val px64dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, context.resources.displayMetrics).toInt()
    private val px48dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.resources.displayMetrics).toInt()
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val bubbles = HashMap<Call, Bubble>()
    override fun onCallListChanged() {
        val liveCalls = CallList.rootCalls
        val iterator = bubbles.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (!liveCalls.contains(it.key)) {
                it.value.hide()
                iterator.remove()
            }
        }

        if (Settings.canDrawOverlays(context)) {
            val windowSize = Point().apply { windowManager.defaultDisplay.getSize(this) }
            liveCalls.forEach { call ->
                if (!call.hasParent && !bubbles.containsKey(call)) {
                    bubbles[call] = Bubble(context, call).apply {
                        var defaultY = px64dp
                        while (isSpaceUsed(defaultY)) {
                            defaultY += px64dp
                        }
                        y = min(defaultY, windowSize.y - px64dp)
                        show()
                    }
                }
            }
        }
    }

    private fun isSpaceUsed(newY: Int): Boolean {
        val windowSize = Point().apply { windowManager.defaultDisplay.getSize(this) }
        return bubbles.map { it.value }
                .filter { it.x < windowSize.x / 2 } // Bubble at left
                .any { it.y in newY..(newY + px48dp) || newY in it.y..(it.y + px48dp) }
    }

    fun setUp() {
        CallList.addListener(this)
    }

    fun cleanUp() {
        CallList.removeListener(this)
    }
}