package tw.lospot.kin.call.connection

import android.telecom.Conferenceable
import android.telecom.Connection
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tw.lospot.kin.call.Log

/**
 * The List of in call Connections and Conferences
 * Created by kin on 2017/8/19.
 */
object CallList {
    private const val TAG = "CallList"
    private val scope = MainScope()
    private val jobMap = MutableStateFlow(emptyMap<TelecomCall, Job>())
    private val telecomCalls = jobMap.map { it.keys.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    private val telecomCallsByConferencable = telecomCalls.map { calls ->
        calls.associateBy { it.conferenceable }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private val changed = MutableSharedFlow<Unit>()
    val calls = telecomCallsByConferencable.combine(changed.onStart { emit(Unit) }) { calls, _ ->
        calls.values.map { CallSnapshot(it, calls) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val rootCalls = calls.map { calls ->
        calls.filterNot { it.hasParent }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val conferenceables = rootCalls.map { calls ->
        calls.filter { !it.isExternal }.map { it.rawCall.conferenceable }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    init {
        scope.launch { calls.collect{
            it.forEach { Log.v("ZZZZ", "$it") }
        } }
        scope.launch { changed.collect{ Log.v("ZZZZ", "change")
        } }
    }

    fun onCallAdded(call: TelecomCall) {
        Log.d(TAG, "onCallAdded $call")
        jobMap.update { oldMap ->
            oldMap + (call to scope.launch {
                launch { call.onStateChanged.collect { onStateChanged(call) } }
                launch { call.onStateChanged.collect(changed) }
                launch { call.onPlayDtmfTone.collect { onPlayDtmfTone(call, it) } }
                launch { conferenceables.collect { call.conferenceables = it } }
                if (call.isConference) launch { telecomCalls.collect { call.maybeUnboxConference() } }
            })
        }
    }

    private fun onCallRemoved(call: TelecomCall) {
        Log.d(TAG, "onCallRemoved $call")
        jobMap.update {
            it[call]?.cancel()
            it - call
        }
    }

    fun getCall(conferenceable: Conferenceable): TelecomCall? =
        telecomCallsByConferencable.value[conferenceable]

    private fun onStateChanged(call: TelecomCall) {
        when (call.state) {
            Connection.STATE_DISCONNECTED -> onCallRemoved(call)
            Connection.STATE_ACTIVE -> if (!call.hasParent) {
                telecomCalls.value
                    .filter { call != it }
                    .filter { !it.hasParent }
                    .filter { it.state == Connection.STATE_ACTIVE }
                    .forEach { it.hold() }
            }
        }
    }

    private fun onPlayDtmfTone(call: TelecomCall, c: Char) {
        when (c) {
            '1' -> call.toggleRxVideo()
            '2' -> call.pushInternalCall()
            '3' -> call.requestRtt()
        }
    }
}