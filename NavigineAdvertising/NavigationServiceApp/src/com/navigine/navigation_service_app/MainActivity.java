package com.navigine.navigation_service_app;
import com.navigine.navigation_service.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class MainActivity extends Activity
{
  public static final int DEBUG_LEVEL = 2;
  
  private Button    mRestartButton  = null;
  private TimerTask mTimerTask      = null;
  private Handler   mHandler        = new Handler();
  private Timer     mTimer          = new Timer();
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    Y.setDebugLevel(this, DEBUG_LEVEL);
    Y.initialize(getApplication(), null, null);
    
    setTextValue(R.id.settings__user_hash_edit,     Y.getUserHash(this));
    setTextValue(R.id.settings__server_url_edit,    Y.getServerUrl(this));
    
    mRestartButton = (Button)findViewById(R.id.settings__restart_service_button);
    mRestartButton.setEnabled(false);
    
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
     mTimer.schedule(mTimerTask, 5000, 100);
  }
  
  @Override public void onDestroy()
  {
    super.onDestroy();
  }
  
  public void setTextValue(int id, String text)
  {
    EditText edit = (EditText)findViewById(id);
    if (edit != null)
      edit.setText(text);
  }
  
  public String getTextValue(int id)
  {
    EditText edit = (EditText)findViewById(id);
    return edit.getText().toString();
  }
  
  public void saveSettings(View v)
  {
    Y.setUserHash (this, getTextValue(R.id.settings__user_hash_edit));
    Y.setServerUrl(this, getTextValue(R.id.settings__server_url_edit));
  }
  
  public void toggleService(View v)
  {
    Y.toggleService(this);
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (Y.checkService())
        {
          mRestartButton.setText("Stop service");
          mRestartButton.setEnabled(true);
        }
        else
        {
          mRestartButton.setText("Start service");
          mRestartButton.setEnabled(true);
        }
      }
    };
}
