package tw.lospot.kin.call.connection

import android.annotation.TargetApi
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.telecom.Connection

@TargetApi(28)
class RttRobot(private val rttTextStream: Connection.RttTextStream) : Handler.Callback {
    private val handler = Handler(Looper.getMainLooper(), this)
    private var savedString = ""
    fun start() {
        handler.sendEmptyMessage(0)
    }

    fun stop() {
        handler.removeMessages(0)
    }

    override fun handleMessage(m: Message): Boolean {
        val str = rttTextStream.readImmediately()
        if (str != null) {
            savedString += str
            if (savedString.contains(10.toChar())) {
                rttTextStream.write("Echo: $savedString")
                savedString = ""
            }
        }
        handler.sendEmptyMessageDelayed(0, 200)
        return true
    }
}