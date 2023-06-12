package tw.lospot.kin.call.bubble

import android.content.Context
import android.graphics.Point
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.connection.CallSnapshot
import kotlin.math.min

/**
 * Created by Kin_Lo on 2018/2/27.
 */

class BubbleList(val context: Context) {
    private val px64dp = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        64f,
        context.resources.displayMetrics
    ).toInt()
    private val px48dp = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        48f,
        context.resources.displayMetrics
    ).toInt()
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val bubbles = HashMap<Int, Bubble>()
    private val jobs = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + jobs)

    private fun isSpaceUsed(newY: Int): Boolean {
        val windowSize = Point().apply { windowManager.defaultDisplay.getSize(this) }
        return bubbles.map { it.value }
            .filter { it.x < windowSize.x / 2 } // Bubble at left
            .any { it.y in newY..(newY + px48dp) || newY in it.y..(it.y + px48dp) }
    }

    private fun onRootCallsChanged(liveCalls: List<CallSnapshot>) {
        val removed = bubbles.keys - liveCalls.map { it.id }.toSet()
        removed.forEach { bubbles.remove(it)?.hide() }

        if (Settings.canDrawOverlays(context)) {
            val added = liveCalls.filterNot { it.id !in bubbles.keys }
            val windowSize = Point().apply { windowManager.defaultDisplay.getSize(this) }
            added.forEach { call ->
                bubbles[call.id] = Bubble(context, call.rawCall).apply {
                    var defaultY = px64dp
                    while (isSpaceUsed(defaultY)) {
                        defaultY += px64dp
                    }
                    y = min(defaultY, windowSize.y - px64dp)
                    show()
                }
            }
        }
        liveCalls.forEach {
            bubbles[it.id]?.onCallStateChanged(it, it.state)
        }
    }

    fun setUp() {
        scope.launch { CallList.rootCalls.collect { onRootCallsChanged(it) } }
    }

    fun cleanUp() {
        onRootCallsChanged(emptyList())
        jobs.cancelChildren()
    }
}