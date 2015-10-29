package com.navigine.navigine;
import com.navigine.navigine.*;

import android.app.*;
import android.content.*;
import android.database.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.*;
import android.text.method.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.ImageView.*;
import android.widget.LinearLayout.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class BeaconActivity extends Activity
{
  // Constants
  private static final String TAG = "Navigine.BeaconActivity";
  
  // This context
  private final Context context = this;
  
  // GUI parameters
  private TextView mTextView = null;
  
  private String _imagePath = null;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "BeaconActivity created");
    super.onCreate(savedInstanceState);
    
    String title     = getIntent().getStringExtra("beacon_action_title");
    String content   = getIntent().getStringExtra("beacon_action_content");
    String imageUrl  = getIntent().getStringExtra("beacon_action_image_url");
    String imagePath = getIntent().getStringExtra("beacon_action_image_path");
    
    _imagePath = imagePath;
    
    boolean imageCached = false;
    File imageFile = new File(imagePath);
    if (imageFile.exists())
      imageCached = true;
    
    Log.d(TAG, "imageUrl=" + imageUrl);
    Log.d(TAG, "imagePath=" + imagePath);
    Log.d(TAG, "Image cached: " + imageCached);
    
    String htmlText = String.format(Locale.ENGLISH,
                                    "<html><body>" +
                                    "<img src=\"%s\"/></body></html>",
                                    imageCached ? imagePath : imageUrl);
    
    Html.ImageGetter imageGetter =
      new Html.ImageGetter()
      {
        @Override public Drawable getDrawable(String source)
        {
          Display display = getWindowManager().getDefaultDisplay();
          Point size = new Point();
          display.getSize(size);
          int width  = size.x;
          int height = size.y;
          
          try
          {
            Bitmap bitmap = null;
            if (source.startsWith("http://"))
            {
              // Url
              Log.d(TAG, String.format(Locale.ENGLISH, "Downloading image '%s'", source));
              String tmpPath = _imagePath + ".tmp";
              URL url = new URL(source);
              InputStream is = url.openStream();
              OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpPath));
              int count = 0;
              byte[] data = new byte[32768];
              while ((count = is.read(data)) != -1)
                os.write(data, 0, count);
              os.close();
              
              (new File(tmpPath)).renameTo(new File(_imagePath));
              Log.d(TAG, String.format(Locale.ENGLISH, "Image saved to file '%s'", _imagePath));
              bitmap = BitmapFactory.decodeFile(_imagePath);
            }
            else
            {
              // Regular file
              bitmap = BitmapFactory.decodeFile(source, null);
            }
            
            //Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
            //                                                Math.min(width, bitmap.getWidth()),
            //                                                Math.min(height / 2, bitmap.getHeight()),
            //                                                true);
            int scaledWidth  = width;
            int scaledHeight = bitmap.getHeight() * width / bitmap.getWidth();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
            Drawable d = new BitmapDrawable(context.getResources(), scaledBitmap);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            return d;
          }
          catch (Throwable e)
          {
            Log.e(TAG, Log.getStackTraceString(e));
          }
          return null;
        }
      };
    
    // Setting up GUI parameters
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    
    TextView textView = new TextView(this);
    textView.setText(Html.fromHtml(htmlText, imageGetter, null));
    textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                              LayoutParams.WRAP_CONTENT));
    
    TextView textView2 = new TextView(this);
    textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    textView2.setText(Html.fromHtml(String.format(Locale.ENGLISH, "<html><body><h2>%s</h2>%s</body></html>", title, content)));
    textView2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                               LayoutParams.WRAP_CONTENT));
    
    Button closeButton = new Button(this);
    closeButton.setText("Close");
    closeButton.setWidth(150);
    closeButton.setHeight(60);
    closeButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          finish();
        }
      });
    
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.addView(textView);
    layout.addView(textView2);
    layout.addView(closeButton);
    setContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
                                            LayoutParams.MATCH_PARENT));
    
    
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "BeaconActivity destroyed");
    super.onDestroy();
  }
  
  @Override public void onStart()
  {
    Log.d(TAG, "BeaconActivity started");
    super.onStart();
  }
  
  @Override public void onStop()
  {
    Log.d(TAG, "BeaconActivity stopped");
    super.onStop();
    finish();
    
    //Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
    //restartServiceIntent.setPackage(getPackageName());
    //
    //PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
    //AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    //alarmService.set(AlarmManager.ELAPSED_REALTIME,
    //                 SystemClock.elapsedRealtime() + 1000,
    //                 restartServicePendingIntent);
  }
  
  @Override public void onPause()
  {
    Log.d(TAG, "BeaconActivity paused");
    super.onPause();
  }
  
  @Override public void onResume()
  {
    Log.d(TAG, "BeaconActivity resumed");
    super.onResume();
  }
}
