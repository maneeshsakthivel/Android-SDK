package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnDismissListener;
import android.database.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.*;
import android.text.method.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.ImageView.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class SplashActivity extends Activity
{
  // Constants
  private static final String TAG = "NAVIGINE.SplashActivity";
  
  // This context
  private final Context mContext = this;
  
  private TimerTask mTimerTask   = null;
  private Handler   mHandler     = new Handler();
  private Timer     mTimer       = new Timer();
  
  boolean mInitialized = false;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "SplashActivity created");
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.splash);
    
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                         WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "SplashActivity destroyed");
    super.onStart();
  }
  
  @Override public void onStart()
  {
    Log.d(TAG, "SplashActivity started");
    super.onStart();
    
    // Starting interface updates
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          update();
        }
      };
    mTimer.schedule(mTimerTask, 500, 500);
  }
  
  @Override public void onStop()
  {
    Log.d(TAG, "SplashActivity stopped");
    super.onStop();
    
    if (mTimerTask != null)
    {
      mTimerTask.cancel();
      mTimerTask = null;
    }
  }
  
  @Override public void onBackPressed()
  {
    moveTaskToBack(true);
  }
  
  private void update()
  {
    mHandler.post(mRunnable);
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (!mInitialized)
        {
          NavigineApp.initialize(getApplicationContext());
          mInitialized = true;
          return;
        }
        else
        {
          // Starting loader activity
          Intent intent = new Intent(mContext, LoaderActivity.class);
          startActivity(intent);
          if (mTimerTask != null)
          {
            mTimerTask.cancel();
            mTimerTask = null;
          }
        }
      }
    };
}
