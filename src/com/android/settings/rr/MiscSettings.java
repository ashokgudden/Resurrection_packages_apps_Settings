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
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;

import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;
import java.lang.*;
import java.util.List;

import com.android.internal.util.rr.PackageUtils;
import com.android.internal.logging.MetricsProto.MetricsEvent;

public class MiscSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final int DEFAULT_SMS_MAX_COUNT = 30;
    private static final String APP_REMOVER = "system_app_remover";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private static final String RR_OTA_APP = "update_settings";
    private static final String RR_DELTA = "delta_updates";
    private static final String WEATHER_SETTINGS = "weather_settings_pref";
	private static final String WEATHER_PACKAGE = "com.cyanogenmod.lockclock";
	private static final String OTA_PACKAGE = "com.resurrection.ota";
	private static final String DELTA_PACKAGE = "eu.chainfire.opendelta";
	private static final String LOGCAT_PACKAGE = "org.omnirom.logcat";
    private static final String SELINUX = "selinux";
    private static final String SELINX_PREF ="selinux_switch";
    private static final String KEY_SMS_SECURITY_CHECK_PREF = "sms_security_check_limit";

    private PreferenceScreen mWeatherPref;
    private PreferenceScreen mDelta;
    private PreferenceScreen mOta;
    private PreferenceScreen mAppRemover;
    private PreferenceScreen mLogCat;
    private SwitchPreference mSelinux;
    private Preference mSelinuxPref;
    private ListPreference mSmsSecurityCheck;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();

        //SELinux
        mSelinux = (SwitchPreference) findPreference(SELINUX);
        mAppRemover = (PreferenceScreen) findPreference(APP_REMOVER);
        mSelinuxPref = (Preference) findPreference(SELINX_PREF);

        mSmsSecurityCheck = (ListPreference) findPreference(KEY_SMS_SECURITY_CHECK_PREF);

       // Add package manager to check if features are available
        PackageManager pm = getActivity().getPackageManager();
        boolean isTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (!isTelephony) {
            getPreferenceScreen().removePreference(mSmsSecurityCheck);
        } else {
            mSmsSecurityCheck.setOnPreferenceClickListener(this);
            mSmsSecurityCheck.setOnPreferenceChangeListener(this);
            int smsSecurityCheck = Settings.Global.getInt(resolver, Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT, DEFAULT_SMS_MAX_COUNT);
            updateSmsSecuritySummary(smsSecurityCheck);
        }

        if (canSU()) {
            mSelinux.setOnPreferenceChangeListener(this);
            if (CMDProcessor.runShellCommand("getenforce").getStdout().contains("Enforcing")) {
                mSelinux.setChecked(true);
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else {
                mSelinux.setChecked(false);
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
        } else {
            if (mSelinux != null) 
                getPreferenceScreen().removePreference(mSelinux);
             if (mAppRemover != null)
                 getPreferenceScreen().removePreference(mAppRemover);
            if (mSelinuxPref != null) 
                getPreferenceScreen().removePreference(mSelinuxPref);
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

        PreferenceScreen mLogCat = (PreferenceScreen) findPreference("logcat_app");
        if (!PackageUtils.isAvailableApp(LOGCAT_PACKAGE, getActivity())) {
            getPreferenceScreen().removePreference(mLogCat);
        }
    }

    private void updateSmsSecuritySummary(int i) {
        if (i == 0) {
            String message = "Display an alert dialog to prevent applications from sending SMS messages too frequently. Current limit: Unlimited messages in 1 minute";
            mSmsSecurityCheck.setSummary(message);
        } else {        
            String message = getString(R.string.sms_security_check_limit_summary, i);
            mSmsSecurityCheck.setSummary(message);
        }
    }

    private boolean canSU() {
        Process process = null;
        int exitValue = -1;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream toProcess = new DataOutputStream(process.getOutputStream());
            toProcess.writeBytes("exec id\n");
            toProcess.flush();
            exitValue = process.waitFor();
        } catch (Exception e) {
            exitValue = -1;
            e.printStackTrace();
        }
        return exitValue == 0;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setSelinuxEnabled(String status) {
        SharedPreferences.Editor editor = getContext().getSharedPreferences("selinux_pref", Context.MODE_PRIVATE).edit();
        editor.putString("selinux", status);
        editor.apply();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Always returns true to prevent invoking an intent without catching exceptions.
        // See {@link Preference#performClick(PreferenceScreen)}/
        if (preference == mSmsSecurityCheck) {
            int smsSecurityCheck = Settings.Global.getInt(getActivity().getContentResolver(), Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT, DEFAULT_SMS_MAX_COUNT);
            String cConvert = String.valueOf(smsSecurityCheck);
            int index = mSmsSecurityCheck.findIndexOfValue((String) cConvert);
            mSmsSecurityCheck.setValueIndex(index);
            updateSmsSecuritySummary(smsSecurityCheck);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mSelinux) {
            if (value.toString().equals("true")) {
                CMDProcessor.runSuCommand("setenforce 1");
                setSelinuxEnabled("true");
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else if (value.toString().equals("false")) {
                CMDProcessor.runSuCommand("setenforce 0");
                setSelinuxEnabled("false");
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
            return true;
        } else if (preference == mSmsSecurityCheck) {
            int smsSecurityCheck = Integer.valueOf((String) value);
            Settings.Global.putInt(resolver, Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT, smsSecurityCheck);
            updateSmsSecuritySummary(smsSecurityCheck);
        }
        return false;
    }
}
