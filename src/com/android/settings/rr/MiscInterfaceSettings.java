/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.rr.MainSettingsLayout;

import java.util.ArrayList;
import java.util.List;

public class MiscInterfaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "MiscInterfaceSettings";
    private static final String RR_OTA = "rr_ota_fab";
    private static final String RR_OTA_SIZE = "rr_ota_fab_size";

    private SwitchPreference mFabShow, mFabSize;
    private MainSettingsLayout Main;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main = new MainSettingsLayout();

        addPreferencesFromResource(R.xml.rr_interface_other_settings);

        mFabShow = (SwitchPreference) findPreference(RR_OTA);
        mFabShow.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.RR_OTA_FAB, 0) == 1));
        mFabShow.setOnPreferenceChangeListener(this);

        mFabSize = (SwitchPreference) findPreference(RR_OTA_SIZE);
        mFabSize.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.RR_OTA_SIZE, 0) == 1));
        mFabSize.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mFabShow) {
            boolean newvalue = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RR_OTA_FAB, newvalue ? 1 : 0);
            RestartMain();
            return true;
        }

        if (preference == mFabSize) {
            boolean newvalue = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RR_OTA_SIZE, newvalue ? 1 : 0);
            RestartMain();
            return true;
        }
        return false;
    }

    private void RestartMain() {
        Main.finishMain();
        Intent fabIntent = new Intent();
        fabIntent.setClassName("com.android.settings", "com.android.settings.Settings$MainSettingsLayoutActivity");
        startActivity(fabIntent);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                     boolean enabled) {
               ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.rr_interface_other_settings;
                result.add(sir);

                return result;
            }
    };
}
