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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Build;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

import com.android.internal.util.rr.PackageUtils;
import com.android.internal.logging.MetricsProto.MetricsEvent;

public class MiscSettings extends SettingsPreferenceFragment {

    private static final String APP_REMOVER = "system_app_remover";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private static final String RR_OTA_APP = "update_settings";
    private static final String RR_DELTA = "delta_updates";
    private static final String WEATHER_SETTINGS = "weather_settings_pref";
	private static final String WEATHER_PACKAGE = "com.cyanogenmod.lockclock";
	private static final String OTA_PACKAGE = "com.resurrection.ota";
	private static final String DELTA_PACKAGE = "eu.chainfire.opendelta";

    private PreferenceScreen mWeatherPref;
    private PreferenceScreen mDelta;
    private PreferenceScreen mOta;
    private PreferenceScreen mAppRemover;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();

        mAppRemover = (PreferenceScreen) findPreference(APP_REMOVER);

        // Magisk Manager
        boolean magiskSupported = false;
        // SuperSU
        boolean suSupported = false;
        try {
            magiskSupported = (getPackageManager().getPackageInfo("com.topjohnwu.magisk", 0).versionCode > 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            suSupported = (getPackageManager().getPackageInfo("eu.chainfire.supersu", 0).versionCode >= 185);
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (magiskSupported || suSupported || isRootForAppsEnabled()) {
        } else {
            if (mAppRemover != null)
                getPreferenceScreen().removePreference(mAppRemover);
        }

        PreferenceScreen mDelta = (PreferenceScreen) findPreference(RR_DELTA);
        if (!PackageUtils.isAvailableApp(DELTA_PACKAGE, getActivity())) {
            getPreferenceScreen().removePreference(mDelta);
        }

        PreferenceScreen mOta = (PreferenceScreen) findPreference(RR_OTA_APP);
        if (!PackageUtils.isAvailableApp(OTA_PACKAGE, getActivity())) {
            getPreferenceScreen().removePreference(mOta);
        }

        PreferenceScreen mWeatherPref = (PreferenceScreen) findPreference(WEATHER_SETTINGS);
        if (!PackageUtils.isAvailableApp(WEATHER_PACKAGE, getActivity())) {
            getPreferenceScreen().removePreference(mWeatherPref);
        }
    }

    public static boolean isRootForAppsEnabled() {
        int value = SystemProperties.getInt(ROOT_ACCESS_PROPERTY, 0);
        boolean daemonState =
                SystemProperties.get("init.svc.su_daemon", "absent").equals("running");
        return daemonState && (value == 1 || value == 3);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

