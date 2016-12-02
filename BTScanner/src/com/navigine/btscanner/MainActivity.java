package com.navigine.btscanner;
import com.navigine.btscanner.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class MainActivity extends Activity
{
  // Constants
  private static final String TAG = "BTScanner";
  
  // This object
  private final Context context = this;
  
  // Measure thread
  MeasureThread     mMeasureThread = null;
  
  // UI parameters
  private TextView  mTextView  = null;
  private TimerTask mTimerTask = null;
  private Handler   mHandler   = new Handler();
  private Timer     mTimer     = new Timer();
  private List<String> mMessageList = new ArrayList<String>();
  
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
      
    mTextView = (TextView)findViewById(R.id.text_view);
    
    mMeasureThread = new MeasureThread(this);
    mMeasureThread.setBluetoothEnabled(true);
    mMeasureThread.setBluetoothScanEnabled(true, 1000, 100);
    
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, 500, 100);
  }
  
  @Override public void onDestroy()
  {
    mMeasureThread.terminate();
    try
    {
      Log.d(TAG, "Joining MeasureThread");
      mMeasureThread.join();
    }
    catch (Throwable e)
    {
      Log.e(TAG, Log.getStackTraceString(e));
    }
    super.onDestroy();
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        final long timeNow = System.currentTimeMillis();
        final List<MeasureThread.ScanResult> scanResults = mMeasureThread.getScanResults(0);
        List<MeasureThread.ScanResult> beaconEntries = new ArrayList<MeasureThread.ScanResult>();
        int beaconEntriesCounter = 0;
        
        Set<String> devices = new TreeSet<String>();
        for(int i = scanResults.size() - 1; i >= 0; --i)
        {
          MeasureThread.ScanResult result = scanResults.get(i);
          
          beaconEntriesCounter++;
          if (!devices.contains(result.address))
            beaconEntries.add(result);
          devices.add(result.address);
        }
        
        Collections.sort(beaconEntries, new Comparator<MeasureThread.ScanResult>() {
          @Override public int compare(MeasureThread.ScanResult result1, MeasureThread.ScanResult result2) {
            return result1.distance < result2.distance ? -1 : result1.distance > result2.distance ? 1 : 0;
          }});
        
        StringBuilder messageBuilder = new StringBuilder();
          
        messageBuilder.append(String.format(Locale.ENGLISH, "BEACONs (%d), entries/sec: %.1f\n",
                              beaconEntries.size(), (float)beaconEntriesCounter * 1000.0f / MeasureThread.STORAGE_TIMEOUT));
        
        for(int i = 0; i < beaconEntries.size(); ++i)
        {
          if (i >= 15)
          {
            messageBuilder.append("...\n");
            break;
          }
          MeasureThread.ScanResult result = beaconEntries.get(i);
          String uuid = result.uuid.substring(0, 6) + "…";
          messageBuilder.append(String.format(Locale.ENGLISH, "%s: \t%05d: \t%05d: \t%s  %d / %.1fm \t(%.1f sec) \t BAT=%d%%\n",
                                              result.name, result.major, result.minor, uuid, result.rssi, result.distance,
                                              (float)(timeNow - result.timeLabel) / 1000,
                                              result.battery));
        }
        messageBuilder.append("\n");
        
        mTextView.setText(messageBuilder.toString());
      }
    };
}
