package com.navigine.navigation_service_app;
import com.navigine.navigation_service.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class BootReceiver extends BroadcastReceiver
{
  private static final String TAG = "NavigationService";
  
  public void onReceive(Context context, Intent intent)
  {
    final String action = intent.getAction();
    
    Log.d(TAG, "Received " + action);
    Log.d(TAG, "NavigationService will be started in 60 secs!");
    
    //
    // NB: set service params here (user hash and debug level)
    //
    Y.setDebugLevel(context, MainActivity.DEBUG_LEVEL);
    
    Intent serviceIntent = new Intent(context, NavigationService.class);
    serviceIntent.setPackage(context.getPackageName());
    
    PendingIntent pendingIntent = PendingIntent.getService(context, 1, serviceIntent, PendingIntent.FLAG_ONE_SHOT);
    AlarmManager alarmService = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    alarmService.set(AlarmManager.ELAPSED_REALTIME,
                     SystemClock.elapsedRealtime() + 60000,
                     pendingIntent);
  }
}
