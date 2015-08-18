package com.mmazzarolo.dev.easy_bookmarks.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.mmazzarolo.dev.easy_bookmarks.R;


/**
 * Created by Matteo on 07/08/2015.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
