package tw.lospot.kin.call.toolkitinfo

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import tw.lospot.kin.call.R

class InfoActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        setActionBar(findViewById(R.id.toolbar))
        actionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.commit {
            replace(R.id.preference, InfoFragment())
        }
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return true
    }

}