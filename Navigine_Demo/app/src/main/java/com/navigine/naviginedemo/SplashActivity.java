package com.navigine.naviginedemo;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import java.lang.*;
import java.io.*;
import java.util.*;

import com.navigine.naviginesdk.*;

public class SplashActivity extends Activity
{
  private static final String TAG = "Navigine.Demo";
  private static final int LOADER_TIMEOUT = 30; // seconds
  
  private TextView  mErrorLabel   = null;
  private TimerTask mTimerTask    = null;
  private Handler   mHandler      = new Handler();
  private Timer     mTimer        = new Timer();
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "SplashActivity created");
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_splash);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                         WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

    mErrorLabel = (TextView)findViewById(R.id.splash__error_label);
    mErrorLabel.setVisibility(View.GONE);
    
    // Starting interface updates
    mTimerTask =
      new TimerTask()
      {
        @Override public void run()
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, 500, 500);
  }

  @Override public void onBackPressed()
  {
    moveTaskToBack(true);
  }
  
  private int  mLoader       = 0;
  private int  mLoaderState  = 0;
  private long mLoaderTime   = 0;
  
  private void updateLoader()
  {
    if (DemoApp.Navigation == null)
      return;
    
    long timeNow = NavigineSDK.currentTimeMillis() / 1000;
    
    if (mLoader == 0)
    {
      mLoader      = NavigineSDK.startLocationLoader(DemoApp.LOCATION_NAME, false);
      mLoaderTime  = timeNow;
      mLoaderState = 0;
      Log.d(TAG, "Location loader: STARTED");
    }
    else
    {
      int state = NavigineSDK.checkLoader(mLoader);
      if (state >= 0 && state <= 99)
      {
        if (Math.abs(timeNow - mLoaderTime) > LOADER_TIMEOUT)
        {
          NavigineSDK.stopLoader(mLoader);
          mLoader      = 0;
          mLoaderTime  = 0;
          mLoaderState = 0;
          Log.d(TAG, "Location loader: TIMEOUT");
          return;
        }
        if (mLoaderState != state)
        {
          mLoaderTime  = timeNow;
          mLoaderState = state;
        }
        Log.d(TAG, "Location loader: progress " + state + "%");
        return;
      }
      NavigineSDK.stopLoader(mLoader);
      mLoader      = 0;
      mLoaderTime  = 0;
      mLoaderState = 0;
      
      if (state == 100)
        Log.d(TAG, "Location loader: FINISHED");
      else
        Log.d(TAG, "Location loader: FAILED with error=" + state);
    }
  }
  
  private int  mVenueLoader     = 0;
  private long mVenueLoaderTime = 0;
  
  private void updateVenueLoader()
  {
    if (DemoApp.Navigation == null)
      return;
    
    final long timeNow      = NavigineSDK.currentTimeMillis() / 1000;
    final String venuesFile = NavigineSDK.getLocationDir(DemoApp.LOCATION_NAME) + "/venues.xml";
    final String venuesUrl  = String.format(Locale.ENGLISH, "https://api.navigine.com/venues?locationId=%d&format=xml&userHash=%s",
                                            DemoApp.LOCATION_ID, DemoApp.USER_HASH);
    
    if (mVenueLoader == 0)
    {
      mVenueLoader      = NavigineSDK.startUrlLoader(venuesUrl, venuesFile);
      mVenueLoaderTime  = timeNow;
      Log.d(TAG, "Venue loader: STARTED");
    }
    else
    {
      int state = NavigineSDK.checkLoader(mVenueLoader);
      if (state >= 0 && state <= 99)
      {
        if (Math.abs(timeNow - mVenueLoaderTime) > LOADER_TIMEOUT)
        {
          NavigineSDK.stopLoader(mVenueLoader);
          mVenueLoader      = 0;
          mVenueLoaderTime  = 0;
          Log.d(TAG, "Venue loader: TIMEOUT");
        }
        return;
      }
      NavigineSDK.stopLoader(mVenueLoader);
      mVenueLoader      = 0;
      mVenueLoaderTime  = 0;
      
      if (state == 100)
        Log.d(TAG, "Venue loader: FINISHED");
      else
        Log.d(TAG, "Venue loader: FAILED with error=" + state);
    }
  }
  
  boolean mInitialized = false;
  boolean mFinished    = false;
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (mFinished)
          return;

        if (!mInitialized)
        {
          mInitialized = true;
          if (DemoApp.initialize(getApplicationContext()))
          {
            updateLoader();
            updateVenueLoader();
          }
          else
            mErrorLabel.setVisibility(View.VISIBLE);
          return;
        }

        if (DemoApp.Navigation == null)
          return;

        if (mLoader > 0)
          updateLoader();
        
        if (mVenueLoader > 0)
          updateVenueLoader();
        
        if (mLoader == 0 && mVenueLoader == 0)
        {
          final String archiveFile = NavigineSDK.getLocationFile(DemoApp.LOCATION_NAME);
          if ((new File(archiveFile)).exists())
          {
            // Starting main activity
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            mTimerTask.cancel();
            mTimerTask = null;
            mFinished = true;
          }
        }
      }
    };
}
