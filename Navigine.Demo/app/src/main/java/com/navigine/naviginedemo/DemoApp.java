package com.navigine.naviginedemo;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.util.*;
import java.lang.*;
import java.util.*;

public class DemoApp extends Application
{
  public static final String      TAG           = "Navigine.Demo";
  public static final String      USER_HASH     = "0000-0000-0000-0000";
  public static final String      LOCATION_NAME = "Navigine Office";
  
  public static Context           AppContext    = null;
  public static NavigationThread  Navigation    = null;
  
  // Screen parameters
  public static float DisplayWidthPx            = 0.0f;
  public static float DisplayHeightPx           = 0.0f;
  public static float DisplayWidthDp            = 0.0f;
  public static float DisplayHeightDp           = 0.0f;
  public static float DisplayDensity            = 0.0f;

  @Override public void onCreate()
  {
    super.onCreate();
  }
  
  public static boolean initialize(Context appContext)
  {
    NavigineSDK.setDebugLevel(2);
    if (!NavigineSDK.initialize(appContext, USER_HASH, null))
      return false;
    
    AppContext = appContext;
    Navigation = NavigineSDK.getNavigation();
    DisplayMetrics displayMetrics = AppContext.getResources().getDisplayMetrics();
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
  
  public static void finish()
  {
    if (AppContext == null || Navigation == null)
      return;
    
    NavigineSDK.finish();
    Navigation = null;
    AppContext = null;
  }
}
