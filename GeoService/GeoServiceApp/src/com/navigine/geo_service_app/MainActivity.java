package com.navigine.geo_service_app;
import com.navigine.geo_service.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.location.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import java.util.*;

public class MainActivity extends Activity
{
  private static final String TAG = "GeoServiceApp";
  
  private Context   mContext      = null;
  private Button    mStartButton  = null;
  private TextView  mTextView     = null;
  private TimerTask mTimerTask    = null;
  private Handler   mHandler      = new Handler();
  private Timer     mTimer        = new Timer();
  
  private BroadcastReceiver mLocationReceiver = null;
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    // Initializing GeoService parameters
    GeoService.setParameter(this, "debug_level",      Default.DEBUG_LEVEL);
    GeoService.setParameter(this, "gps_scan_timeout", Default.GPS_SCAN_TIMEOUT);
    GeoService.setParameter(this, "wake_frequency",   Default.WAKE_FREQUENCY);
    
    mContext     = getApplicationContext();
    mTextView    = (TextView)findViewById(R.id.text_view);
    mStartButton = (Button)findViewById(R.id.start_button);
    
    mLocationReceiver =
      new BroadcastReceiver()
      {
        @Override public void onReceive(Context ctxt, Intent intent)
        {
          try
          {
            final int id = intent.getIntExtra("id", 0);
            Location location = intent.getParcelableExtra("location");
            
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(location.getTime());
            
            mTextView.setText(String.format(Locale.ENGLISH,
                                            "ID:       \t\t\t\t\t %d\n" +
                                            "Time:     \t\t\t %04d-%02d-%02d %02d:%02d:%02d (UTC)\n" +
                                            "Latitude: \t\t %.8f\n" +
                                            "Longitude:\t %.8f\n" +
                                            "Accuracy: \t %.2f\n", id,
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH) + 1,
                                            calendar.get(Calendar.DAY_OF_MONTH),
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            calendar.get(Calendar.SECOND),
                                            location.getLatitude(),
                                            location.getLongitude(),
                                            location.getAccuracy()));
          }
          catch (Throwable e)
          { }
        }
      };
    registerReceiver(mLocationReceiver, new IntentFilter("com.navigine.geo_service.LOCATION_UPDATE"));
    
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, 100, 100);
  }
  
  @Override public void onDestroy()
  {
    unregisterReceiver(mLocationReceiver);
    super.onDestroy();
  }
  
  @Override public void onStart()
  {
    Log.d(TAG, "Set parameter: active_mode_enabled=true");
    GeoService.setParameter(this, "active_mode_enabled", true);
    super.onStart();
  }
  
  @Override public void onStop()
  {
    Log.d(TAG, "Set parameter: active_mode_enabled=false");
    GeoService.setParameter(this, "active_mode_enabled", false);
    super.onStop();
  }
  
  public void onToggleService(View v)
  {
    if (GeoService.isStarted(mContext))
      GeoService.stopService(mContext);
    else
      GeoService.startService(mContext);
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (GeoService.isStarted(mContext))
          mStartButton.setText("Stop service");
        else
          mStartButton.setText("Start service");
      }
    };  
}
