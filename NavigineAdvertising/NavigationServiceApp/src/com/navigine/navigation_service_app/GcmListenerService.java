package com.navigine.navigation_service_app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService
{
  final static String TAG = "NavigationService";
  @Override public void onMessageReceived(String from, Bundle data)
  {
    // Extracting data from Bundle
    String  identifier          = data.getString("id");
    String  title               = data.getString("title");
    String  contentUrl          = data.getString("content");
    String  description         = data.getString("description");
    String  imageUrl            = data.getString("image");
    String  barcode             = data.getString("barcode");
    String  expirationDate      = data.getString("expirationDate");
    String  expirationDateLabel = data.getString("expirationDateLabel");
    
    int id = 1;
    try { id = Integer.parseInt(identifier); } catch (Throwable e) { }
    
    Log.d(TAG, "GCM notification received");
    Log.d(TAG, "id = " + id);
    Log.d(TAG, "title = " + title);
    Log.d(TAG, "content  = " + contentUrl);
    Log.d(TAG, "imageUrl = " + imageUrl);
    Log.d(TAG, "barcode  = " + barcode);
    Log.d(TAG, "expirationDate = " + expirationDate);
    Log.d(TAG, "expirationDateLabel = " + expirationDateLabel);
    
    try
    {
      Context context = getApplicationContext();
      Intent intent = new Intent(context, NotificationActivity.class);
      intent.putExtra("notification_title", title);
      intent.putExtra("notification_content", contentUrl);
      intent.putExtra("notification_image_url", imageUrl);
      intent.putExtra("notification_description", description);
      intent.putExtra("notification_expiration_time", expirationDate);
      intent.putExtra("notification_barcode", barcode);
      
      PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      
      Notification.Builder notificationBuilder = new Notification.Builder(context);
      notificationBuilder.setSmallIcon(R.drawable.notification);
      notificationBuilder.setContentTitle(title);
      notificationBuilder.setContentText(description);
      notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
      notificationBuilder.setAutoCancel(true);
      notificationBuilder.setContentIntent(pendingIntent);
      
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
