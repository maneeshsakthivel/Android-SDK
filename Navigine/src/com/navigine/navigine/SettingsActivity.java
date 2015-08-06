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

public class SettingsActivity extends Activity
{
  // Constants
  private static final String TAG = "Navigine.SettingsActivity";
  
  // This context
  private final Context context = this;
  
  private int mBackgroundMode = NavigationThread.MODE_NORMAL;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.settings);
    
    CheckBox serverCheckBox = (CheckBox)findViewById(R.id.settings__navigation_server_checkbox);
    serverCheckBox.setOnCheckedChangeListener(
      new CompoundButton.OnCheckedChangeListener()
      {
        @Override public void onCheckedChanged(CompoundButton button, boolean checked)
        {
          findViewById(R.id.settings__navigation_server_address_edit).setVisibility(checked ? View.VISIBLE : View.GONE);
        }
      });
    
    Button saveButton = (Button)findViewById(R.id.settings__save_settings_button);
    saveButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          saveSettings();
        }
      });
    
    setCheckBox (R.id.settings__beacon_service_checkbox,        NavigineApp.Settings.getBoolean("beacon_service_enabled", true));
    setCheckBox (R.id.settings__navigation_server_checkbox,     NavigineApp.Settings.getBoolean("navigation_server_enabled", false));
    setTextValue(R.id.settings__navigation_server_address_edit, NavigineApp.Settings.getString("navigation_server_address", NavigineApp.DEFAULT_SERVER));
    setCheckBox (R.id.settings__save_navigation_log_checkbox,   NavigineApp.Settings.getBoolean("navigation_log_enabled", false));
    setCheckBox (R.id.settings__save_navigation_track_checkbox, NavigineApp.Settings.getBoolean("navigation_track_enabled", false));
    setCheckBox (R.id.settings__post_messages_enabled_checkbox, NavigineApp.Settings.getBoolean("post_messages_enabled", true));
    
    mBackgroundMode = NavigineApp.Settings.getInt("background_navigation_mode", NavigationThread.MODE_NORMAL);
    switch (mBackgroundMode)
    {
      case NavigationThread.MODE_NORMAL:
        ((RadioButton)findViewById(R.id.radio_normal_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_ECONOMIC:
        ((RadioButton)findViewById(R.id.radio_economic_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_ECONOMIC2:
        ((RadioButton)findViewById(R.id.radio_economic2_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_IDLE:
        ((RadioButton)findViewById(R.id.radio_idle_mode)).setChecked(true);
        break;
    }
    
    boolean serverEnabled = getCheckBox(R.id.settings__navigation_server_checkbox);
    EditText serverAddressEdit = (EditText)findViewById(R.id.settings__navigation_server_address_edit);
    serverAddressEdit.setVisibility(serverEnabled ? View.VISIBLE : View.GONE);
  }
  
  public void onRadioButtonClicked(View view)
  {
    boolean checked = ((RadioButton)view).isChecked();
    if (!checked)
      return;
    
    // Check which radio button was clicked
    switch (view.getId())
    {
      case R.id.radio_normal_mode:
        mBackgroundMode = NavigationThread.MODE_NORMAL;
        break;
      
      case R.id.radio_economic_mode:
        mBackgroundMode = NavigationThread.MODE_ECONOMIC;
        break;
      
      case R.id.radio_economic2_mode:
        mBackgroundMode = NavigationThread.MODE_ECONOMIC2;
        break;
      
      case R.id.radio_idle_mode:
        mBackgroundMode = NavigationThread.MODE_IDLE;
        break;
    }
  }
  
  private void setTextValue(int id, String text)
  {
    EditText edit = (EditText)findViewById(id);
    if (edit != null)
      edit.setText(text);
  }
  
  private String getTextValue(int id)
  {
    EditText edit = (EditText)findViewById(id);
    return edit.getText().toString();
  }
  
  private int getIntValue(int id, int defaultValue, int minValue, int maxValue)
  {
    EditText edit = (EditText)findViewById(id);
    String text = edit.getText().toString();
    int value = defaultValue;
    try { value = Integer.parseInt(text); } catch (Throwable e) { }
    return Math.max(Math.min(value, maxValue), minValue);
  }
  
  private void setCheckBox(int id, boolean enabled)
  {
    CheckBox checkBox = (CheckBox)findViewById(id);
    if (checkBox != null)
      checkBox.setChecked(enabled);
  }
  
  private boolean getCheckBox(int id)
  {
    CheckBox checkBox = (CheckBox)findViewById(id);
    return checkBox.isChecked();
  }
  
  private void saveSettings()
  {
    SharedPreferences.Editor editor = NavigineApp.Settings.edit();
    editor.putInt("background_navigation_mode",    mBackgroundMode);
    editor.putBoolean("beacon_service_enabled",    getCheckBox (R.id.settings__beacon_service_checkbox));
    editor.putBoolean("navigation_server_enabled", getCheckBox (R.id.settings__navigation_server_checkbox));
    editor.putString ("navigation_server_address", getTextValue(R.id.settings__navigation_server_address_edit));
    editor.putBoolean("navigation_log_enabled",    getCheckBox (R.id.settings__save_navigation_log_checkbox));
    editor.putBoolean("navigation_track_enabled",  getCheckBox (R.id.settings__save_navigation_track_checkbox));
    editor.putBoolean("post_messages_enabled",     getCheckBox (R.id.settings__post_messages_enabled_checkbox));
    editor.commit();
  }
}
