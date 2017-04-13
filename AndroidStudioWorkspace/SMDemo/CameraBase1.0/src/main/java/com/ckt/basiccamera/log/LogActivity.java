package com.ckt.basiccamera.log;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * * @author qianghua.song
 */
public class LogActivity extends Activity {
    protected String TAG = getClass().getSimpleName() + "Log";
    private long time;

    public void onCreate(Bundle savedInstanceState) {
        time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    public void onStop() {
        super.onStart();
        Log.d(TAG, "onStop");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
    }

    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        Log.d(TAG, "onAttachFragment");
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged:" + hasFocus);
    }
}
