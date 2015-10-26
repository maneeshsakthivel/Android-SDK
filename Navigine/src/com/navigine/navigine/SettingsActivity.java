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
  private static final int REQUEST_PICK_FILE = 1;
  
  // This context
  private final Context context = this;
  
  private boolean mBackgroundNavigationEnabled = true;
  private int mBackgroundMode = NavigationThread.MODE_NORMAL;
  
  private boolean mNavigationFileEnabled = false;
  private String mNavigationFile = "";
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.settings);
    
    CheckBox backgroundNavigationCheckBox = (CheckBox)findViewById(R.id.settings__background_navigation_checkbox);
    backgroundNavigationCheckBox.setOnCheckedChangeListener(
      new CompoundButton.OnCheckedChangeListener()
      {
        @Override public void onCheckedChanged(CompoundButton button, boolean checked)
        {
          mBackgroundNavigationEnabled = checked;
          findViewById(R.id.settings__radio_group).setVisibility(checked ? View.VISIBLE : View.GONE);
        }
      });
    
    CheckBox navigationFileCheckBox = (CheckBox)findViewById(R.id.settings__navigation_file_enabled_checkbox);
    navigationFileCheckBox.setOnClickListener(
      new CompoundButton.OnClickListener()
      {
        @Override public void onClick(View v)
        {
          mNavigationFileEnabled = ((CheckBox)findViewById(R.id.settings__navigation_file_enabled_checkbox)).isChecked();
          if (mNavigationFileEnabled)
          {
            Intent intent = new Intent(NavigineApp.AppContext, FilePickerActivity.class);
            startActivityForResult(intent, REQUEST_PICK_FILE);
          }
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
    
    if (NavigineApp.Settings.getBoolean("navigation_file_enabled", false) &&
        NavigineApp.Settings.getString("navigation_file", "").length() > 0)
    {
      mNavigationFileEnabled = true;
      mNavigationFile = NavigineApp.Settings.getString("navigation_file", "");
      setCheckBox (R.id.settings__navigation_file_enabled_checkbox, true);
      TextView tv = (TextView)findViewById(R.id.settings__navigation_file_enabled_label);
      String name = new File(mNavigationFile).getName();
      String text = String.format(Locale.ENGLISH, "Navigation file enabled:\n'%s'", name);
      tv.setText(text);
    }
    else
    {
      mNavigationFileEnabled = false;
      setCheckBox (R.id.settings__navigation_file_enabled_checkbox, false);
      TextView tv = (TextView)findViewById(R.id.settings__navigation_file_enabled_label);
      tv.setText("Navigation file enabled");
    }
    
    setTextValue(R.id.settings__location_server_address_edit,   NavigineApp.Settings.getString ("location_server_address", NavigineApp.DEFAULT_SERVER));
    setCheckBox (R.id.settings__location_server_ssl_checkbox,   NavigineApp.Settings.getBoolean("location_server_ssl_enabled", true));
    setCheckBox (R.id.settings__beacon_service_checkbox,        NavigineApp.Settings.getBoolean("beacon_service_enabled", true));
    setCheckBox (R.id.settings__save_navigation_log_checkbox,   NavigineApp.Settings.getBoolean("navigation_log_enabled", false));
    setCheckBox (R.id.settings__save_navigation_track_checkbox, NavigineApp.Settings.getBoolean("navigation_track_enabled", false));
    setCheckBox (R.id.settings__post_messages_enabled_checkbox, NavigineApp.Settings.getBoolean("post_messages_enabled", true));
    
    mBackgroundMode = NavigineApp.Settings.getInt("background_navigation_mode", NavigationThread.MODE_NORMAL);
    switch (mBackgroundMode)
    {
      case NavigationThread.MODE_NORMAL:
        setCheckBox(R.id.settings__background_navigation_checkbox, true);
        ((RadioButton)findViewById(R.id.settings__radio_normal_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_ECONOMIC1:
        setCheckBox(R.id.settings__background_navigation_checkbox, true);
        ((RadioButton)findViewById(R.id.settings__radio_economic_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_ECONOMIC2:
        setCheckBox(R.id.settings__background_navigation_checkbox, true);
        ((RadioButton)findViewById(R.id.settings__radio_economic2_mode)).setChecked(true);
        break;
      
      case NavigationThread.MODE_IDLE:
        setCheckBox(R.id.settings__background_navigation_checkbox, false);
    }
  }
  
  public void onRadioButtonClicked(View view)
  {
    boolean checked = ((RadioButton)view).isChecked();
    if (!checked)
      return;
    
    // Check which radio button was clicked
    switch (view.getId())
    {
      case R.id.settings__radio_normal_mode:
        mBackgroundMode = NavigationThread.MODE_NORMAL;
        break;
      
      case R.id.settings__radio_economic_mode:
        mBackgroundMode = NavigationThread.MODE_ECONOMIC1;
        break;
      
      case R.id.settings__radio_economic2_mode:
        mBackgroundMode = NavigationThread.MODE_ECONOMIC2;
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
    editor.putString ("location_server_address",      getTextValue(R.id.settings__location_server_address_edit));
    editor.putBoolean("location_server_ssl_enabled",  getCheckBox (R.id.settings__location_server_ssl_checkbox));
    editor.putInt("background_navigation_mode",       mBackgroundNavigationEnabled ? mBackgroundMode : NavigationThread.MODE_IDLE);
    editor.putBoolean("beacon_service_enabled",       getCheckBox (R.id.settings__beacon_service_checkbox));
    editor.putBoolean("navigation_log_enabled",       getCheckBox (R.id.settings__save_navigation_log_checkbox));
    editor.putBoolean("navigation_track_enabled",     getCheckBox (R.id.settings__save_navigation_track_checkbox));
    editor.putBoolean("post_messages_enabled",        getCheckBox (R.id.settings__post_messages_enabled_checkbox));
    editor.putBoolean("navigation_file_enabled",      mNavigationFileEnabled);
    editor.putString ("navigation_file",              mNavigationFileEnabled ? mNavigationFile : "");
    editor.commit();
    
    NavigineApp.applySettings();
  }
  
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (requestCode != REQUEST_PICK_FILE)
      return;
    
    if (resultCode == RESULT_OK)
    {
      if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH))
      {
        // Get the file path
        File f = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
        
        mNavigationFileEnabled = true;
        mNavigationFile = f.getAbsolutePath();
        
        ((CheckBox)findViewById(R.id.settings__navigation_file_enabled_checkbox)).setChecked(true);
        TextView tv = (TextView)findViewById(R.id.settings__navigation_file_enabled_label);
        String text = String.format(Locale.ENGLISH, "Navigation file enabled:\n'%s'", f.getName());
        tv.setText(text);
      }
    }
    else
    {
      mNavigationFileEnabled = false;
      mNavigationFile = "";
      ((CheckBox)findViewById(R.id.settings__navigation_file_enabled_checkbox)).setChecked(false);
      TextView tv = (TextView)findViewById(R.id.settings__navigation_file_enabled_label);
      tv.setText("Navigation file enabled");
    }
  }
}
