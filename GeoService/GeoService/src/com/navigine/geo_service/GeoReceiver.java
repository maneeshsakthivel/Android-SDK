package com.navigine.geo_service;
import com.navigine.geo_service.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class GeoReceiver extends BroadcastReceiver
{
  private static final String TAG = "GeoService";
  
  public void onReceive(Context context, Intent intent)
  {
    final String action = intent.getAction();
    Logger.d(TAG, 3, "Received " + action);
    
    final int wakeFrequency = GeoService.getParameter(context, "wake_frequency", GeoService.WAKE_FREQUENCY);
    
    if (action.equals("com.navigine.geo_service.SERVICE_START"))
    {
      Logger.d(TAG, 3, "GeoService will be waked every " + wakeFrequency + " secs!");
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                                                               new Intent("com.navigine.geo_service.SERVICE_WAKE"),
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
      AlarmManager alarmService = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      if (alarmService != null)
        alarmService.setRepeating(AlarmManager.RTC_WAKEUP,
                                  System.currentTimeMillis() + 5000,
                                  wakeFrequency * 1000, pendingIntent);
    }
    else if (action.equals("com.navigine.geo_service.SERVICE_STOP"))
    {
      Logger.d(TAG, 3, "GeoService will be stopped!");
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                                                               new Intent("com.navigine.geo_service.SERVICE_WAKE"),
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
      AlarmManager alarmService = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
      if (alarmService != null)
        alarmService.cancel(pendingIntent);
      pendingIntent.cancel();
    }
    else if (action.equals("com.navigine.geo_service.SERVICE_WAKE"))
    {
      context.startService(new Intent(context, com.navigine.geo_service.GeoService.class));
    }
  }
}
