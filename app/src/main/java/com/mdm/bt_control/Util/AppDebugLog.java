package com.mdm.bt_control.Util;

public class AppDebugLog {

	public static final boolean LOG_ENABLED = true;

	private static final String TAG = "[Autosearching]";

	public static void i(String tag, String msg) {
		if (LOG_ENABLED) {
			android.util.Log.i(TAG + tag, msg);
		}
	}

	public static void i(String tag, String msg, Throwable e) {
		if (LOG_ENABLED) {
			android.util.Log.i(TAG + tag, msg, e);
		}
	}

	public static void d(String tag, String msg) {
		if (LOG_ENABLED) {
			android.util.Log.d(TAG + tag, msg);
		}
	}

	public static void d(String tag, String msg, Throwable e) {
		if (LOG_ENABLED) {
			android.util.Log.d(TAG + tag, msg, e);
		}
	}

	public static void w(String tag, String msg) {
		if (LOG_ENABLED) {
			android.util.Log.w(TAG + tag, msg);
		}
	}

	public static void w(String tag, String msg, Throwable e) {
		if (LOG_ENABLED) {
			android.util.Log.w(TAG + tag, msg, e);
		}
	}

	public static void e(String tag, String msg) {
		if (LOG_ENABLED) {
			android.util.Log.e(TAG + tag, msg);
		}
	}

	public static void e(String tag, String msg, Throwable e) {
		if (LOG_ENABLED) {
			android.util.Log.e(TAG + tag, msg, e);
		}
	}

}
