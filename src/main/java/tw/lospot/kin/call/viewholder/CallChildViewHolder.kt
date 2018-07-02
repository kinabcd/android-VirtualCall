package tw.lospot.kin.call.viewholder

import android.view.View
import android.widget.TextView
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call

class CallChildViewHolder(view: View) : BaseViewHolder(view) {
    var call: Call? = null
        set(value) {
            field = value
            updateView()
        }
    private val name = view.findViewById<TextView>(R.id.name)
    private val end = view.findViewById<View>(R.id.end)

    init {
        end.setOnClickListener { call?.disconnect() }
    }

    private fun updateView() {
        name.text = call?.getName()
    }
}