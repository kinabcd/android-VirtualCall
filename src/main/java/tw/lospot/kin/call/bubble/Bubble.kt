package tw.lospot.kin.call.bubble

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.telecom.Connection.*
import android.view.Gravity.END
import android.view.Gravity.TOP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_PHONE
import kotlinx.android.synthetic.main.bubble.view.*
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call

/**
 * Created by Kin_Lo on 2018/2/27.
 */
class Bubble(context: Context, private val call: Call) : Call.Listener {

    enum class State {
        NONE, SHOWN, EXPANDED
    }

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val layoutInflater = LayoutInflater.from(context)
    private val packageName = context.packageName
    private val actionsView = BubbleActionBox(context)

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
        rootView.callId.setOnClickListener {
            if (state == State.EXPANDED)
                collapse()
            else
                expand()
        }
        rootView.callId
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
        get() = rootParam.x
        set(value) {
            rootParam.x = value
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

    private fun startShow() {
        updateViews()
        rootView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        x = -rootView.measuredWidth
        rootParam.gravity = TOP or END
        windowManager.addView(rootView, rootParam)
        call.addListener(this)
        isViewAdded = true
        animateMoveForX(idView.measuredWidth - rootView.measuredWidth) {
            setState(State.SHOWN)
        }
    }

    private fun startHide() {
        animateMoveForX(-rootView.measuredWidth) {
            windowManager.removeView(rootView)
            call.removeListener(this)
            isViewAdded = false
            setState(State.NONE)
        }
    }

    private fun startExpand() {
        animateMoveForX((actionsView.width - rootView.width) / 2) {
            actionsView.y = y + rootView.height
            actionsView.show {
                setState(State.EXPANDED)
            }
        }
    }

    private fun startCollapse() {
        actionsView.hide {
            animateMoveForX(idView.measuredWidth - rootView.measuredWidth) {
                setState(State.SHOWN)
            }
        }
    }

    private fun animateMoveForX(targetX: Int, endCallback: () -> Unit) {
        val animator = ValueAnimator.ofInt(x, targetX)
        animator.addUpdateListener {
            x = it.animatedValue as Int
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                rootParam.x = targetX
                windowManager.updateViewLayout(rootView, rootParam)
                endCallback()
            }

            override fun onAnimationCancel(animation: Animator?) {
                onAnimationEnd(animation)
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        })
        animator.start()
    }

    private fun updateViews() {
        idView.text = call.id.toString()
        if (call.isExternal) {
            idView.backgroundTintList = ColorStateList.valueOf(0xffAAAAAA.toInt())
            actionsView.actions = emptyList()
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
                else -> ArrayList(0)
            }
        }
    }

    override fun onCallStateChanged(call: Call, newState: Int) {
        updateViews()
    }

    private fun gerAnswerAction(): BubbleAction {
        return BubbleAction(Icon.createWithResource(packageName, android.R.drawable.ic_menu_call),
                "Answer", {
            call.answer()
            collapse()
        })
    }

    private fun gerDisconnectAction(): BubbleAction {
        return BubbleAction(Icon.createWithResource(packageName, android.R.drawable.ic_delete),
                "Disconnect", {
            call.disconnect()
            collapse()
        })
    }

    private fun gerPushAction(): BubbleAction {
        return BubbleAction(Icon.createWithResource(packageName, android.R.drawable.stat_sys_upload),
                "Push to External", {
            call.push()
            collapse()
        })
    }
}