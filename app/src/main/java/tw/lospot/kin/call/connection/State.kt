package tw.lospot.kin.call.connection

import android.telecom.Connection

enum class State(val telecomState: Int) {
    NEW(Connection.STATE_NEW),
    RINGING(Connection.STATE_RINGING),
    DIALING(Connection.STATE_DIALING),
    ACTIVE(Connection.STATE_ACTIVE),
    HOLDING(Connection.STATE_HOLDING),
    DISCONNECTED(Connection.STATE_DISCONNECTED),
    PULLING(Connection.STATE_PULLING_CALL),
    ;

    companion object {
        fun find(telecomState: Int): State = values().first { telecomState == it.telecomState }
    }

}