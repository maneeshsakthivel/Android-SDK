package com.navigine.navigation_service_app;
import com.navigine.navigation_service.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView.*;
import android.widget.LinearLayout.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class NotificationActivity extends Activity
{
  private static final String TAG = "NAVIGINE.NotificationActivity";
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "NotificationActivity created");
    super.onCreate(savedInstanceState);
    
    // Initializing SDK
    Y.setDebugLevel(this, MainActivity.DEBUG_LEVEL);
    Y.initialize(getApplication(), null, null);
    
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.notification);
    
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                         WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    
    String title          = getIntent().getStringExtra("notification_title");
    String content        = getIntent().getStringExtra("notification_content");
    String imageUrl       = getIntent().getStringExtra("notification_image_url");
    String description    = getIntent().getStringExtra("notification_description");
    String expirationTime = getIntent().getStringExtra("notification_expiration_time");
    String barcode        = getIntent().getStringExtra("notification_barcode");
    
    Log.d(TAG, "notification_title = " + title);
    Log.d(TAG, "notification_content = " + content);
    Log.d(TAG, "notification_image_url = " + imageUrl);
    Log.d(TAG, "notification_description = " + description);
    Log.d(TAG, "notification_expiration_time = " + expirationTime);
    Log.d(TAG, "notification_barcode = " + barcode);
    
    WebView webView = (WebView)findViewById(R.id.notification__web_view);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setSupportZoom(true);
    webView.loadUrl(content);
    
    Y.reportEvent(String.format(Locale.ENGLISH, "Notification opened: %s", title));
    
    // Sending broadcast intent
    Intent intent = new Intent("com.navigine.navigation_service.NOTIFICATION_OPENED");
    intent.putExtra("notification_title",           title);
    intent.putExtra("notification_content",         content);
    intent.putExtra("notification_image_url",       imageUrl);
    intent.putExtra("notification_expiration_time", expirationTime);
    intent.putExtra("notification_barcode", barcode);
    sendBroadcast(intent);
  }
}
