package tw.lospot.kin.call.connection

import android.telecom.*
import android.telecom.TelecomManager.EXTRA_INCOMING_VIDEO_STATE
import android.telecom.TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.bubble.BubbleList

class ConnectionService : android.telecom.ConnectionService() {

    private val bubbleList by lazy { BubbleList(applicationContext) }
    override fun onCreate() {
        super.onCreate()
        bubbleList.setUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleList.cleanUp()
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): android.telecom.Connection {
        val connection = ConnectionProxy(applicationContext, request.address, request.accountHandle)
        connection.videoState = request.extras.getInt(EXTRA_INCOMING_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
        CallList.onCallAdded(Call(connection))
        connection.telecomConnection.setRinging()
        return connection.telecomConnection
    }

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): android.telecom.Connection {
        val connection = ConnectionProxy(applicationContext, request.address, request.accountHandle)
        connection.videoState = request.extras.getInt(EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
        CallList.onCallAdded(Call(connection))
        connection.telecomConnection.setDialing()
        return connection.telecomConnection
    }

    override fun onConference(connection1: android.telecom.Connection, connection2: android.telecom.Connection) {
        val phoneAccountHandle = CallList.getCall(connection1)?.phoneAccountHandle
                ?: CallList.getCall(connection2)?.phoneAccountHandle
                ?: PhoneAccountHelper(applicationContext).phoneAccountHandle
        val conference = ConferenceProxy(applicationContext, phoneAccountHandle)
        conference.telecomConference.addConnection(connection1)
        conference.telecomConference.addConnection(connection2)
        addConference(conference.telecomConference)
        CallList.onCallAdded(Call(conference))
        Log.d(this, "onConference")
    }

    override fun onRemoteConferenceAdded(conference: RemoteConference) {
        super.onRemoteConferenceAdded(conference)
        Log.d(this, "onRemoteConferenceAdded $conference")
    }

    override fun onRemoteExistingConnectionAdded(connection: RemoteConnection) {
        super.onRemoteExistingConnectionAdded(connection)
        Log.d(this, "onRemoteExistingConnectionAdded $connection")
    }
}
