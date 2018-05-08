package com.navigine.naviginedemo;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import java.lang.*;
import java.io.*;
import java.util.*;

import com.navigine.naviginesdk.*;

public class SplashActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback
{
  private static final String TAG = "NAVIGINE.Demo";
  
  private Context   mContext     = null;
  private TextView  mStatusLabel = null;
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    mContext = getApplicationContext();
    
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_splash);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                         WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

    mStatusLabel = (TextView)findViewById(R.id.splash__status_label);
    
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                                                           Manifest.permission.ACCESS_COARSE_LOCATION,
                                                           Manifest.permission.READ_EXTERNAL_STORAGE,
                                                           Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
  }

  @Override public void onBackPressed()
  {
    moveTaskToBack(true);
  }
  
  @Override public void onRequestPermissionsResult(int requestCode,
                                                   String permissions[],
                                                   int[] grantResults)
  {
    boolean permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED &&
                                 ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    boolean permissionStorage  = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)  == PackageManager.PERMISSION_GRANTED &&
                                 ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    switch (requestCode)
    {
      case 101:
        if (!permissionLocation || (DemoApp.WRITE_LOGS && !permissionStorage))
          finish();
        else
        {
          if (DemoApp.initialize(getApplicationContext()))
          {
            NavigineSDK.loadLocationInBackground(DemoApp.LOCATION_ID, 30,
              new Location.LoadListener()
              {
                @Override public void onFinished(int locationId)
                {
                  Intent intent = new Intent(mContext, MainActivity.class);
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  mContext.startActivity(intent);
                }
                @Override public void onFailed(int locationId, int error)
                {
                  mStatusLabel.setText("Error downloading location 'Navigine Demo' (error " + error + ")! " +
                                       "Please, try again later or contact technical support");
                }
                @Override public void onUpdate(int locationId, int progress)
                {
                  mStatusLabel.setText("Downloading location: " + progress + "%");
                }
              });
          }
          else
          {
            mStatusLabel.setText("Error initializing NavigineSDK! Please, contact technical support");
          }
        }
        break;
    }
  }
}
