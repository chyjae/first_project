package org.mokyang.android;

import org.mokyang.android.util.Logger;

import android.app.Application;

public class MokyangApplication extends Application {
	private static final String TAG = MokyangApplication.class.getName();

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "onCreate()");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		Logger.d(TAG, "onTerminate()");
	}

}
