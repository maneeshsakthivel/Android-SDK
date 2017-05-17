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
      GeoService.setParameter(context, "max_cache_size",   Default.MAX_CACHE_SIZE);
      GeoService.setParameter(context, "wake_frequency",   Default.WAKE_FREQUENCY);
      GeoService.setParameter(context, "send_frequency",   Default.SEND_FREQUENCY);
      GeoService.setParameter(context, "send_timeout",     Default.SEND_TIMEOUT);
      GeoService.setParameter(context, "send_url",         Default.SEND_URL);
      
      // Starting GeoService
      GeoService.startService(context);
    }
  }
}
