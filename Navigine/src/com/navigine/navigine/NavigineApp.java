package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.util.*;

public class NavigineApp extends Application
{
  public static final String      TAG               = "NavigineApp";
  public static final String      DEFAULT_SERVER    = "server.navigine.com";
  public static final int         DEFAULT_SEND_PORT = 27015;
  public static final int         DEFAULT_RECV_PORT = 27016;
  
  public static Context           AppContext        = null;
  public static SharedPreferences Settings          = null;
  public static NavigationThread  Navigation        = null;
  public static SenderThread      Sender            = null;
  public static IMUThread         IMU               = null;
  
  @Override public void onCreate()
  {
    super.onCreate();
  }
  
  public static void initNavigation(Context appContext)
  {
    // Setting static parameters
    BeaconService.DEBUG_LEVEL     = 2;
    NativeUtils.DEBUG_LEVEL       = 2;
    LocationLoader.DEBUG_LEVEL    = 2;
    NavigationThread.DEBUG_LEVEL  = 2;
    MeasureThread.DEBUG_LEVEL     = 2;
    SensorThread.DEBUG_LEVEL      = 2;
    Parser.DEBUG_LEVEL            = 2;
    
    NavigationThread.STRICT_MODE  = false;
    LocationLoader.SERVER         = "api.navigine.com";
    
    try
    {
      AppContext = appContext;
      Settings   = AppContext.getSharedPreferences("NavigineSettings", 0);
      Navigation = new NavigationThread(Settings.getString("user_hash", ""), AppContext);
      Sender = new SenderThread();
      IMU = new IMUThread();    
    }
    catch (Throwable e)
    {
      Navigation = null;
      Log.e(TAG, Log.getStackTraceString(e));
      return;
    }
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Location directory: %s",
          LocationLoader.getLocationDir(AppContext, "")));
    
    if (AppContext == null || Navigation == null)
      return;
    
    // Loading map file
    String mapFile = Settings.getString("map_file", "");
    if (mapFile.length() > 0 && !Navigation.loadArchive(mapFile))
    {
      SharedPreferences.Editor editor = Settings.edit();
      editor.putString("map_file", "");
      editor.commit();
    }
  }
  
  public static String getLogFile(String extension)
  {
    if (AppContext == null || Navigation == null)
      return null;
    
    String archivePath = Navigation.getArchivePath();
    if (archivePath != null && archivePath != null)
    {
      for(int i = 1; true; ++i)
      {
        String suffix = String.format(Locale.ENGLISH, ".%d.%s", i, extension);
        String logFile = archivePath.replaceAll("\\.zip$", suffix);
        if ((new File(logFile)).exists())
          continue;
        return logFile;
      }
    }
    return null;
  }
  
  public static void startNavigation()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.loadArchive(Settings.getString("map_file", ""));
    
    if (Settings.getBoolean("navigation_log_enabled", false))
      Navigation.setLogFile(getLogFile("log"));
    else
      Navigation.setLogFile(null);
    
    if (Settings.getBoolean("navigation_track_enabled", false))
      Navigation.setTrackFile(getLogFile("txt"));
    else
      Navigation.setTrackFile(null);
    
    int mode = NavigationThread.MODE_NORMAL;
    
    if (Settings.getBoolean("navigation_file_enabled", false))
    {
      mode = NavigationThread.MODE_FILE;
      Navigation.setNavigationFile(Settings.getString("navigation_file", ""));
    }
    
    Navigation.setPostEnabled(Settings.getBoolean("post_messages_enabled", true));
    Navigation.setMode(mode);
    
    //if (Settings.getBoolean("navigation_server_enabled", false))
    //{
    //  String address = Settings.getString("navigation_server_address", "");
    //  if (address.length() > 0)
    //  {
    //    Sender.reconnect(address, DEFAULT_SEND_PORT, 10);
    //  }
    //}
  }
  
  public static void setBackgroundMode()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    switch (Navigation.getMode())
    {
      case NavigationThread.MODE_NORMAL:
      case NavigationThread.MODE_ECONOMIC1:
      case NavigationThread.MODE_ECONOMIC2:
        Navigation.setMode(Settings.getInt("background_navigation_mode", NavigationThread.MODE_NORMAL));
        break;
    }
  }
  
  public static void setForegroundMode()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    switch (Navigation.getMode())
    {
      case NavigationThread.MODE_NORMAL:
      case NavigationThread.MODE_ECONOMIC1:
      case NavigationThread.MODE_ECONOMIC2:
        Navigation.setMode(NavigationThread.MODE_NORMAL);
        break;
    }
  }
  
  public static void stopNavigation()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.setLogFile(null);
    Navigation.setTrackFile(null);
    Navigation.setMode(NavigationThread.MODE_IDLE);
    //Sender.idle();
    //if (IMU.getConnectionState() == IMUThread.STATE_NORMAL)
    //{
    //  Log.d(TAG, "Disconnecting from IMU");
    //  IMU.disconnect();
    //}
  }
  
  public static void startScanning()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.setLogFile(null);
    Navigation.setTrackFile(null);
    Navigation.setMode(NavigationThread.MODE_SCAN);
    //Sender.idle();
  }
  
  public static void stopScanning()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.setLogFile(null);
    Navigation.setTrackFile(null);
    Navigation.setMode(NavigationThread.MODE_IDLE);
    //Sender.idle();
  }
  
  public static void destroyNavigation()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Log.d(TAG, "Terminating IMU, Sender, Navigation threads!");
    IMU.terminate();
    Sender.terminate();
    Navigation.terminate();
    try
    {
      Log.d(TAG, "Joining with IMU thread");
      IMU.join();
      Log.d(TAG, "Joining with Sender thread");
      Sender.join();
      Log.d(TAG, "Joining with Navigation thread");
      Navigation.join();
    }
    catch (Throwable e)
    {
      Log.e(TAG, "Joining error!");
      Log.e(TAG, Log.getStackTraceString(e));
    }
    IMU = null;
    Sender = null;
    Navigation = null;
    AppContext = null;
  }
  
}
