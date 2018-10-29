package net.arkellyga.gpstracker.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import net.arkellyga.gpstracker.R;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
