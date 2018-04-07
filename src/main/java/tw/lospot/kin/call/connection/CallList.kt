package tw.lospot.kin.call.connection

import android.telecom.Connection
import tw.lospot.kin.call.Log
import java.util.concurrent.CopyOnWriteArraySet

/**
 * The List of in call Connections and Conferences
 * Created by kin on 2017/8/19.
 */
object CallList {
    private const val TAG = "CallList"
    private val sCalls = CopyOnWriteArraySet<Call>()
    private val sCallsByTelecomCall = HashMap<TelecomCall.Common, Call>()
    private val sListeners = CopyOnWriteArraySet<Listener>()
    private fun notifyCallListChanged() {
        sListeners.forEach {
            it.onCallListChanged()
        }
    }

    fun addListener(listener: Listener) {
        sListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        sListeners.remove(listener)
    }

    fun onCallAdded(call: Call) {
        Log.d(TAG, "onCallAdded $call")
        sCalls.add(call)
        sCallsByTelecomCall[call.telecomCall] = call
        call.addListener(CallListener)
        notifyCallListChanged()
    }

    fun onCallRemoved(call: Call) {
        Log.d(TAG, "onCallRemoved $call")
        sCalls.remove(call)
        sCallsByTelecomCall.remove(call.telecomCall)
        call.removeListener(CallListener)
        getAllCalls().forEach { it.maybeDestroy() }
        notifyCallListChanged()
    }

    fun getAllCalls(): Set<Call> = sCalls

    fun isTracking(call: Call): Boolean = sCalls.contains(call)

    fun getCall(telecomCall: TelecomCall.Common): Call? = sCallsByTelecomCall[telecomCall]

    fun onStateChanged(call: Call, newState: Int) {
        if (!isTracking(call) && call.state != Connection.STATE_DISCONNECTED) {
            onCallAdded(call)
        } else if (isTracking(call) && call.state == Connection.STATE_DISCONNECTED) {
            onCallRemoved(call)
        }
        if (newState == Connection.STATE_ACTIVE && !call.hasParent) {
            getAllCalls()
                    .filter { call != it }
                    .filter { !call.children.contains(it) }
                    .filter { it.state == Connection.STATE_ACTIVE }
                    .filter { !it.hasParent }
                    .forEach { it.hold() }
        }

        val allConferenceableCall = getAllCalls().filter { !it.hasParent && !it.isExternal }
        allConferenceableCall.forEach {
            it.conferenceable = allConferenceableCall
        }
        notifyCallListChanged()
    }

    object CallListener : Call.Listener {
        override fun onCallStateChanged(call: Call, newState: Int) {
            onStateChanged(call, newState)
        }
    }

    interface Listener {
        fun onCallListChanged()
    }
}