package tw.lospot.kin.call

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.phoneaccount.PhoneAccountHelper
import tw.lospot.kin.call.viewholder.AccountViewHolder
import tw.lospot.kin.call.viewholder.BaseViewHolder
import tw.lospot.kin.call.viewholder.CallChildViewHolder
import tw.lospot.kin.call.viewholder.CallViewHolder

class InCallAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    private var items: List<Any> = emptyList()
    var calls: List<Call> = emptyList()
        set(value) {
            field = value
            updateCallsByAccountId()
        }
    var accounts: List<PhoneAccountHelper> = emptyList()
        set(value) {
            field = value
            updateCallsByAccountId()
        }

    private fun updateCallsByAccountId() {
        val newItems = ArrayList<Any>(calls.size + accounts.size)
        accounts.forEach { account ->
            newItems.add(account)
            calls.asSequence()
                    .filter { it.phoneAccountHandle == account.phoneAccountHandle }
                    .filter { it.isConference || !it.hasParent }.toList()
                    .forEach { parentCall ->
                        newItems.add(parentCall)
                        parentCall.children.forEach { childCall ->
                            newItems.add(childCall)
                        }
                    }
        }
        items = newItems.toList()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when {
            item is PhoneAccountHelper -> 0
            item is Call && !item.hasParent -> 1
            item is Call && item.hasParent -> 2
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            0 -> AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.account_item, parent, false))
            1 -> CallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.calllist_item, parent, false))
            2 -> CallChildViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.calllist_item_child, parent, false))
            else -> throw RuntimeException("viewType error")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = items[position]
        when {
            holder is AccountViewHolder && item is PhoneAccountHelper -> holder.phoneAccountHelper = item
            holder is CallViewHolder && item is Call -> holder.call = item
            holder is CallChildViewHolder && item is Call -> holder.call = item
        }
    }

}