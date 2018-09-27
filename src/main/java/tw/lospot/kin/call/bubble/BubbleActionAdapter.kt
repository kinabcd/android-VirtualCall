package tw.lospot.kin.call.bubble

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import tw.lospot.kin.call.R

class BubbleActionAdapter(context: Context) : RecyclerView.Adapter<BubbleActionViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    var actions = emptyList<BubbleAction>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = actions.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleActionViewHolder =
            BubbleActionViewHolder(layoutInflater.inflate(R.layout.bubble_action_item, parent, false))

    override fun onBindViewHolder(holder: BubbleActionViewHolder, position: Int) {
        actions[position].let {
            holder.icon = it.icon
            holder.text = it.text
            holder.onClick = it.callback
        }
    }
}