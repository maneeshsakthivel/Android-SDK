package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnDismissListener;
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
  private static final String TAG = "NAVIGINE.MainActivity";
  
  // This context
  private final Context mContext = this;
  
  // GUI parameters
  private Button mLocationManagementButton = null;
  private Button mNavigationModeButton = null;
  private Button mMeasuringModeButton  = null;
  private Button mTextModeButton       = null;
  private Button mSettingsButton       = null;
  private Button mLoginButton          = null;
  
  private boolean mBeaconServiceStarted = false;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "MainActivity created");
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);
    
    // Setting up GUI parameters
    mLoginButton = (Button)findViewById(R.id.main_mode__login_button);
    mLocationManagementButton = (Button)findViewById(R.id.main_mode__location_management_button);
    mNavigationModeButton = (Button)findViewById(R.id.main_mode__navigation_mode_button);
    mMeasuringModeButton  = (Button)findViewById(R.id.main_mode__measuring_mode_button);
    mTextModeButton       = (Button)findViewById(R.id.main_mode__text_mode_button);
    mSettingsButton       = (Button)findViewById(R.id.main_mode__settings_button);
    
    mLoginButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          showUserHashDialog();
        }
      });
    
    mLocationManagementButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          // Starting Loader activity
          Log.d(TAG, "Loading location management mode");
          Intent I = new Intent(mContext, LoaderActivity.class);
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
          Intent I = new Intent(mContext, NavigationActivity.class);
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
          Intent I = new Intent(mContext, MeasuringActivity.class);
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
          Intent I = new Intent(mContext, TextActivity.class);
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
          Intent I = new Intent(mContext, SettingsActivity.class);
          startActivity(I);
        }
      });
    
    NavigineApp.initialize(getApplicationContext());
  }
  
  @Override public void onStart()
  {
    Log.d(TAG, "MainActivity started");
    super.onStart();
    
    refresh();
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "MainActivity destroyed");
    super.onStart();
    
    NavigineApp.destroyNavigation();
  }
  
  private EditText _userEdit = null;  
  private AlertDialog _alertDialog = null;
  private void showUserHashDialog()
  {
    mLoginButton.setVisibility(View.GONE);
    mLocationManagementButton.setVisibility(View.GONE);
    mNavigationModeButton.setVisibility(View.GONE);
    mMeasuringModeButton.setVisibility (View.GONE);
    mTextModeButton.setVisibility(View.GONE);
    mSettingsButton.setVisibility(View.GONE);
    
    String userHash = NavigineApp.Settings.getString("user_hash", "");
    
    LayoutInflater inflater = getLayoutInflater();
    View view = inflater.inflate(R.layout.user_hash_dialog, null);
    _userEdit = (EditText)view.findViewById(R.id.user_hash_edit);
    _userEdit.setText(userHash);
    _userEdit.setTypeface(Typeface.MONOSPACE); 
    
    Button loginButton = (Button)view.findViewById(R.id.user_hash_dialog__login_button);
    Button cancelButton = (Button)view.findViewById(R.id.user_hash_dialog__cancel_button);
    
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
    alertBuilder.setView(view);
    alertBuilder.setTitle("Enter user ID");
    
    loginButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (_alertDialog != null)
          {
            String userHash = _userEdit.getText().toString();
            SharedPreferences.Editor editor = NavigineApp.Settings.edit();
            editor.putString("user_hash", userHash);
            editor.commit();
            NavigineApp.applySettings();          
            
            refresh();
            _alertDialog.cancel();
          }
        }
      });
    
    cancelButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (_alertDialog != null)
          {
            refresh();
            _alertDialog.cancel();
          }
        }
      });
    
    _alertDialog = alertBuilder.create();
    _alertDialog.setCanceledOnTouchOutside(false);
    _alertDialog.setOnDismissListener(
      new OnDismissListener()
      {
        @Override public void onDismiss(DialogInterface dialog) 
        {
          refresh();
        }
      });
    
    _alertDialog.show();
  }
  
  private void refresh()
  {
    if (NavigineApp.Navigation == null)
    {
      Toast.makeText(getApplicationContext(), "Unable to create Navigation thread!", Toast.LENGTH_LONG).show();
      mLocationManagementButton.setVisibility(View.GONE);
      mNavigationModeButton.setVisibility(View.GONE);
      mMeasuringModeButton.setVisibility(View.GONE);
      mTextModeButton.setVisibility(View.GONE);
      return;
    }
    
    String mapFile  = NavigineApp.Settings.getString("map_file", "");
    String userHash = NavigineApp.Settings.getString("user_hash", "");
    
    boolean hasMap  = mapFile.length() > 0;
    boolean hasHash = userHash.length() > 0;
    boolean debugMode = NavigineApp.Settings.getBoolean("debug_mode_enabled", false);
    
    mLoginButton.setVisibility(View.VISIBLE);
    mLocationManagementButton.setVisibility(hasHash ? View.VISIBLE : View.GONE);
    mNavigationModeButton.setVisibility(hasHash && hasMap ? View.VISIBLE : View.GONE);
    mMeasuringModeButton.setVisibility (hasHash && hasMap ? View.VISIBLE : View.GONE);
    mTextModeButton.setVisibility(hasHash && debugMode ? View.VISIBLE : View.GONE);
    mSettingsButton.setVisibility(View.VISIBLE);
  }
  
}
