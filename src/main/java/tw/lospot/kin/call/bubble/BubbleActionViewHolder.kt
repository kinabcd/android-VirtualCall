package tw.lospot.kin.call.bubble

import android.graphics.drawable.Icon
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.bubble_action_item.view.*

class BubbleActionViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    init {
        view.setOnClickListener { onClick() }
    }

    var onClick: () -> Unit = {}
    var icon: Icon? = null
        set(value) {
            field = value
            view.icon.setImageIcon(value)
        }

    var text: String? = null
        set(value) {
            field = value
            view.text.text = value
        }
}