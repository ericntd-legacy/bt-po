package sg.edu.dukenus.pononin;

import org.projectproto.yuscope.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;


public class ManageWakeLock {
	// Logging
	private static final String TAG = "ManageWakeLock";
	private static final boolean DEBUG = true;
	
  private static PowerManager.WakeLock mWakeLock = null;
  private static PowerManager.WakeLock mPartialWakeLock = null;
  private static final boolean PREFS_SCREENON_DEFAULT = true;
  private static final boolean PREFS_DIMSCREEN_DEFAULT = false;
  private static final String PREFS_TIMEOUT_DEFAULT = "30";

  public static synchronized void acquireFull(Context mContext) {
    if (mWakeLock != null) {
      if (DEBUG) Log.v(TAG,"**Wakelock already held");
      return;
    }

    PowerManager mPm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

    int flags;

    // Check dim screen preference
    if (mPrefs.getBoolean(
        mContext.getString(R.string.pref_dimscreen_key), PREFS_DIMSCREEN_DEFAULT)) {
      flags = PowerManager.SCREEN_DIM_WAKE_LOCK;
    } else {
      flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
    }

    // Check if screen should turn on, if so, set flags and unlock keyguard
    if (mPrefs.getBoolean(mContext.getString(R.string.pref_screen_on_key), PREFS_SCREENON_DEFAULT)) {
      flags |= PowerManager.ACQUIRE_CAUSES_WAKEUP;
      ManageKeyguard.disableKeyguard(mContext);
    }

    mWakeLock = mPm.newWakeLock(flags, TAG+".full");
    mWakeLock.setReferenceCounted(false);
    mWakeLock.acquire();
    if (DEBUG) Log.v(TAG,"**Wakelock acquired");

    // Fetch wakelock/screen timeout from preferences
    int timeout =
      Integer.valueOf(
          mPrefs.getString(mContext.getString(R.string.pref_timeout_key), PREFS_TIMEOUT_DEFAULT));

    // Set a receiver to remove all locks in "timeout" seconds
    ClearAllReceiver.setCancel(mContext, timeout);
  }

  public static synchronized void acquirePartial(Context mContext) {
    // Check if partial lock already exists
    if (mPartialWakeLock != null) return;

    PowerManager mPm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

    // TODO: this should be partial wake lock, but that seems to be causing issues with the cpu
    // sleeping so changed to screen bright wake lock
    mPartialWakeLock = mPm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG + ".partial");
    //mPartialWakeLock = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG + ".partial");
    if (DEBUG) Log.v(TAG,"**Wakelock (partial) acquired");
    mPartialWakeLock.setReferenceCounted(false);
    mPartialWakeLock.acquire();
  }

  public static synchronized void releaseFull() {
    if (mWakeLock != null) {
      if (DEBUG) Log.v(TAG,"**Wakelock released");
      mWakeLock.release();
      mWakeLock = null;
    }
  }

  public static synchronized void releasePartial() {
    if (mPartialWakeLock != null) {
      if (DEBUG) Log.v(TAG,"**Wakelock (partial) released");
      mPartialWakeLock.release();
      mPartialWakeLock = null;
    }
  }

  public static synchronized void releaseAll() {
    releaseFull();
    releasePartial();
  }

}
