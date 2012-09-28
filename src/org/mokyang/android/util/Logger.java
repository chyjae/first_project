package org.mokyang.android.util;

import org.mokyang.android.Const;

import android.util.Log;

public class Logger {
	static private boolean DEBUG = Const.DEBUG;

	static public void d(String tag, String msg) {
		if (DEBUG) {
			Log.d(tag, msg);
		}
	}

	static public void i(String tag, String msg) {
		if (DEBUG) {
			Log.i(tag, msg);
		}
	}

	static public void e(String tag, String msg) {
		if (DEBUG) {
			Log.e(tag, msg);
		}
	}

	static public void w(String tag, String msg) {
		if (DEBUG) {
			Log.w(tag, msg);
		}
	}
}
