package com.navigine.geo_service;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.widget.Toast;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class GeoService extends IntentService
{
  private static final String TAG = "GeoService";
  
  // Debug levels:
  //  0 - no debug
  //  1 - errors
  //  2 - api functions
  //  3 - beacon updates & pushes
  public static int DEBUG_LEVEL   = 3;
  
  private static boolean mStarted = false;
  private static boolean mStopped = false;
  
  public static long   mLocationTime = 0;
  public static double mLatitude  = 0.0f;
  public static double mLongitude = 0.0f;
  public static String mProvider  = "";
  
  private Handler mHandler = null;
  
  // Check if service has been started
  public static boolean isStarted()
  {
    return mStarted;
  }
  
  // Ask service to stop (service will be stopped as eventually)
  public static void requestToStop()
  {
    if (mStarted)
      mStopped = true;
  }
  
  public GeoService()
  {
    super("GeoService");
    mHandler = new Handler();
  }
  
  private PendingIntent mPendingIntent = null;
  
  @Override public void onCreate()
  {
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, "GeoService: created");
    
    Intent serviceIntent = new Intent(getApplicationContext(), GeoService.class);
    mPendingIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent,
                                              PendingIntent.FLAG_UPDATE_CURRENT);
    
    AlarmManager alarmService = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    if (alarmService != null)
      alarmService.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime(),
                                60 * 1000, mPendingIntent);
    mStarted = true;
    mStopped = false;
    super.onCreate();
  }
  
  @Override public void onDestroy()
  {
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, "GeoService: destroyed");
    
    mStarted = false;
    mStopped = false;
    super.onDestroy();
  }
  
  private LocationManager   mLocationManager  = null;
  private LocationListener  mLocationListener = null;
  
  @Override protected void onHandleIntent(Intent intent)
  {
    try
    {
      // Setting up location manager
      mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
      if (mLocationManager != null)
      {
        try
        {
          mLocationListener = new LocationListener()
          {
            @Override public void onLocationChanged(android.location.Location location) { }
            @Override public void onProviderDisabled(String provider) { }
            @Override public void onProviderEnabled(String provider)  { }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
          };
          
          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,     1000, 0.0f, mLocationListener);
          mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0.0f, mLocationListener);
        }
        catch (Throwable e)
        {
          if (DEBUG_LEVEL >= 1)
            Log.e(TAG, Log.getStackTraceString(e));
          mLocationManager  = null;
          mLocationListener = null;
        }
      }
      
      while (!mStopped)
      {
        try {Thread.sleep(1000); } catch (Throwable e) { }        
        
        if (mLocationManager == null)
          continue;
        
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null)
          location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location == null)
          continue;
        
        updateLocation(location);
      }
      AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
      if (alarmService != null)
        alarmService.cancel(mPendingIntent);
    }
    catch (Throwable e)
    {
      Log.e(TAG, "Navigation service caught unhandled exception! Restarting service!");
      if (DEBUG_LEVEL >= 1)
        Log.e(TAG, Log.getStackTraceString(e));
      restartService(10);
    }
  }
  
  @Override public void onTaskRemoved(Intent rootIntent)
  {
    restartService(10);
    super.onTaskRemoved(rootIntent); 
  }
  
  private void restartService(int timeout)
  {
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, "GeoService will be restarted in " + timeout + " seconds!");
    Intent serviceIntent = new Intent(getApplicationContext(), GeoService.class);
    PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent,
                                                           PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager alarmService = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    alarmService.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                     SystemClock.elapsedRealtime() + timeout * 1000,
                     pendingIntent);
  }
  
  private void updateLocation(Location location)
  {
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    long   time       = location.getTime();
    double latitude   = location.getLatitude();
    double longitude  = location.getLongitude();
    double accuracy   = location.getAccuracy();
    String provider   = location.getProvider();
    
    long timeOld = settings.getLong("location_time", 0L);
    if (time != timeOld)
    {
      SharedPreferences.Editor editor = settings.edit();
      editor.putLong("location_time", time);
      editor.putLong("location_latitude",  Double.doubleToRawLongBits(latitude));
      editor.putLong("location_longitude", Double.doubleToRawLongBits(longitude));
      editor.putLong("location_accuracy",  Double.doubleToRawLongBits(accuracy));
      editor.putString("location_provider", provider);
      editor.commit();
      
      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      calendar.setTimeInMillis(System.currentTimeMillis());
      logMessage(String.format(Locale.ENGLISH, "%04d-%02d-%02d %02d:%02d:%02d: latitude=%.8f; longitude=%.8f; accuracy=%.2f; provider=%s",
                 calendar.get(Calendar.YEAR),
                 calendar.get(Calendar.MONTH) + 1,
                 calendar.get(Calendar.DAY_OF_MONTH),
                 calendar.get(Calendar.HOUR_OF_DAY),
                 calendar.get(Calendar.MINUTE),
                 calendar.get(Calendar.SECOND),
                 latitude, longitude, accuracy,
                 provider));
    }
  }
  
  private void logMessage(String message)
  {
    final String filename = getExternalFilesDir(null).getAbsolutePath() + "/GeoService.log";
    
    try
    {
      if (DEBUG_LEVEL >= 2)
        Log.d(TAG, message);
      
      FileWriter fw = new FileWriter(filename, true);
      fw.write(message);
      fw.write("\n");
      fw.close();
    }
    catch (Throwable e)
    {
      if (DEBUG_LEVEL >= 1)
      {
        Log.e(TAG, Log.getStackTraceString(e));
        Log.e(TAG, "Unable to open/write output log file " + filename);
      }
    }
  }
}
