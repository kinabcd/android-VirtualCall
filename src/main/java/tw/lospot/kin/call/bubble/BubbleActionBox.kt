package tw.lospot.kin.call.bubble

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import kotlinx.android.synthetic.main.bubble_action_box.view.*
import tw.lospot.kin.call.R

class BubbleActionBox(context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val layoutInflater = LayoutInflater.from(context)
    var actions
        get() = adapter.actions
        set(value) {
            adapter.actions = value
        }
    var y = 0
        set(value) {
            field = value
            rootParam.y = y
        }

    val width
        get() = if (rootView.width == 0) {
            rootView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            rootView.measuredWidth
        } else {
            rootView.width
        }

    private val adapter by lazy { BubbleActionAdapter(context) }

    private val rootView by lazy {
        val field = layoutInflater.inflate(R.layout.bubble_action_box, null, false)
        field.actions.layoutManager = LinearLayoutManager(context)
        field.actions.adapter = adapter
        return@lazy field
    }

    private val rootParam by lazy {
        WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT)
    }
    var isShowing = false
        private set

    fun show(callback: () -> Unit = {}) {
        if (!isShowing) {
            rootParam.gravity = Gravity.TOP or Gravity.END
            windowManager.addView(rootView, rootParam)
            isShowing = true
            callback()
        }
    }

    fun hide(callback: () -> Unit = {}) {
        if (isShowing) {
            windowManager.removeView(rootView)
            isShowing = false
            callback()
        }
    }
}