/*
 * Copyright (C) 2016 RR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Build;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class MiscSettings extends SettingsPreferenceFragment  implements Indexable {

    private static final String APP_REMOVER = "system_app_remover";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private PreferenceScreen mAppRemover;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();

        mAppRemover = (PreferenceScreen) findPreference(APP_REMOVER);

        if (isMagiskSupported() || isSUSupported() || isRootForAppsEnabled()) {
        } else {
            if (mAppRemover != null)
                getPreferenceScreen().removePreference(mAppRemover);
        }

    }

    public static boolean isRootForAppsEnabled() {
        int value = SystemProperties.getInt(ROOT_ACCESS_PROPERTY, 0);
        boolean daemonState =
                SystemProperties.get("init.svc.su_daemon", "absent").equals("running");
        return daemonState && (value == 1 || value == 3);
    }

    public static boolean isMagiskSupported(Context context) {
        boolean magiskSupported = false;
        try {
            magiskSupported = (context.getPackageManager().getPackageInfo("com.topjohnwu.magisk", 0).versionCode > 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return magiskSupported;
    }

    public static boolean isSUSupported(Context context) {
        boolean suSupported = false;
        try {
            suSupported = (context.getPackageManager().getPackageInfo("eu.chainfire.supersu", 0).versionCode >= 185);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return suSupported;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                     boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.rr_misc;
                result.add(sir);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                ArrayList<String> result = new ArrayList<String>();
                if (!isMagiskSupported(context) && !isSUSupported(context) && !isRootForAppsEnabled()) {
                    result.add(APP_REMOVER);
                }
                return result;
            }
    };
}

