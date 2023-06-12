package tw.lospot.kin.call.connection

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.PhoneAccountHandle
import android.telecom.RemoteConference
import android.telecom.RemoteConnection
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.bubble.BubbleList
import tw.lospot.kin.call.notification.StatusBarNotifier
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager
import tw.lospot.kin.call.phoneaccount.PhoneAccountManager.Companion.DEFAULT_ACCOUNT

class ConnectionService : android.telecom.ConnectionService() {

    private val bubbleList by lazy { BubbleList(applicationContext) }
    private val notification by lazy { StatusBarNotifier(this) }
    override fun onCreate() {
        super.onCreate()
        notification.setUp()
        bubbleList.setUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        notification.cleanUp()
        bubbleList.cleanUp()
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val connection = ConnectionProxy(applicationContext, request)
        CallList.onCallAdded(connection)
        connection.telecomConnection.setRinging()
        return connection.telecomConnection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val connection = ConnectionProxy(applicationContext, request)
        CallList.onCallAdded(connection)
        connection.telecomConnection.setDialing()
        return connection.telecomConnection
    }

    override fun onConference(connection1: Connection, connection2: Connection) {
        val phoneAccountHandle = CallList.getCall(connection1)?.phoneAccountHandle
            ?: CallList.getCall(connection2)?.phoneAccountHandle
            ?: PhoneAccountManager(applicationContext).phoneAccountHandleFor(DEFAULT_ACCOUNT)
        val conference = ConferenceProxy(applicationContext, phoneAccountHandle)
        conference.telecomConference.addConnection(connection1)
        conference.telecomConference.addConnection(connection2)
        addConference(conference.telecomConference)
        CallList.onCallAdded(conference)
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
