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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.app.Fragment;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.util.rr.PackageUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.util.Helpers;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;

public class RecentsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {


    private static final String RECENTS_TYPE = "navigation_bar_recents";
    private static final String OMNISWITCH = "omniswitch";
    private static final String SLIM_RECENTS = "slim_recent_panel";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";
    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");


	private ListPreference mRecentsType;
    private Preference mOmniSwitch;
    private Preference mSlimRecents;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_recents);

        mSlimRecents = (Preference) findPreference(SLIM_RECENTS);
        mOmniSwitch = (Preference) findPreference(OMNISWITCH);

        mRecentsType = (ListPreference) findPreference(RECENTS_TYPE);
        int type = Settings.System.getIntForUser(getActivity().getContentResolver(),
                            Settings.System.NAVIGATION_BAR_RECENTS, 0,
                            UserHandle.USER_CURRENT);

        if (!PackageUtils.isAvailableApp(OMNISWITCH_PACKAGE_NAME, getActivity())) {
            getPreferenceScreen().removePreference(mOmniSwitch);
            mRecentsType.setEntries(getResources().getStringArray(R.array.recents_type_title_entries_2));
            mRecentsType.setEntryValues(getResources().getStringArray(R.array.recents_type_title_values_2));
	}
	    
        mRecentsType.setValue(String.valueOf(type));
        mRecentsType.setSummary(mRecentsType.getEntry());
        mRecentsType.setOnPreferenceChangeListener(this);
        updatePreference(type);

    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentsType) {
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_RECENTS,
                    Integer.valueOf((String) newValue));
            int val = Integer.parseInt((String) newValue);
            if (val== 0 || val == 2) {
                Helpers.showSystemUIrestartDialog(getActivity());
            }
            mRecentsType.setValue(String.valueOf(newValue));
            mRecentsType.setSummary(mRecentsType.getEntry());
            updatePreference(val);
        } 
		return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mOmniSwitch){
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public void updatePreference(int type) {
     if (PackageUtils.isAvailableApp(OMNISWITCH_PACKAGE_NAME, getActivity())) {
        if(type == 0 || type == 2) {
           mSlimRecents.setEnabled(false);
           mOmniSwitch.setEnabled(false);
        } else if (type == 3) {
           mSlimRecents.setEnabled(true);
           mOmniSwitch.setEnabled(false);
        } else if (type == 1) {
           mSlimRecents.setEnabled(false);
           mOmniSwitch.setEnabled(true);
        }
     } else {
        if(type == 0 || type == 2) {
           mSlimRecents.setEnabled(false);
        } else if (type == 3) {
           mSlimRecents.setEnabled(true);
        }
     }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                     boolean enabled) {
               ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.rr_recents;
                result.add(sir);

                return result;
            }
    };
}
