package com.navigine.geo_service;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.util.*;

public class MainActivity extends Activity
{
  public static final String TAG = "GeoService";
  
  private Button    mStartButton    = null;
  private TextView  mTextView       = null;
  private TimerTask mTimerTask      = null;
  private Handler   mHandler        = new Handler();
  private Timer     mTimer          = new Timer();
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    mTextView    = (TextView)findViewById(R.id.text_view);
    mStartButton = (Button)findViewById(R.id.start_button);
    mStartButton.setEnabled(false);
    
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, 1000, 100);
  }
  
  @Override public void onDestroy()
  {
    super.onDestroy();
  }
  
  public void onToggleService(View v)
  {
    if (GeoService.isStarted())
    {
      Log.d(TAG, "Stopping GeoService");
      GeoService.requestToStop();
    }
    else
    {
      Log.d(TAG, "Starting GeoService");
      startService(new Intent(getApplicationContext(), GeoService.class));
    }
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        SharedPreferences settings = getSharedPreferences("GeoService", 0);
        long time = settings.getLong("location_time", 0L);
        double latitude  = Double.longBitsToDouble(settings.getLong("location_latitude", 0L));
        double longitude = Double.longBitsToDouble(settings.getLong("location_longitude", 0L));
        double accuracy  = Double.longBitsToDouble(settings.getLong("location_accuracy", 0L));
        String provider  = settings.getString("location_provider", "");
        
        if (time > 0)
        {
          StringBuilder builder = new StringBuilder();
          builder.append(String.format(Locale.ENGLISH, "Provider:  \t %s\n", provider));
          builder.append(String.format(Locale.ENGLISH, "Latitude:  \t %.8f°\n", latitude));
          builder.append(String.format(Locale.ENGLISH, "Longitude: \t %.8f°\n", longitude));
          builder.append(String.format(Locale.ENGLISH, "Accuracy:  \t %.2f m\n", accuracy));
          builder.append(String.format(Locale.ENGLISH, "Time ago:  \t %d sec\n", (System.currentTimeMillis() - time) / 1000));
          mTextView.setText(builder.toString());
        }
        
        if (GeoService.isStarted())
        {
          mStartButton.setText("Stop service");
          mStartButton.setEnabled(true);
        }
        else
        {
          mStartButton.setText("Start service");
          mStartButton.setEnabled(true);
        }
      }
    };  
}
