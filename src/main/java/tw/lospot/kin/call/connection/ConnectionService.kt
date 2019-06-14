package tw.lospot.kin.call.connection

import android.os.Build
import android.telecom.*
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.bubble.BubbleList
import tw.lospot.kin.call.notification.StatusBarNotifier
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper

class ConnectionService : android.telecom.ConnectionService() {

    private val bubbleList by lazy { BubbleList(applicationContext) }
    private val notification by lazy { StatusBarNotifier(this) }
    override fun onCreate() {
        super.onCreate()
        notification.setUp()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            bubbleList.setUp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notification.cleanUp()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            bubbleList.cleanUp()
        }
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        val connection = ConnectionProxy(applicationContext, request)
        CallList.onCallAdded(Call(connection))
        connection.telecomConnection.setRinging()
        return connection.telecomConnection
    }

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        val connection = ConnectionProxy(applicationContext, request)
        CallList.onCallAdded(Call(connection))
        connection.telecomConnection.setDialing()
        return connection.telecomConnection
    }

    override fun onConference(connection1: Connection, connection2: Connection) {
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
