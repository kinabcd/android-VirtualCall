package tw.lospot.kin.call.viewholder

import android.os.Build
import android.view.View
import android.widget.TextView
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call

class CallViewHolder(view: View) : BaseViewHolder(view) {
    var call: Call? = null
        set(value) {
            field = value
            updateView()
        }
    private val name = view.findViewById<TextView>(R.id.name)
    private val end = view.findViewById<View>(R.id.end)
    private val pullCall = view.findViewById<View>(R.id.pullCall)
    private val pushCall = view.findViewById<View>(R.id.pushCall)
    private val requestVideo = view.findViewById<View>(R.id.requestVideo)

    init {
        end.setOnClickListener { call?.disconnect() }
    }

    private fun updateView() {
        name.text = call?.getName()
        when {
            Build.VERSION.SDK_INT < 25 -> run {
                pullCall.visibility = View.GONE
                pushCall.visibility = View.GONE
            }
            call?.isExternal == true -> run {
                pullCall.visibility = View.VISIBLE
                pushCall.visibility = View.GONE
            }
            call?.isExternal == false -> run {
                pullCall.visibility = View.GONE
                pushCall.visibility = View.VISIBLE
            }
        }
        requestVideo.setOnClickListener { call?.toggleRxVideo() }
        requestVideo.setOnLongClickListener { call?.requestVideo(); true }
    }
}