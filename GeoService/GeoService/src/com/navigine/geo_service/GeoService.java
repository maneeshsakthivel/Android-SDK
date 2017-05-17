package com.navigine.geo_service;
import com.navigine.geo_service.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.telephony.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class GeoService extends IntentService
{
  public static final String TAG              = "GeoService";
  public static final int WAKE_FREQUENCY      = 60;     // seconds, how often should service be waked up?
  public static final int SEND_FREQUENCY      = 300;    // seconds, how often should messages be sent?
  public static final int SEND_TIMEOUT        = 30;     // seconds, how much time should wait for sending data?
  public static final int GPS_SCAN_TIMEOUT    = 10;     // seconds, how much time should wait for GPS-coordinates?
  public static final int MAX_CACHE_SIZE      = 1000;   // maximum number of cached messages
  
  private static boolean  mStarted = false;
  
  private Handler mHandler = null;
  private int mDebugLevel     = 0;
  private int mGpsScanTimeout = GPS_SCAN_TIMEOUT;
  private int mMaxCacheSize   = MAX_CACHE_SIZE;
  private int mSendFrequency  = SEND_FREQUENCY;
  private int mSendTimeout    = SEND_TIMEOUT;
  private String mSendUrl     = null;
  
  // Check if service has been started
  public static boolean isStarted(Context context)
  {
    Intent serviceIntent = new Intent("com.navigine.geo_service.SERVICE_WAKE");
    return (PendingIntent.getBroadcast(context, 0, serviceIntent, PendingIntent.FLAG_NO_CREATE) != null);
  }
  
  public static boolean isWakeUp(Context context)
  {
    return mStarted;
  }
  
  public static void startService(Context context)
  {
    context.sendBroadcast(new Intent("com.navigine.geo_service.SERVICE_START"));
  }
  
  public static void stopService(Context context)
  {
    context.sendBroadcast(new Intent("com.navigine.geo_service.SERVICE_STOP"));
    mStarted = false;
  }
  
  public long getTimeStamp(String key)
  {
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    return settings.getLong("timestamp__" + key, 0);
  }
  
  public void setTimeStamp(String key, long timeMillis)
  {
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putLong("timestamp__" + key, timeMillis);
    editor.commit();
  }
  
  public GeoService()
  {
    super("GeoService");
    mHandler = new Handler();
  }
  
  @Override public void onCreate()
  {
    mDebugLevel     = getParameter(this, "debug_level",      0);
    mGpsScanTimeout = getParameter(this, "gps_scan_timeout", GPS_SCAN_TIMEOUT);
    mMaxCacheSize   = getParameter(this, "max_cache_size",   MAX_CACHE_SIZE);
    mSendFrequency  = getParameter(this, "send_frequency",   SEND_FREQUENCY);
    mSendTimeout    = getParameter(this, "send_timeout",     SEND_TIMEOUT);
    mSendUrl        = getParameter(this, "send_url",         null);
    
    String logFile = null;
    try
    {
      logFile = getExternalFilesDir(null).getAbsolutePath() + "/GeoService.log";
    }
    catch (Throwable e)
    {
      logFile = getFilesDir().getAbsolutePath() + "/GeoService.log";
    }
    
    Logger.setLogFile(logFile);
    Logger.setLogLevel(mDebugLevel);
    Logger.d(TAG, 3, "GeoService created");
    mStarted = true;
    
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    if (!settings.contains("device_id"))
    {
      String deviceId = getDeviceId();
      if (deviceId != null)
      {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("device_id", deviceId);
        editor.commit();
      }
    }
    
    super.onCreate();
  }
  
  @Override public void onDestroy()
  {
    if (mLocationManager != null)
      mLocationManager.removeUpdates(mLocationListener);
    
    Logger.d(TAG, 3, "GeoService destroyed");
    mStarted = false;
    super.onDestroy();
  }
  
  @Override public void onTaskRemoved(Intent rootIntent)
  {
    if (mLocationManager != null)
      mLocationManager.removeUpdates(mLocationListener);
    
    Logger.d(TAG, 3, "GeoService task removed");
    mStarted = false;
    super.onTaskRemoved(rootIntent); 
  }
  
  private LocationListener  mLocationListener = null;
  private LocationManager   mLocationManager  = null;
  
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
          
          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, mLocationListener);
          mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0.0f, mLocationListener);
        }
        catch (Throwable e)
        {
          Logger.d(TAG, 1, Log.getStackTraceString(e));
          mLocationManager  = null;
          mLocationListener = null;
        }
      }
  
      AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>()
      {
        @Override protected Boolean doInBackground(Void... params)
        {
          try
          {
            Set<String> hashSet = getCache();
            if (hashSet == null)
              return Boolean.TRUE;
            
            if (mSendUrl == null)
              return Boolean.TRUE;
            
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            for(String s: hashSet)
            {
              String[] parts = s.split(":");
              if (parts.length >= 4)
                builder.append(String.format(Locale.ENGLISH, "{\"time\":\"%s\",\"latitude\":\"%s\",\"longitude\":\"%s\",\"accuracy\":\"%s\"},",
                                             parts[0], parts[1], parts[2], parts[3]));
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]");
            
            Logger.d(TAG, 2, "Sending data: " + builder.toString());
            
            SharedPreferences settings = getSharedPreferences("GeoService", 0);
            String deviceId = settings.getString("device_id", "");
            
            MultipartUtility multipart = new MultipartUtility(mSendUrl);
            multipart.addFormField("deviceId", deviceId);
            multipart.addFormField("data", builder.toString());
            List<String> response = multipart.finish();
            clearCache();
            Logger.d(TAG, 2, "Sending data: FINISHED");
          }
          catch (Throwable e)
          {
            Logger.d(TAG, 2, Log.getStackTraceString(e));
            Logger.d(TAG, 2, "Sending data: FAILED");
            return Boolean.FALSE;
          }
          return Boolean.TRUE;
        }
        
        @Override protected void onCancelled(Boolean result)
        {
          Logger.d(TAG, 2, "Sending data: CANCELLED");
        }
      };
      
      long timeNow  = System.currentTimeMillis();
      long stopTime = timeNow + mGpsScanTimeout * 1000;
      Location location = null, locationGps = null, locationNet = null;
      
      while (mStarted && timeNow < stopTime)
      {
        try { Thread.sleep(1000); } catch ( Throwable e) { }
        timeNow  = System.currentTimeMillis();
        if ((location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)) != null)     locationGps = location;
        if ((location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) != null) locationNet = location;
      }
      
      updateLocation(locationGps);
      updateLocation(locationNet);
      
      if (!mStarted)
        return;
      
      long sendTimeStamp = getTimeStamp("send_message");
      timeNow  = System.currentTimeMillis();
      stopTime = timeNow + mSendTimeout * 1000;
      
      if (timeNow < sendTimeStamp)
        return;
      
      setTimeStamp("send_message", timeNow + mSendFrequency * 1000);
      task.execute();
      
      while (mStarted && timeNow < stopTime && task.getStatus() != AsyncTask.Status.FINISHED)
      {
        try { Thread.sleep(1000); } catch ( Throwable e) { }
        timeNow = System.currentTimeMillis();
      }
      
      if (task.getStatus() != AsyncTask.Status.FINISHED)
        task.cancel(true);
      
      try { Thread.sleep(1000); } catch ( Throwable e) { }
    }
    catch (Throwable e)
    {
      Logger.d(TAG, 1, "Caught unhandled exception!");
      Logger.d(TAG, 1, Log.getStackTraceString(e));
    }
  }
  
  private void updateLocation(android.location.Location location)
  {
    if (location == null)
      return;
    
    final long   time       = location.getTime()/1000;
    final double latitude   = location.getLatitude();
    final double longitude  = location.getLongitude();
    final double accuracy   = location.getAccuracy();
    final String provider   = location.getProvider();
    
    final String hash = String.format(Locale.ENGLISH, "%010d:%.8f:%.8f:%.2f:%s",
                                      time, latitude, longitude, accuracy, provider);
    
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    String locationHash = settings.getString("location_hash", null);    
    if (locationHash == null || hash.compareTo(locationHash) > 0)
      addToCache(hash);
  }
  
  private synchronized Set<String> getCache()
  {
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    return settings.getStringSet("location_strings", null);
  }
  
  private synchronized void addToCache(String hash)
  {
    Logger.d(TAG, 2, "Location update: " + hash);
    
    String[] parts = hash.split(":");
    if (parts.length >= 4)
    {
      Intent intent = new Intent("com.navigine.geo_service.LOCATION_UPDATE");
      intent.putExtra("time",       parts[0]);
      intent.putExtra("latitude",   parts[1]);
      intent.putExtra("longitude",  parts[2]);
      intent.putExtra("accuracy",   parts[3]);
      sendBroadcast(intent);
    }
    
    TreeSet<String> hashTreeSet = new TreeSet<String>();
    
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    Set<String> hashSet = settings.getStringSet("location_strings", null);
    if (hashSet != null)
      hashTreeSet.addAll(hashSet);
    
    hashTreeSet.add(hash);
    if (hashTreeSet.size() > MAX_CACHE_SIZE)
      hashTreeSet.pollFirst();
    
    SharedPreferences.Editor editor = settings.edit();
    editor.putString("location_hash", hash);
    editor.putStringSet("location_strings", hashTreeSet);
    editor.commit();
  }
  
  private synchronized void clearCache()
  {
    SharedPreferences settings = getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putStringSet("location_strings", null);
    editor.commit();
  }
  
  private String getDeviceId()
  {
    try
    {
      TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
      return telephonyManager.getDeviceId();
    }
    catch (Throwable e)
    {
      Logger.d(TAG, 1, Log.getStackTraceString(e));
      Logger.d(TAG, 1, "Unable to get device IMEI, using random generated ID");
      return null;
    }
  }
  
  public static int getParameter(Context context, String name, int defaultValue)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    return settings.getInt(name, defaultValue);
  }
  
  public static long getParameter(Context context, String name, long defaultValue)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    return settings.getLong(name, defaultValue);
  }
  
  public static boolean getParameter(Context context, String name, boolean defaultValue)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    return settings.getBoolean(name, defaultValue);
  }
  
  public static String getParameter(Context context, String name, String defaultValue)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    return settings.getString(name, defaultValue);
  }
  
  public static void setParameter(Context context, String name, int value)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putInt(name, value);
    editor.commit();
  }
  
  public static void setParameter(Context context, String name, long value)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putLong(name, value);
    editor.commit();
  }
  
  public static void setParameter(Context context, String name, boolean value)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putBoolean(name, value);
    editor.commit();
  }
  
  public static void setParameter(Context context, String name, String value)
  {
    SharedPreferences settings = context.getSharedPreferences("GeoService", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(name, value);
    editor.commit();
  }
  
}
