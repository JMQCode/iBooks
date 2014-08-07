package com.greenlemonmobile.app.ebook;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.greenlemonmobile.app.ebook.entity.UpgradeInfo;

public class SettingPreference extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_about);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
	    if (preference.getKey().equals("about_update")) {
	        UpgradeInfo.checkUpdate(this, false);
	    }
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onResume() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
			findPreference("about_version").setSummary(pi.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		super.onResume();
	}

}
