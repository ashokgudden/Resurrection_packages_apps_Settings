/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Activity;
import android.app.Fragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.telephony.TelephonyManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;
import static com.android.settings.notification.SettingPref.TYPE_SYSTEM;

public class OtherSoundSettings extends SettingsPreferenceFragment implements Indexable,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "OtherSoundSettings";

    private static final int DEFAULT_ON = 1;

    private static final int DLG_SAFE_HEADSET_VOLUME = 0; 

    private static final int EMERGENCY_TONE_SILENT = 0;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_VIBRATE = 2;
    private static final int DEFAULT_EMERGENCY_TONE = EMERGENCY_TONE_SILENT;

    private static final int DOCK_AUDIO_MEDIA_DISABLED = 0;
    private static final int DOCK_AUDIO_MEDIA_ENABLED = 1;
    private static final int DEFAULT_DOCK_AUDIO_MEDIA = DOCK_AUDIO_MEDIA_DISABLED;

    private static final String KEY_SAFE_HEADSET_VOLUME = "safe_headset_volume"; 

    private static final String KEY_DIAL_PAD_TONES = "dial_pad_tones";
    private static final String KEY_SCREEN_LOCKING_SOUNDS = "screen_locking_sounds";
    private static final String KEY_DOCKING_SOUNDS = "docking_sounds";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String KEY_TOUCH_SOUNDS = "touch_sounds";
    private static final String KEY_VIBRATE_ON_TOUCH = "vibrate_on_touch";
    private static final String KEY_DOCK_AUDIO_MEDIA = "dock_audio_media";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String PREF_LESS_NOTIFICATION_SOUNDS = "less_notification_sounds";
    private static final String KEY_CAMERA_SOUNDS = "camera_sounds";
    private static final String PROP_CAMERA_SOUND = "persist.sys.camera-sound";

    private ListPreference mAnnoyingNotifications;
    private SwitchPreference mCameraSounds;

    private SwitchPreference mSafeHeadsetVolume; 

    // Boot Sounds needs to be a system property so it can be accessed during boot.
    private static final String KEY_BOOT_SOUNDS = "boot_sounds";
    private static final String PROPERTY_BOOT_SOUNDS = "persist.sys.bootanim.play_sound";

    private static final String KEY_POWER_NOTIFICATIONS = "power_notifications";
    private static final String KEY_POWER_NOTIFICATIONS_VIBRATE = "power_notifications_vibrate";
    private static final String KEY_POWER_NOTIFICATIONS_RINGTONE = "power_notifications_ringtone";

    // Request code for power notification ringtone picker
    private static final int REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE = 1;

    // Used for power notification uri string if set to silent
    private static final String POWER_NOTIFICATIONS_SILENT_URI = "silent";

    private ListPreference mPowerSounds;
    private SwitchPreference mPowerSoundsVibrate;
    private Preference mPowerSoundsRingtone;

    private static final SettingPref PREF_DIAL_PAD_TONES = new SettingPref(
            TYPE_SYSTEM, KEY_DIAL_PAD_TONES, System.DTMF_TONE_WHEN_DIALING, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return Utils.isVoiceCapable(context);
        }
    };

    private static final SettingPref PREF_SCREEN_LOCKING_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_SCREEN_LOCKING_SOUNDS, System.LOCKSCREEN_SOUNDS_ENABLED, DEFAULT_ON);

    private static final SettingPref PREF_DOCKING_SOUNDS = new SettingPref(
            TYPE_GLOBAL, KEY_DOCKING_SOUNDS, Global.DOCK_SOUNDS_ENABLED, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return hasDockSettings(context);
        }
    };

    private static final SettingPref PREF_VOLUME_ADJUST_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_VOLUME_ADJUST_SOUNDS, System.VOLUME_ADJUST_SOUNDS_ENABLED,
            DEFAULT_ON) {
    };

    private static final SettingPref PREF_TOUCH_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_TOUCH_SOUNDS, System.SOUND_EFFECTS_ENABLED, DEFAULT_ON) {
        @Override
        protected boolean setSetting(final Context context, final int value) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final AudioManager am =
                            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (value != 0) {
                        am.loadSoundEffects();
                    } else {
                        am.unloadSoundEffects();
                    }
                }
            });
            return super.setSetting(context, value);
        }
    };

    private static final SettingPref PREF_VIBRATE_ON_TOUCH = new SettingPref(
            TYPE_SYSTEM, KEY_VIBRATE_ON_TOUCH, System.HAPTIC_FEEDBACK_ENABLED, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return hasHaptic(context);
        }
    };

    private static final SettingPref PREF_DOCK_AUDIO_MEDIA = new SettingPref(
            TYPE_GLOBAL, KEY_DOCK_AUDIO_MEDIA, Global.DOCK_AUDIO_MEDIA_ENABLED,
            DEFAULT_DOCK_AUDIO_MEDIA, DOCK_AUDIO_MEDIA_DISABLED, DOCK_AUDIO_MEDIA_ENABLED) {
        @Override
        public boolean isApplicable(Context context) {
            return hasDockSettings(context);
        }

        @Override
        protected String getCaption(Resources res, int value) {
            switch(value) {
                case DOCK_AUDIO_MEDIA_DISABLED:
                    return res.getString(R.string.dock_audio_media_disabled);
                case DOCK_AUDIO_MEDIA_ENABLED:
                    return res.getString(R.string.dock_audio_media_enabled);
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    private static final SettingPref PREF_EMERGENCY_TONE = new SettingPref(
            TYPE_GLOBAL, KEY_EMERGENCY_TONE, Global.EMERGENCY_TONE, DEFAULT_EMERGENCY_TONE,
            EMERGENCY_TONE_ALERT, EMERGENCY_TONE_VIBRATE, EMERGENCY_TONE_SILENT) {
        @Override
        public boolean isApplicable(Context context) {
            final int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
            return activePhoneType == TelephonyManager.PHONE_TYPE_CDMA;
        }

        @Override
        protected String getCaption(Resources res, int value) {
            switch(value) {
                case EMERGENCY_TONE_SILENT:
                    return res.getString(R.string.emergency_tone_silent);
                case EMERGENCY_TONE_ALERT:
                    return res.getString(R.string.emergency_tone_alert);
                case EMERGENCY_TONE_VIBRATE:
                    return res.getString(R.string.emergency_tone_vibrate);
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    private static final SettingPref[] PREFS = {
        PREF_DIAL_PAD_TONES,
        PREF_SCREEN_LOCKING_SOUNDS,
        PREF_DOCKING_SOUNDS,
        PREF_VOLUME_ADJUST_SOUNDS,
        PREF_TOUCH_SOUNDS,
        PREF_VIBRATE_ON_TOUCH,
        PREF_DOCK_AUDIO_MEDIA,
        PREF_EMERGENCY_TONE,
    };

    private SwitchPreference mBootSounds;

    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private Context mContext;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_OTHER_SOUND;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_other_sounds;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.other_sound_settings);

        mContext = getActivity();

        mAnnoyingNotifications = (ListPreference) findPreference(PREF_LESS_NOTIFICATION_SOUNDS);
        int notificationThreshold = System.getInt(getContentResolver(),
                System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, 0);
        mAnnoyingNotifications.setValue(Integer.toString(notificationThreshold));
        mAnnoyingNotifications.setOnPreferenceChangeListener(this);
 
        mCameraSounds = (SwitchPreference) findPreference(KEY_CAMERA_SOUNDS);
        mCameraSounds.setChecked(SystemProperties.getBoolean(PROP_CAMERA_SOUND, true));
        mCameraSounds.setOnPreferenceChangeListener(this);

        mSafeHeadsetVolume = (SwitchPreference) findPreference(KEY_SAFE_HEADSET_VOLUME);
        mSafeHeadsetVolume.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
				Settings.System.SAFE_HEADSET_VOLUME, 1) != 0);
        mSafeHeadsetVolume.setOnPreferenceChangeListener(this);

        // power state change notification sounds
        mPowerSounds = (ListPreference) findPreference(KEY_POWER_NOTIFICATIONS);
        int mPowerSoundsValue =
                Global.getInt(getContentResolver(), Global.POWER_NOTIFICATIONS_ENABLED, 0);
        mPowerSounds.setValue(Integer.toString(mPowerSoundsValue));
        mPowerSounds.setSummary(mPowerSounds.getEntry());
        mPowerSounds.setOnPreferenceChangeListener(this);

        mPowerSoundsVibrate = (SwitchPreference) findPreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        mPowerSoundsVibrate.setChecked(Global.getInt(getContentResolver(),
                Global.POWER_NOTIFICATIONS_VIBRATE, 0) != 0);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        }

        mPowerSoundsRingtone = findPreference(KEY_POWER_NOTIFICATIONS_RINGTONE);
        String currentPowerRingtonePath =
                Global.getString(getContentResolver(), Global.POWER_NOTIFICATIONS_RINGTONE);

        // set to default notification if we don't yet have one
        if (currentPowerRingtonePath == null) {
                currentPowerRingtonePath = System.DEFAULT_NOTIFICATION_URI.toString();
                Global.putString(getContentResolver(),
                        Global.POWER_NOTIFICATIONS_RINGTONE, currentPowerRingtonePath);
        }
        // is it silent ?
        if (currentPowerRingtonePath.equals(POWER_NOTIFICATIONS_SILENT_URI)) {
            mPowerSoundsRingtone.setSummary(
                    getString(R.string.power_notifications_ringtone_silent));
        } else {
            final Ringtone ringtone =
                    RingtoneManager.getRingtone(getActivity(), Uri.parse(currentPowerRingtonePath));
            if (ringtone != null) {
                mPowerSoundsRingtone.setSummary(ringtone.getTitle(getActivity()));
            }
        }

        for (SettingPref pref : PREFS) {
            pref.init(this);
        }

        if (mContext.getResources().getBoolean(R.bool.has_boot_sounds)) {
            mBootSounds = (SwitchPreference) findPreference(KEY_BOOT_SOUNDS);
            mBootSounds.setChecked(SystemProperties.getBoolean(PROPERTY_BOOT_SOUNDS, true));
        } else {
            removePreference(KEY_BOOT_SOUNDS);
        }

        final CMHardwareManager hardware = CMHardwareManager.getInstance(mContext);
        if (!hardware.isSupported(CMHardwareManager.FEATURE_VIBRATOR)) {
            removePreference(CMSettings.Secure.VIBRATOR_INTENSITY);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.register(false);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SAFE_HEADSET_VOLUME.equals(key)) {
            if ((Boolean) objValue) {
                    Settings.System.putInt(getActivity().getContentResolver(),
                            Settings.System.SAFE_HEADSET_VOLUME, 1);
            } else {
                    showDialogInner(DLG_SAFE_HEADSET_VOLUME);
            }
        }
        if (PREF_LESS_NOTIFICATION_SOUNDS.equals(key)) {
            final int val = Integer.valueOf((String) objValue);
            System.putInt(getContentResolver(),
                    System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, val);
        } else if (preference == mCameraSounds) {
               if (KEY_CAMERA_SOUNDS.equals(key)) {
                   if ((Boolean) objValue) {
                       SystemProperties.set(PROP_CAMERA_SOUND, "1");
                   } else {
                       SystemProperties.set(PROP_CAMERA_SOUND, "0");
                   }
                }
                return true;
        } else if (KEY_POWER_NOTIFICATIONS.equals(key)) {
            int mPowerSoundsValue = Integer.valueOf((String) objValue);
            Global.putInt(getContentResolver(),
                    Global.POWER_NOTIFICATIONS_ENABLED,
                    mPowerSoundsValue);
            int index = mPowerSounds.findIndexOfValue((String) objValue);
            mPowerSounds.setSummary(mPowerSounds.getEntries()[index]);
            mPowerSoundsVibrate.setEnabled(index > 0);
            mPowerSoundsRingtone.setEnabled(index > 0);
        }
        return true;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        OtherSoundSettings getOwner() {
            return (OtherSoundSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
                switch (id) {
                   case DLG_SAFE_HEADSET_VOLUME:
                       return new AlertDialog.Builder(getActivity())
                           .setTitle(R.string.attention)
                           .setMessage(R.string.safe_headset_volume_warning_dialog_text)
                           .setPositiveButton(R.string.ok,
                               new DialogInterface.OnClickListener() {
                                   public void onClick(DialogInterface dialog, int which) {
                                       Settings.System.putInt(getOwner().getActivity().getContentResolver(),
                                       Settings.System.SAFE_HEADSET_VOLUME, 0);
                                   }
                               })
                           .setNegativeButton(R.string.cancel,
                               new DialogInterface.OnClickListener() {
                                   public void onClick(DialogInterface dialog, int which) {
                                       dialog.cancel();
                                   }
                               })
                           .create();
                }
                   throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
                switch (id) {
                   case DLG_SAFE_HEADSET_VOLUME:
                       getOwner().mSafeHeadsetVolume.setChecked(true);
                       break;
                   }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mBootSounds != null && preference == mBootSounds) {
            SystemProperties.set(PROPERTY_BOOT_SOUNDS, mBootSounds.isChecked() ? "1" : "0");
            return false;
        } else if (preference == mPowerSoundsVibrate) {
            Global.putInt(getContentResolver(),
                    Global.POWER_NOTIFICATIONS_VIBRATE,
                    mPowerSoundsVibrate.isChecked() ? 1 : 0);
            return false;
        } else if (preference == mPowerSoundsRingtone) {
            launchNotificationSoundPicker(REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE,
                    Global.getString(getContentResolver(),
                            Global.POWER_NOTIFICATIONS_RINGTONE));
	        return false;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    private static boolean hasDockSettings(Context context) {
        return context.getResources().getBoolean(R.bool.has_dock_settings);
    }

    private static boolean hasHaptic(Context context) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return vibrator != null && vibrator.hasVibrator();
    }

    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                for (SettingPref pref : PREFS) {
                    cr.registerContentObserver(pref.getUri(), false, this);
                }
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            for (SettingPref pref : PREFS) {
                if (pref.getUri().equals(uri)) {
                    pref.update(mContext);
                    return;
                }
            }
        }
    }

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.other_sound_settings;
            return Arrays.asList(sir);
        }

        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> rt = new ArrayList<String>();
            for (SettingPref pref : PREFS) {
                if (!pref.isApplicable(context)) {
                    rt.add(pref.getKey());
                }
            }
            return rt;
        }
    };

    private void launchNotificationSoundPicker(int code, String currentPowerRingtonePath) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.power_notifications_ringtone_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                System.DEFAULT_NOTIFICATION_URI);
        if (currentPowerRingtonePath != null &&
                !currentPowerRingtonePath.equals(POWER_NOTIFICATIONS_SILENT_URI)) {
            Uri uri = Uri.parse(currentPowerRingtonePath);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            }
        }
        startActivityForResult(intent, code);
    }

    private void setPowerNotificationRingtone(Intent intent) {
        final Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        final String toneName;
        final String toneUriPath;

        if ( uri != null ) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            toneName = ringtone.getTitle(getActivity());
            toneUriPath = uri.toString();
        } else {
            // silent
            toneName = getString(R.string.power_notifications_ringtone_silent);
            toneUriPath = POWER_NOTIFICATIONS_SILENT_URI;
        }

        mPowerSoundsRingtone.setSummary(toneName);
        Global.putString(getContentResolver(),
                Global.POWER_NOTIFICATIONS_RINGTONE, toneUriPath);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE:
                if (resultCode == Activity.RESULT_OK) {
                    setPowerNotificationRingtone(data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
