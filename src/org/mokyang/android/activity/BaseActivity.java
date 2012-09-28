package org.mokyang.android.activity;

import org.mokyang.android.util.Logger;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

public class BaseActivity extends Activity {
	private static final String TAG = BaseActivity.class.getName();

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Logger.d(TAG, "onActivityResult()");
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Logger.d(TAG, "onAttachedToWindow()");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Logger.d(TAG, "onConfigurationChanged()");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.d(TAG, "onCreate()");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Logger.d(TAG, "onDestroy()");
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Logger.d(TAG, "onDetachedFromWindow()");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(TAG, "onPause()");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Logger.d(TAG, "onRestart()");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Logger.d(TAG, "onRestoreInstanceState()");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(TAG, "onResume()");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.d(TAG, "onSaveInstanceState()");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Logger.d(TAG, "onStart()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Logger.d(TAG, "onStop()");
	}

}
