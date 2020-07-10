package tw.lospot.kin.call.toolkitinfo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import tw.lospot.kin.call.R


class InfoFragment : PreferenceFragmentCompat() {

    private val appVersionPref by lazy { findPreference<Preference>("application_version")!! }
    private val appAuthorPref by lazy { findPreference<Preference>("application_author")!! }

    private val packageManager by lazy { requireContext().packageManager }
    private val myPackageInfo by lazy { packageManager.getPackageInfo(requireContext().packageName, 0) }
    private val myVersionCode by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            myPackageInfo.longVersionCode
        } else {
            myPackageInfo.versionCode.toLong()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.information)
        appVersionPref.summary = "${myPackageInfo.versionName} ($myVersionCode)"
        appAuthorPref.setOnPreferenceClickListener { _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kin.lospot.tw")))
            true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }
}