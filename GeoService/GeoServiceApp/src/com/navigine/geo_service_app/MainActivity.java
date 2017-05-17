package com.navigine.geo_service_app;
import com.navigine.geo_service.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import java.util.*;

public class MainActivity extends Activity
{
  private Context   mContext        = null;
  private Button    mStartButton    = null;
  private TextView  mTextView       = null;
  private TimerTask mTimerTask      = null;
  private Handler   mHandler        = new Handler();
  private Timer     mTimer          = new Timer();
  
  private BroadcastReceiver mLocationReceiver = null;
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    // Initializing GeoService parameters
    GeoService.setParameter(this, "debug_level",      Default.DEBUG_LEVEL);
    GeoService.setParameter(this, "gps_scan_timeout", Default.GPS_SCAN_TIMEOUT);
    GeoService.setParameter(this, "max_cache_size",   Default.MAX_CACHE_SIZE);
    GeoService.setParameter(this, "wake_frequency",   Default.WAKE_FREQUENCY);
    GeoService.setParameter(this, "send_frequency",   Default.SEND_FREQUENCY);
    GeoService.setParameter(this, "send_timeout",     Default.SEND_TIMEOUT);
    GeoService.setParameter(this, "send_url",         Default.SEND_URL);
    
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
            final String latitude  = intent.getStringExtra("latitude");
            final String longitude = intent.getStringExtra("longitude");
            final String accuracy  = intent.getStringExtra("accuracy");
            final String timeStr   = intent.getStringExtra("time");
            final long   time      = Long.parseLong(timeStr) * 1000;
            
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(time);
            
            mTextView.setText(String.format(Locale.ENGLISH,
                                            "Time:     \t\t\t %04d-%02d-%02d %02d:%02d:%02d (UTC)\n" +
                                            "Latitude: \t\t %s\n" +
                                            "Longitude:\t %s\n" +
                                            "Accuracy: \t %s\n",
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH) + 1,
                                            calendar.get(Calendar.DAY_OF_MONTH),
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            calendar.get(Calendar.SECOND),
                                            latitude, longitude, accuracy));
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
