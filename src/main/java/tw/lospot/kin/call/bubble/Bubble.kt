package tw.lospot.kin.call.bubble

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Icon
import android.os.Build
import android.telecom.Connection.*
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.*
import android.view.Gravity.LEFT
import android.view.Gravity.TOP
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_PHONE
import android.view.animation.OvershootInterpolator
import kotlinx.android.synthetic.main.bubble.view.*
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by Kin_Lo on 2018/2/27.
 */
class Bubble(context: Context, private val call: Call) : Call.Listener {

    enum class State {
        NONE, SHOWN, EXPANDED
    }

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val layoutInflater = LayoutInflater.from(context)
    private val resources = context.resources
    private val packageName = context.packageName
    private val actionsView = BubbleActionBox(context)
    private val windowSize get() = Point().apply { windowManager.defaultDisplay.getSize(this) }
    private val marginHorizontal = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics).toInt()
    private val clickThreshold = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 5f, context.resources.displayMetrics).toInt()

    private val rootView by lazy { layoutInflater.inflate(R.layout.bubble, null, false) }
    private val rootParam by lazy {
        WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TYPE_APPLICATION_OVERLAY
                } else {
                    TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT)
    }
    private val idView by lazy {
        rootView.callId.apply {
            setOnClickListener {
                if (state == State.EXPANDED)
                    collapse()
                else
                    expand()
            }
            setOnTouchListener(TouchEventProcessor())
        }
    }
    private var targetState = State.NONE
    private var state = State.NONE
    private var pending = false
    private var isViewAdded = false

    var y: Int
        get() = rootParam.y
        set(value) {
            rootParam.y = value
            if (isViewAdded) {
                windowManager.updateViewLayout(rootView, rootParam)
            }
        }

    var x: Int
        get() = rootParam.x + rootView.measuredWidth / 2
        set(value) {
            rootParam.x = value - rootView.measuredWidth / 2
            if (isViewAdded) {
                windowManager.updateViewLayout(rootView, rootParam)
            }
        }

    fun show() {
        Log.v(this, "show $call")
        setTargetState(State.SHOWN)
    }

    fun hide() {
        Log.v(this, "hide $call")
        setTargetState(State.NONE)
    }

    private fun expand() {
        Log.v(this, "expand $call")
        setTargetState(State.EXPANDED)

    }

    private fun collapse() {
        if (targetState == State.EXPANDED) {
            Log.v(this, "collapse $call")
            setTargetState(State.SHOWN)
        }
    }

    private fun setState(state: State) {
        Log.v(this, "setState $state")
        this.state = state
        pending = false
        nextState()

    }

    private fun setTargetState(state: State) {
        Log.v(this, "setTargetState $state")
        targetState = state
        nextState()
    }

    private fun nextState() {
        if (pending) return
        if (state == targetState) return
        pending = true
        when (state) {
            State.NONE -> run {
                startShow()
            }
            State.SHOWN -> run {
                if (targetState == State.NONE) {
                    startHide()
                } else if (targetState == State.EXPANDED) {
                    startExpand()
                }
            }
            State.EXPANDED -> run {
                startCollapse()
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun startShow() {
        updateViews()
        rootView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        x = -rootView.measuredWidth / 2
        rootParam.gravity = TOP or LEFT
        windowManager.addView(rootView, rootParam)
        call.addListener(this)
        isViewAdded = true
        moveToEdge {
            setState(State.SHOWN)
        }
    }

    private fun startHide() {
        moveToOutOfEdge {
            windowManager.removeView(rootView)
            call.removeListener(this)
            isViewAdded = false
            setState(State.NONE)
        }
    }

    private fun startExpand() {
        val moveToLeft = x < windowSize.x / 2
        val targetX = if (moveToLeft) {
            actionsView.width / 2 + marginHorizontal
        } else {
            windowSize.x - (actionsView.width / 2) - marginHorizontal
        }
        animateMoveForX(targetX) {
            actionsView.x = targetX
            actionsView.y = y + rootView.height
            actionsView.show {
                setState(State.EXPANDED)
            }
        }
    }

    private fun startCollapse() {
        actionsView.hide {
            moveToEdge {
                setState(State.SHOWN)
            }
        }
    }

    private fun moveToOutOfEdge(endCallback: () -> Unit = {}) {
        val moveToLeft = x < windowSize.x / 2
        val targetX = if (moveToLeft) {
            -rootView.measuredWidth / 2
        } else {
            windowSize.x + rootView.measuredWidth / 2
        }
        animateMoveForX(targetX, endCallback)
    }

    private fun moveToEdge(endCallback: () -> Unit = {}) {
        val moveToLeft = x < windowSize.x / 2
        val targetX = if (moveToLeft) {
            rootView.measuredWidth / 2 + marginHorizontal
        } else {
            windowSize.x - rootView.measuredWidth / 2 - marginHorizontal
        }
        animateMoveForX(targetX, endCallback)
    }

    private fun animateMoveForX(targetX: Int, endCallback: () -> Unit = {}) {
        ValueAnimator.ofInt(x, targetX).apply {
            addUpdateListener {
                x = it.animatedValue as Int
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    x = targetX
                    endCallback()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    onAnimationEnd(animation)
                }

                override fun onAnimationRepeat(animation: Animator?) {
                }
            })
            interpolator = OvershootInterpolator()
            duration = 200
        }.start()
    }

    private fun updateViews() {
        idView.text = call.id.toString()
        if (call.isExternal) {
            idView.backgroundTintList = ColorStateList.valueOf(0xffAAAAAA.toInt())
            actionsView.actions = arrayListOf(gerDisconnectAction())
        } else {
            idView.backgroundTintList = ColorStateList.valueOf(when (call.state) {
                STATE_INITIALIZING -> 0xffCCCCCC.toInt()
                STATE_NEW -> 0xffCCCCCC.toInt()
                STATE_RINGING -> 0xffAAFFAA.toInt()
                STATE_DIALING -> 0xffFFAAAA.toInt()
                STATE_ACTIVE -> 0xffAAAAFF.toInt()
                STATE_HOLDING -> 0xffAAAAAA.toInt()
                STATE_DISCONNECTED -> 0xff999999.toInt()
                STATE_PULLING_CALL -> 0xffFF0000.toInt()
                else -> 0xffffffff.toInt()
            })
            actionsView.actions = when (call.state) {
                STATE_RINGING -> {
                    arrayListOf(gerDisconnectAction())
                }
                STATE_DIALING -> {
                    arrayListOf(gerAnswerAction(), gerDisconnectAction())
                }
                STATE_ACTIVE -> {
                    arrayListOf(gerPushAction(), gerDisconnectAction())
                }
                STATE_HOLDING -> {
                    arrayListOf(gerDisconnectAction())
                }
                else -> emptyList()
            }
        }
    }

    override fun onCallStateChanged(call: Call, newState: Int) {
        updateViews()
    }

    private fun gerAnswerAction(): BubbleAction {
        return BubbleAction(
                Icon.createWithResource(packageName, R.drawable.ic_answer_call)
                        .setTint(resources.getColor(android.R.color.holo_green_light, null)),
                resources.getString(R.string.answer_call)) {
            call.answer()
            collapse()
        }
    }

    private fun gerDisconnectAction(): BubbleAction {
        return BubbleAction(
                Icon.createWithResource(packageName, R.drawable.ic_end_call)
                        .setTint(resources.getColor(android.R.color.holo_red_light, null)),
                resources.getString(R.string.disconnect_call)) {
            call.disconnect()
            collapse()
        }
    }

    private fun gerPushAction(): BubbleAction {
        return BubbleAction(Icon.createWithResource(packageName, android.R.drawable.stat_sys_upload), "Push to External") {
            call.push()
            collapse()
        }
    }

    private inner class TouchEventProcessor : View.OnTouchListener {
        private var isTouching = false
        private var downPoint: Point = Point(0, 0)
        private var downViewPoint: Point = Point(0, 0)
        private var maybeClick = false

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isTouching) return false
                    downPoint.x = motionEvent.rawX.toInt()
                    downPoint.y = motionEvent.rawY.toInt()
                    downViewPoint.x = x
                    downViewPoint.y = y
                    isTouching = true
                    maybeClick = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isTouching) return false
                    val deltaX = motionEvent.rawX.toInt() - downPoint.x
                    val deltaY = motionEvent.rawY.toInt() - downPoint.y
                    x = downViewPoint.x + deltaX
                    y = downViewPoint.y + deltaY
                    if (maybeClick && sqrt(deltaX.toDouble().pow(2.0) + deltaY.toDouble().pow(2.0)) > clickThreshold) {
                        maybeClick = false
                        if (targetState == State.EXPANDED) {
                            actionsView.hide {
                                setTargetState(State.SHOWN)
                                setState(State.SHOWN)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isTouching = false
                    moveToEdge()
                    if (maybeClick) {
                        view.performClick()
                    }
                }
            }
            return true
        }
    }
}