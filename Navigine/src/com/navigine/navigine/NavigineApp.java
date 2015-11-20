package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;
import com.navigine.imu.*;

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
  public static final String      DEFAULT_SERVER    = "https://api.navigine.com";
  public static final String      DEFAULT_USER_HASH = "0000-0000-0000-0000";
  
  public static Context           AppContext        = null;
  public static SharedPreferences Settings          = null;
  public static NavigationThread  Navigation        = null;
  public static IMU_Thread        IMU               = null;
  public static int               IMU_Location      = 0;
  public static int               IMU_SubLocation   = 0;
  
  @Override public void onCreate()
  {
    super.onCreate();
        
    registerReceiver(
      new BroadcastReceiver()
      {
        @Override public void onReceive(Context context, Intent intent)
        {
          int id           = intent.getIntExtra("beacon_action_id", 0);
          String title     = intent.getStringExtra("beacon_action_title");
          String content   = intent.getStringExtra("beacon_action_content");
          String imageUrl  = intent.getStringExtra("beacon_action_image_url");
          postNotification(id, title, content, imageUrl);
        }
      },
      new IntentFilter("com.navigine.navigine.beacon_action")
    );
  }
  
  public static void initialize(Context appContext)
  {
    // Setting static parameters
    BeaconService.DEBUG_LEVEL     = 2;
    NativeUtils.DEBUG_LEVEL       = 2;
    LocationLoader.DEBUG_LEVEL    = 2;
    NavigationThread.DEBUG_LEVEL  = 2;
    MeasureThread.DEBUG_LEVEL     = 2;
    SensorThread.DEBUG_LEVEL      = 2;
    Parser.DEBUG_LEVEL            = 2;
    
    NavigationThread.STRICT_MODE  = true;
    
    try
    {
      AppContext = appContext;
      Settings   = AppContext.getSharedPreferences("NavigineSettings", 0);
      Navigation = new NavigationThread(Settings.getString("user_hash", ""), AppContext);
      IMU = new IMU_Thread();    
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
    
    applySettings();
  }
  
  public static void applySettings()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    // Setting up server parameters
    String address = Settings.getString("location_server_address", NavigineApp.DEFAULT_SERVER);
    String userHash = Settings.getString("user_hash", NavigineApp.DEFAULT_USER_HASH);
    LocationLoader.initialize(AppContext, userHash, address);
    
    // Setting up BeaconService
    BeaconService.setUserHash(userHash);
    BeaconService.setLocationId(Navigation.getLocation() == null ?
                                0 : NavigineApp.Navigation.getLocation().id);
    
    if (!Settings.getBoolean("beacon_service_enabled", true))
    {
      BeaconService.requestToStop();
      return;
    }
    
    if (!BeaconService.isStarted())
    {
      Log.d(TAG, "Starting BeaconService");
      NavigineApp.AppContext.startService(new Intent(AppContext, BeaconService.class));
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
    
    if (IMU.getConnectionState() == IMU_Thread.STATE_NORMAL)
    {
      Log.d(TAG, "Disconnecting from IMU");
      IMU.disconnect();
    }
  }
  
  public static void startScanning()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.setLogFile(null);
    Navigation.setTrackFile(null);
    Navigation.setMode(NavigationThread.MODE_SCAN);
  }
  
  public static void stopScanning()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Navigation.setLogFile(null);
    Navigation.setTrackFile(null);
    Navigation.setMode(NavigationThread.MODE_IDLE);
  }
  
  public static void destroyNavigation()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    Log.d(TAG, "Terminating IMU, Navigation threads!");
    IMU.terminate();
    Navigation.terminate();
    try
    {
      Log.d(TAG, "Joining with IMU thread");
      IMU.join();
      Log.d(TAG, "Joining with Navigation thread");
      Navigation.join();
    }
    catch (Throwable e)
    {
      Log.e(TAG, "Joining error!");
      Log.e(TAG, Log.getStackTraceString(e));
    }
    IMU = null;
    Navigation = null;
    AppContext = null;
  }
  
  public static DeviceInfo getDeviceInfoByIMU(IMU_Device imuDevice)
  {
    if (AppContext == null || Navigation == null)
      return null;
    
    long timeNow = DateTimeUtils.currentTimeMillis();
    
    DeviceInfo device = new DeviceInfo();
    device.id = Navigation.getDeviceId();
    device.type = "android";
    device.time = DateTimeUtils.currentDate(timeNow);
    device.location = IMU_Location;
    device.subLocation = IMU_SubLocation;
    device.x = imuDevice.x;
    device.y = imuDevice.y;
    device.z = imuDevice.z;
    device.r = 2;
    device.azimuth = imuDevice.angle;
    device.timeLabel = timeNow;
    return device;
  }
  
  public static void postNotification(int id, String title, String content, String imageUrl)
  {
    try
    {
      Context context = BeaconService.getContext();
      Log.d(TAG, String.format(Locale.ENGLISH, "Post notification: id=%d, title='%s', content='%s', imageUrl='%s'",
                               id, title, content, imageUrl));
      
      String imagePath = String.format(Locale.ENGLISH, "%s/image-beacon-action-%d.png",
                                       context.getCacheDir().getPath(), id);
      
      Intent intent = new Intent(context, BeaconActivity.class);
      intent.putExtra("beacon_action_title", title);
      intent.putExtra("beacon_action_content", content);
      intent.putExtra("beacon_action_image_url", imageUrl);
      intent.putExtra("beacon_action_image_path", imagePath);
      
      PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      
      Notification.Builder notificationBuilder = new Notification.Builder(context);
      notificationBuilder.setSmallIcon(R.drawable.notification);
      notificationBuilder.setContentTitle(title);
      notificationBuilder.setContentText(content);
      notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
      notificationBuilder.setAutoCancel(true);
      notificationBuilder.setContentIntent(pendingIntent);
      
      File imageFile = new File(imagePath);
      if (imageFile.exists())
      {
        Bitmap imageBitmap = BitmapFactory.decodeFile(imagePath);
        notificationBuilder.setLargeIcon(imageBitmap);
      }
      
      // Get an instance of the NotificationManager service
      NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
      
      // Build the notification and issues it with notification manager.
      notificationManager.notify(id, notificationBuilder.build());
    }
    catch (Throwable e)
    {
      Log.e(TAG, Log.getStackTraceString(e));
    }
  }
}
