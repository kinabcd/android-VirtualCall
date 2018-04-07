package tw.lospot.kin.call

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.calllist_item_child.view.*
import tw.lospot.kin.call.connection.Call

/**
 * Created by Kin_Lo on 2018/3/8.
 */
class InCallListChildItem(call: Call, inflater: LayoutInflater, viewGroup: ViewGroup) {
    val view: View = inflater.inflate(R.layout.calllist_item_child, viewGroup, false)

    init {
        view.name.text = call.getName()
        view.end.setOnClickListener { call.disconnect() }
    }
}