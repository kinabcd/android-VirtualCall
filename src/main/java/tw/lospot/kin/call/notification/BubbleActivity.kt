package tw.lospot.kin.call.notification

import android.app.Activity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tw.lospot.kin.call.InCallAdapter
import tw.lospot.kin.call.Log
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.connection.CallList

class BubbleActivity : Activity(), Call.Listener {
    private val adapter by lazy { InCallAdapter() }
    private val callId by lazy { intent.getIntExtra("callId", -1) }
    private val call by lazy { CallList.getAllCalls().first { it.id == callId } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bubble_content)

        val callList = findViewById<RecyclerView>(R.id.callList)
        callList.adapter = adapter
        callList.layoutManager = LinearLayoutManager(this)
        Log.v("AAAA","callId=$callId")
    }

    override fun onStart() {
        super.onStart()
        updateView()
        call.addListener(this)
    }

    override fun onCallStateChanged(call: Call, newState: Int) {
        updateView()
    }

    override fun onStop() {
        super.onStop()
        call.removeListener(this)
    }

    private fun updateView() {
        adapter.calls = listOf(call) + call.children
    }
}