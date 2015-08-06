package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.location.*;
import android.net.wifi.*;
import android.os.*;
import android.text.method.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class TextActivity extends Activity
{
  // Constants
  private static final String   TAG = "Navigine.TextActivity";
  private static final int      REQUEST_PICK_MAP = 1;  
  
  // This object
  private Context mContext = this;
  
  // UI parameters
  private Button      mButton     = null;
  private TextView    mTextView   = null;
  private TimerTask   mTimerTask  = null;
  private Handler     mHandler    = new Handler();
  private Timer       mTimer      = new Timer();
  
  private boolean     mStarted    = false;
  
  private String      mLogInputFile       = "";
  private String      mLogOutputFile      = "";
  
  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "TextActivity created");
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.text_mode);
    
    // Initializing variables
    mButton = (Button)findViewById(R.id.start_button);
    mTextView = (TextView)findViewById(R.id.text_view);
    mTextView.setMovementMethod(new ScrollingMovementMethod());
    
    NavigineApp.Navigation.setMode(NavigationThread.MODE_SCAN);
    
    // Setting up CONNECT button click handler
    mButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          toggleMode();
        }
      });
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "TextActivity destroyed");
    super.onDestroy();
    
    NavigineApp.Navigation.setMode(NavigationThread.MODE_IDLE);
  }
  
  @Override public void onResume()
  {
    Log.d(TAG, "TextActivity resumed");
    super.onResume();
    
    // Starting interface updates
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          update();
        }
      };
    mTimer.schedule(mTimerTask, 500, 100);
  }
  
  @Override public void onPause()
  {
    Log.d(TAG, "TextActivity paused");
    super.onPause();
    mTimerTask.cancel();
    mTimerTask = null;
  }
  
  @Override public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.text_menu, menu);
    return true;
  }
  
  @Override public boolean onPrepareOptionsMenu(Menu menu)
  {
    return true;
  }
  
  @Override public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
      case R.id.text_menu_load_map:
        Intent intent = new Intent(mContext, FilePickerActivity.class);
        startActivityForResult(intent, REQUEST_PICK_MAP);
        return true;
      
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (resultCode != RESULT_OK)
      return;
    
    switch (requestCode)
    {
      case REQUEST_PICK_MAP:
        if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH))
        {
          // Get the file path
          File f = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
          
          // Loading data
          if (!loadMap(f.getPath()))
          {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
            alertBuilder.setTitle(getString(R.string.error));
            alertBuilder.setMessage(getString(R.string.unable_to_load_map));
            alertBuilder.setNegativeButton(getString(R.string.ok_button),
              new DialogInterface.OnClickListener()
              {
                @Override public void onClick(DialogInterface dlg, int id)
                {
                  dlg.cancel();
                }
              });
            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            return;
          }
          
          SharedPreferences.Editor editor = NavigineApp.Settings.edit();
          editor.putString("map_file", f.getPath());
          editor.commit();
        }
        
      default:
        break;
    }
  }

  private boolean loadMap(String filename)
  {
    if (!NavigineApp.Navigation.loadArchive(filename))
    {
      String error = NavigineApp.Navigation.getLastError();
      if (error != null)
        Toast.makeText(mContext, (CharSequence)error, Toast.LENGTH_LONG).show();
      SharedPreferences.Editor editor = NavigineApp.Settings.edit();
      editor.remove("map_file");
      editor.commit();
      return false;
    }
    return true;
  }
  
  private void toggleMode()
  {
    if (mStarted)
    {
      mStarted = false;
      NavigineApp.stopNavigation(NavigationThread.MODE_SCAN);
      mButton.setText("Start");
    }
    else
    {
      mStarted = true;
      NavigineApp.startNavigation(false);
      mButton.setText("Stop");
    }
  }
  
  private void update()
  {
    mHandler.post(mRunnable);
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        try
        {
          long timeNow = DateTimeUtils.currentTimeMillis();
          StringBuilder messageBuilder = new StringBuilder();
          
          String archivePath = NavigineApp.Navigation.getArchivePath();
          if (archivePath != null && archivePath.length() > 0)
          {
            String name = new File(archivePath).getName();
            messageBuilder.append(String.format(Locale.ENGLISH, "Archive: %s\n", name));
          }
          else
            messageBuilder.append("Archive:\n");
          
          String logFile = NavigineApp.Navigation.getLogFile();
          if (logFile != null && logFile.length() > 0)
          {
            String name = new File(logFile).getName();
            messageBuilder.append(String.format(Locale.ENGLISH, "Log: %s\n", name));
          }
          else
            messageBuilder.append("Log:\n");
          
          String trackFile = NavigineApp.Navigation.getTrackFile();
          if (trackFile != null && trackFile.length() > 0)
          {
            String name = new File(trackFile).getName();
            messageBuilder.append(String.format(Locale.ENGLISH, "Track: %s\n", name));
          }
          else
            messageBuilder.append("Track:\n");
          
          String navFile = NavigineApp.Navigation.getNavigationFile();
          if (navFile != null && navFile.length() > 0)
          {
            String name = new File(navFile).getName();
            messageBuilder.append(String.format(Locale.ENGLISH, "Navigation file: %s\n", name));
          }
          else
            messageBuilder.append("Navigation file:\n");
          
          if (mStarted)
            messageBuilder.append(String.format(Locale.ENGLISH,"Messages: %d in %d sec, msg/sec: %.1f\n",
                                  NavigineApp.Navigation.getTotalMessages(),
                                  NavigineApp.Navigation.getTotalTime(),
                                  (float)NavigineApp.Navigation.getTotalMessages() /
                                  Math.max((float)NavigineApp.Navigation.getTotalTime(), 1.0f)));
          else
            messageBuilder.append("Messages:\n");
          
          if (mStarted)
          {
            int errorCode = NavigineApp.Navigation.getErrorCode();
            if (errorCode > 0)
              messageBuilder.append(String.format(Locale.ENGLISH, "ErrorCode: %d\n", errorCode));
            else
            {
              List<DeviceInfo> devices = NavigineApp.Navigation.getDeviceList();
              for(int i = 0; i < devices.size(); ++i)
              {
                DeviceInfo info = devices.get(i);
                messageBuilder.append(String.format(Locale.ENGLISH, "Device %s: [%d,  %.1f,  %.1f,  %.1f]\n",
                                      info.id, info.subLocation, info.x, info.y, info.azimuth));
              }
              if (devices.size() == 0)
                messageBuilder.append("\n");
            }
          }
          else
            messageBuilder.append("\n");
          
          messageBuilder.append("------------------------------------------------------------------------------------------\n\n");
          
          // Writing WiFi scan results
          List<WScanResult> scanResults   = NavigineApp.Navigation.getScanResults(0);
          List<WScanResult> wifiEntries   = new ArrayList<WScanResult>();
          List<WScanResult> bleEntries    = new ArrayList<WScanResult>();
          List<WScanResult> beaconEntries = new ArrayList<WScanResult>();
          Set<String> bssids = new TreeSet<String>();
          int wifiEntriesCounter = 0;
          int bleEntriesCounter  = 0;
          int beaconEntriesCounter = 0;
          
          for(int i = scanResults.size() - 1; i >= 0; --i)
          {
            WScanResult result = scanResults.get(i);
            if (result.time > timeNow)
              continue;
            
            switch (result.type)
            {
              case WScanResult.TYPE_WIFI:
                wifiEntriesCounter++;
                if (!bssids.contains(result.BSSID))
                  wifiEntries.add(result);
                break;
              
              case WScanResult.TYPE_BLE:
                bleEntriesCounter++;
                if (!bssids.contains(result.BSSID))
                  bleEntries.add(result);
                break;
              
              case WScanResult.TYPE_BEACON:
                beaconEntriesCounter++;
                if (!bssids.contains(result.BSSID))
                  beaconEntries.add(result);
                break;
            }
            
            bssids.add(result.BSSID);
          }
          
          Collections.sort(wifiEntries, new Comparator<WScanResult>() {
            @Override public int compare(WScanResult result1, WScanResult result2) {
              return result1.level > result2.level ? -1 : result1.level < result2.level ? 1 : 0;
          }});
          
          Collections.sort(bleEntries, new Comparator<WScanResult>() {
            @Override public int compare(WScanResult result1, WScanResult result2) {
              return result1.level > result2.level ? -1 : result1.level < result2.level ? 1 : 0;
            }});
          
          Collections.sort(beaconEntries, new Comparator<WScanResult>() {
            @Override public int compare(WScanResult result1, WScanResult result2) {
              return result1.distance < result2.distance ? -1 : result1.distance > result2.distance ? 1 : 0;
            }});
          
          messageBuilder.append(String.format(Locale.ENGLISH, "Wi-Fi networks (%d), entries/sec: %.1f\n",
                                wifiEntries.size(), (float)wifiEntriesCounter * 1000.0f / MeasureThread.STORAGE_TIMEOUT));
          
          for(int i = 0; i < wifiEntries.size(); ++i)
          {
            if (i >= 5)
            {
              messageBuilder.append("...\n");
              break;
            }
            WScanResult result = wifiEntries.get(i);
            String name = result.SSID.length() <= 11 ? result.SSID : result.SSID.substring(0, 10) + "...";
            messageBuilder.append(String.format(Locale.ENGLISH, "%s   \t  %.1f\t  (%.1f sec)\t  (%s)\n",
                                                result.BSSID, (float)result.level,
                                                (float)(timeNow - result.time) / 1000,
                                                name));
          }
          messageBuilder.append("\n");
          
          messageBuilder.append(String.format(Locale.ENGLISH, "BLE devices (%d), entries/sec: %.1f\n",
                                bleEntries.size(), (float)bleEntriesCounter * 1000.0f / MeasureThread.STORAGE_TIMEOUT));
          
          for(int i = 0; i < bleEntries.size(); ++i)
          {
            if (i >= 5)
            {
              messageBuilder.append("...\n");
              break;
            }
            WScanResult result = bleEntries.get(i);
            messageBuilder.append(String.format(Locale.ENGLISH, "%s \t  %.1f\t  (%.1f sec)\n",
                                                result.BSSID, (float)result.level,
                                                (float)(timeNow - result.time) / 1000));
          }
          messageBuilder.append("\n");
          
          messageBuilder.append(String.format(Locale.ENGLISH, "BEACONs (%d), entries/sec: %.1f\n",
                                beaconEntries.size(), (float)beaconEntriesCounter * 1000.0f / MeasureThread.STORAGE_TIMEOUT));
          
          for(int i = 0; i < beaconEntries.size(); ++i)
          {
            if (i >= 5)
            {
              messageBuilder.append("...\n");
              break;
            }
            WScanResult result = beaconEntries.get(i);
            String address = result.BSSID.substring(0, 13) + "...)";
            messageBuilder.append(String.format(Locale.ENGLISH, "%s \t%.1f \t%.1fm \t(%.1f sec)   BAT=%d%%\n",
                                                address, (float)result.level, result.distance,
                                                (float)(timeNow - result.time) / 1000,
                                                result.battery));
          }
          messageBuilder.append("\n");
          
          // Writing accelerometer values
          float[] accelVector = NavigineApp.Navigation.getAccelVector();
          if (accelVector != null)
            messageBuilder.append(String.format(Locale.ENGLISH, "Accelerometer:\t(%.4f, %.4f, %.4f)\n",
                                                accelVector[0], accelVector[1], accelVector[2]));
          
          // Writing magnetometer values
          float[] magnetVector = NavigineApp.Navigation.getMagnetVector();
          if (magnetVector != null)
            messageBuilder.append(String.format(Locale.ENGLISH, "Magnetometer:\t(%.4f, %.4f, %.4f)\n",
                                                magnetVector[0], magnetVector[1], magnetVector[2]));
          
          // Writing gyroscope values
          float[] gyroVector = NavigineApp.Navigation.getGyroVector();
          if (gyroVector != null)
            messageBuilder.append(String.format(Locale.ENGLISH, "Gyroscope:\t\t(%.4f, %.4f, %.4f)\n",
                                                gyroVector[0], gyroVector[1], gyroVector[2]));
          
          // Writing orientation values
          float[] orientVector = NavigineApp.Navigation.getOrientVector();
          if (orientVector != null)
            messageBuilder.append(String.format(Locale.ENGLISH, "Orientation:\t(%.4f, %.4f, %.4f)\n",
                                                orientVector[0], orientVector[1], orientVector[2]));
          
          // Writing location (gps) coordinates
          double[] locVector = NavigineApp.Navigation.getLocationVector();
          if (locVector != null)
            messageBuilder.append(String.format(Locale.ENGLISH, "GPS: (%.8f, %.8f, %.2f, %.2f)\n",
                                                locVector[0], locVector[1], locVector[2], locVector[3]));
          
          mTextView.setText(messageBuilder.toString());
        }
        catch (Throwable e)
        {
          Log.e(TAG, Log.getStackTraceString(e));
        }
      }
    };
}
