package com.navigine.geo_service_app;
import com.navigine.geo_service.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class BootReceiver extends BroadcastReceiver
{
  private static final String TAG = "GeoService";
  
  public void onReceive(Context context, Intent intent)
  {
    final String action = intent.getAction();
    
    if (action.equals("android.intent.action.BOOT_COMPLETED") ||
        action.equals("android.intent.action.PACKAGE_REPLACED"))
    {
      // Initializing GeoService parameters
      GeoService.setParameter(context, "debug_level",      Default.DEBUG_LEVEL);
      GeoService.setParameter(context, "gps_scan_timeout", Default.GPS_SCAN_TIMEOUT);
      GeoService.setParameter(context, "wake_frequency",   Default.WAKE_FREQUENCY);
      
      // Starting GeoService
      GeoService.startService(context);
    }
  }
}
