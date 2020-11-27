package tw.lospot.kin.call.bubble

import android.graphics.drawable.Icon
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tw.lospot.kin.call.R

class BubbleActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val iconView = view.findViewById<ImageView>(R.id.icon)
    private val textView = view.findViewById<TextView>(R.id.text)

    init {
        view.setOnClickListener { onClick() }
    }

    var onClick: () -> Unit = {}
    var icon: Icon? = null
        set(value) {
            field = value
            iconView.setImageIcon(value)
        }

    var text: String? = null
        set(value) {
            field = value
            textView.text = value
        }
}