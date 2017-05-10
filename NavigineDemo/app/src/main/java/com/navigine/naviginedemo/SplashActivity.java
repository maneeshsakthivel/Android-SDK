package com.navigine.naviginedemo;

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

public class SplashActivity extends Activity
{
  private TextView mErrorLabel = null;
  
  class InitTask extends AsyncTask<Void, Void, Boolean>
  {
    @Override protected Boolean doInBackground(Void... params)
    {
      if (!DemoApp.initialize(getApplicationContext()))
      {
        mErrorLabel.setText("Error loading NavigineSDK! It seems that your device is not supported yet! Please, contact technical support");
        mErrorLabel.setVisibility(View.VISIBLE);
        return Boolean.FALSE;
      }
      if (!NavigineSDK.loadLocation(DemoApp.LOCATION_ID, 30))
      {
        mErrorLabel.setText("Error downloading location 'Navigine Demo'! Please, try again later or contact technical support");
        mErrorLabel.setVisibility(View.VISIBLE);
        return Boolean.FALSE;
      }
      return Boolean.TRUE;
    }
    
    @Override protected void onPostExecute(Boolean result)
    {
      if (result.booleanValue())
      {
        // Starting main activity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
      }
    }
  }
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_splash);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                         WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

    mErrorLabel = (TextView)findViewById(R.id.splash__error_label);
    mErrorLabel.setVisibility(View.GONE);
    
    (new InitTask()).execute();
  }

  @Override public void onBackPressed()
  {
    moveTaskToBack(true);
  }
}
