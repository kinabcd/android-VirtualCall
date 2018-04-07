package tw.lospot.kin.call

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.calllist_item.view.*
import tw.lospot.kin.call.connection.Call

/**
 * Created by Kin_Lo on 2018/3/8.
 */
class InCallListItem(call: Call, inflater: LayoutInflater, viewGroup: ViewGroup) {
    val view: View = inflater.inflate(R.layout.calllist_item, viewGroup, false)

    init {
        view.name.text = call.getName()
        view.end.setOnClickListener { call.disconnect() }
        when {
            Build.VERSION.SDK_INT < 25 -> run {
                view.pullCall.visibility = View.GONE
                view.pushCall.visibility = View.GONE
            }
            call.isExternal -> run {
                view.pullCall.visibility = View.VISIBLE
                view.pushCall.visibility = View.GONE
            }
            !call.isExternal -> run {
                view.pullCall.visibility = View.GONE
                view.pushCall.visibility = View.VISIBLE
            }
        }
        view.pushCall.setOnClickListener { call.push() }
        view.pullCall.setOnClickListener { call.pull() }
        view.requestVideo.setOnClickListener { call.toggleRxVideo() }
        view.requestVideo.setOnLongClickListener { call.requestVideo(); true }
        if (call.isConference) {
            call.children.forEach {
                val childItem = InCallListChildItem(it, inflater, view.child)
                view.child.addView(childItem.view)
            }
        }
    }
}