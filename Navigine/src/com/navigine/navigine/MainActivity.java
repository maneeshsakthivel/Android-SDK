package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;

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
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity
{
  // Constants
  private static final String TAG = "Navigine.MainActivity";
  
  // This context
  private final Context context = this;
  
  // GUI parameters
  private Button mLocationManagementButton = null;
  private Button mNavigationModeButton = null;
  private Button mMeasuringModeButton  = null;
  private Button mTextModeButton       = null;
  private Button mSettingsButton       = null;
  
  private boolean mBeaconServiceStarted = false;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "MainActivity created");
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);
    
    // Setting up GUI parameters
    mLocationManagementButton = (Button)findViewById(R.id.location_management_button);
    mNavigationModeButton = (Button)findViewById(R.id.navigation_mode_button);
    mMeasuringModeButton  = (Button)findViewById(R.id.measuring_mode_button);
    mTextModeButton       = (Button)findViewById(R.id.text_mode_button);
    mSettingsButton       = (Button)findViewById(R.id.settings_button);
    
    mLocationManagementButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Loader activity
          Log.d(TAG, "Loading location management mode");
          Intent I = new Intent(context, LoaderActivity.class);
          startActivity(I);
        }
      });
    
    mNavigationModeButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Navigation activity
          Log.d(TAG, "Loading navigation mode");
          Intent I = new Intent(context, NavigationActivity.class);
          startActivity(I);
        }
      });
    
    mMeasuringModeButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Measuring activity
          Log.d(TAG, "Loading measuring mode");
          Intent I = new Intent(context, MeasuringActivity.class);
          startActivity(I);
        }
      });
    
    mTextModeButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Text activity
          Log.d(TAG, "Loading text mode");
          Intent I = new Intent(context, TextActivity.class);
          startActivity(I);
        }
      });
    
    mSettingsButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Settings activity
          Log.d(TAG, "Loading settings mode");
          Intent I = new Intent(context, SettingsActivity.class);
          startActivity(I);
        }
      });
    
    NavigineApp.initialize(getApplicationContext());
  }
  
  @Override public void onStart()
  {
    Log.d(TAG, "MainActivity started");
    super.onStart();
    
    if (NavigineApp.Navigation == null)
    {
      Toast.makeText(getApplicationContext(), "Unable to create Navigation thread!", Toast.LENGTH_LONG).show();
      mLocationManagementButton.setVisibility(View.GONE);
      mNavigationModeButton.setVisibility(View.GONE);
      mMeasuringModeButton.setVisibility(View.GONE);
      mTextModeButton.setVisibility(View.GONE);
      return;
    }
    
    String mapFile = NavigineApp.Settings.getString("map_file", "");
    mNavigationModeButton.setVisibility(mapFile.length() > 0 ? View.VISIBLE : View.GONE);
    mMeasuringModeButton.setVisibility (mapFile.length() > 0 ? View.VISIBLE : View.GONE);
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "MainActivity destroyed");
    super.onStart();
    
    NavigineApp.destroyNavigation();
  }
  
}
