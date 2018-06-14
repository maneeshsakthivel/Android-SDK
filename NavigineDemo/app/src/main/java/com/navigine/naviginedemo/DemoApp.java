package com.navigine.naviginedemo;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class DemoApp extends Application
{
  public static final String      TAG             = "NAVIGINE.Demo";
  public static final String      SERVER_URL      = "https://api.navigine.com";
  public static final String      USER_HASH       = "0000-0000-0000-0000";
  public static final int         LOCATION_ID     = 1603;
  public static final boolean     WRITE_LOGS      = false;
  
  public static NavigationThread  Navigation      = null;
  
  // Screen parameters
  public static float DisplayWidthPx              = 0.0f;
  public static float DisplayHeightPx             = 0.0f;
  public static float DisplayWidthDp              = 0.0f;
  public static float DisplayHeightDp             = 0.0f;
  public static float DisplayDensity              = 0.0f;
  
  @Override public void onCreate()
  {
    super.onCreate();
  }
  
  public static boolean initialize(Context context)
  {
    NavigineSDK.setParameter(context, "debug_level", 2);
    NavigineSDK.setParameter(context, "apply_server_config_enabled",  false);
    NavigineSDK.setParameter(context, "actions_updates_enabled",      false);
    NavigineSDK.setParameter(context, "location_updates_enabled",     true);
    NavigineSDK.setParameter(context, "location_loader_timeout",      60);
    NavigineSDK.setParameter(context, "location_update_timeout",      300);
    NavigineSDK.setParameter(context, "location_retry_timeout",       300);
    NavigineSDK.setParameter(context, "post_beacons_enabled",         true);
    NavigineSDK.setParameter(context, "post_messages_enabled",        true);
    if (!NavigineSDK.initialize(context, USER_HASH, SERVER_URL))
      return false;
    
    Navigation = NavigineSDK.getNavigation();
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    DisplayWidthPx  = displayMetrics.widthPixels;
    DisplayHeightPx = displayMetrics.heightPixels;
    DisplayDensity  = displayMetrics.density;
    DisplayWidthDp  = DisplayWidthPx  / DisplayDensity;
    DisplayHeightDp = DisplayHeightPx / DisplayDensity;
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Display size: %.1fpx x %.1fpx (%.1fdp x %.1fdp, density=%.2f)",
                             DisplayWidthPx, DisplayHeightPx,
                             DisplayWidthDp, DisplayHeightDp,
                             DisplayDensity));
    
    return true;
  }
  
  public static String getLogFile(String extension)
  {
    if (Navigation == null)
      return null;
    
    try
    {
      final String extDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Navigine.Demo";
      (new File(extDir)).mkdirs();
      if (!(new File(extDir)).exists())
        return null;
      
      final int locationId = Navigation.getLocationId();
      if (locationId == 0)
        return null;
      
      final String locationDir = extDir + "/" + locationId;
      (new File(locationDir)).mkdirs();
      if (!(new File(locationDir)).exists())
        return null;
      
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(System.currentTimeMillis());
      
      return String.format(Locale.ENGLISH, "%s/%04d%02d%02d_%02d%02d%02d.%s",
                           locationDir,
                           calendar.get(Calendar.YEAR),
                           calendar.get(Calendar.MONTH) + 1,
                           calendar.get(Calendar.DAY_OF_MONTH),
                           calendar.get(Calendar.HOUR_OF_DAY),
                           calendar.get(Calendar.MINUTE),
                           calendar.get(Calendar.SECOND),
                           extension);
    }
    catch (Throwable e)
    {
      return null;
    }
  }
  
  public static void finish()
  {
    if (Navigation == null)
      return;
    
    NavigineSDK.finish();
    Navigation = null;
  }
}
