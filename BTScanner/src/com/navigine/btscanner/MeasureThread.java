package com.navigine.btscanner;

import android.app.*;
import android.bluetooth.*;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.*;
import android.os.*;
import android.util.*;
import java.lang.*;
import java.util.*;

public class MeasureThread extends Thread
{
  public static final String TAG                  = "BTScanner";
  public static final int STORAGE_TIMEOUT         = 10000;  // in milliseconds
  public static final int BLUETOOTH_SCAN_TIMEOUT  = 1000;   // in milliseconds
  public static final int BLUETOOTH_SLEEP_TIMEOUT = 100;    // in milliseconds
  public static final int SLEEP_TIMEOUT           = 100;    // in milliseconds
  
  // Debug levels:
  //  1 - error messages
  //  2 - api functions
  //  3 - wifi, bluetooth, beacon updates
  public static int DEBUG_LEVEL = 2;
  
  private Context mContext  = null;
  private boolean mStopFlag = false;
  
  // Bluetooth parameters
  private boolean mBluetoothScan = false;
  private long mBluetoothTime = 0;
  private int mBluetoothScanTimeout = BLUETOOTH_SCAN_TIMEOUT;
  private int mBluetoothSleepTimeout = BLUETOOTH_SLEEP_TIMEOUT;
  private BluetoothAdapter mBluetoothAdapter = null;
  private BluetoothAdapter.LeScanCallback mLeScanCallBack = null;
  private boolean mBluetoothScanEnabled = false;
  
  public class ScanResult
  {
    public String   address   = "";
    public String   uuid      = "";
    public int      major     = 0;
    public int      minor     = 0;
    public int      rssi      = 0;
    public int      power     = 0;
    public int      battery   = 0;
    public float    distance  = 0.0f;
    public long     timeLabel = 0;
    
    ScanResult()
    { }
    
    ScanResult(ScanResult result)
    {
      address   = new String(result.address);
      uuid      = new String(result.uuid);
      major     = result.major;
      minor     = result.minor;
      rssi      = result.rssi;
      power     = result.power;
      battery   = result.battery;
      distance  = result.distance;
      timeLabel = result.timeLabel;
    }
  };
  
  private List<ScanResult> mScanResults = new ArrayList<ScanResult>();
  
  private static String byteArrayToHex(byte[] a)
  {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for(byte b: a)
      sb.append(String.format("%02X", b & 0xff));
    return sb.toString();
  }
  
  private static int parseIBeaconData(byte[] data)
  {
    if (data != null)
    {
      for(int i = 0; i + 25 < data.length; ++i)
        if (((data[i] & 0xff) == 0x4c || (data[i] & 0xff) == 0x59) &&
            (data[i+1] & 0xff) == 0x00 &&
            (data[i+2] & 0xff) == 0x02 &&
            (data[i+3] & 0xff) == 0x15)
          return i;
    }
    return -1;
  }
  
  private static String getUuid(byte[] data, int index)
  {
    String uuid = "";
    uuid += byteArrayToHex(Arrays.copyOfRange(data, index, index + 4));
    uuid += "-";
    uuid += byteArrayToHex(Arrays.copyOfRange(data, index + 4, index + 6));
    uuid += "-";
    uuid += byteArrayToHex(Arrays.copyOfRange(data, index + 6, index + 8));
    uuid += "-";
    uuid += byteArrayToHex(Arrays.copyOfRange(data, index + 8, index + 10));
    uuid += "-";
    uuid += byteArrayToHex(Arrays.copyOfRange(data, index + 10, index + 16));
    return uuid;
  }
  
  private static int getBattery(byte[] data, int index)
  {
    for(int i = data.length - 1; i >= index; --i)
      if (data[i] != 0)
        return Math.min(Math.max(data[i], 0), 100);
    return 0;
  }
  
  private static float getDistance(int rssi, int power)
  {
    return (float)Math.pow(10.0, (double)(power - rssi) / 20.0);
  }
  
  public MeasureThread(Context context)
  {
    mContext = context;
    super.start();

    synchronized (this)
    {
      // Setting up bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (mBluetoothAdapter != null)
      {
        try
        {
          mLeScanCallBack = new BluetoothAdapter.LeScanCallback()
          {
            @Override public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
            {
              try
              {
                if (scanRecord != null && scanRecord.length > 0 && rssi < 0 && rssi >= -120)
                {
                  int index = parseIBeaconData(scanRecord);
                  if (index >= 0)
                  {
                    String uuid = getUuid(scanRecord, index + 4);
                    int major   = (int)(scanRecord[index + 20] & 0xff) * 256 + (int)(scanRecord[index + 21] & 0xff);
                    int minor   = (int)(scanRecord[index + 22] & 0xff) * 256 + (int)(scanRecord[index + 23] & 0xff);
                    int power   = (int)(scanRecord[index + 24] & 0xff) - 256;
                    int battery = getBattery(scanRecord, index + 25);
                    float dist  = getDistance(rssi, power);
                    addBeacon(device.getAddress(), uuid, major, minor, rssi, power, battery, dist);
                  }
                }
              }
              catch (Throwable e)
              {
                if (DEBUG_LEVEL >= 1)
                  Log.e(TAG, Log.getStackTraceString(e));
              }
            }
          };
        }
        catch (Throwable e)
        {
          mLeScanCallBack = null;
          if (DEBUG_LEVEL >= 1)
            Log.e(TAG, Log.getStackTraceString(e));
        }
      }
    }

  }
  
  // Send request for thread to terminate
  public synchronized void terminate()
  {
    mStopFlag = true;
  }
  
  public synchronized boolean isBluetoothEnabled()
  {
    return mBluetoothAdapter != null &&
           mBluetoothAdapter.isEnabled();
  }
  
  public synchronized void setBluetoothEnabled(boolean enabled)
  {
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, String.format(Locale.ENGLISH, "Set bluetooth enabled: %s",
            (enabled ? "true" : "false")));
    try
    {
      if (mBluetoothAdapter == null)
        return;
      
      if (enabled)
      {
        if (mBluetoothAdapter.isEnabled())
          return;
        
        if (DEBUG_LEVEL >= 2)
          Log.d(TAG, "Enabling BLUETOOTH");
        mBluetoothAdapter.enable();
      }
      else
      {
        if (!mBluetoothAdapter.isEnabled())
          return;
        
        if (DEBUG_LEVEL >= 2)
         Log.d(TAG, "Disabling BLUETOOTH");
        mBluetoothAdapter.disable();
      }
    }
    catch (Throwable e)
    {
      if (DEBUG_LEVEL >= 1)
        Log.e(TAG, Log.getStackTraceString(e));
    }
  }
  
  public synchronized void setBluetoothScanEnabled(boolean enabled, int scanTimeout, int sleepTimeout)
  {
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, String.format(Locale.ENGLISH, "Set bluetooth scan enabled: %s, scan=%d ms, sleep=%d ms",
            (enabled ? "true" : "false"), scanTimeout, sleepTimeout));
    
    try
    {
      mBluetoothScanEnabled  = enabled;
      mBluetoothScanTimeout  = Math.max(scanTimeout,  BLUETOOTH_SCAN_TIMEOUT);
      mBluetoothSleepTimeout = Math.max(sleepTimeout, BLUETOOTH_SLEEP_TIMEOUT);
      
      // Setting up bluetooth scan
      if (mBluetoothAdapter != null &&
          mBluetoothAdapter.isEnabled() &&
          mLeScanCallBack != null)
      {
        if (DEBUG_LEVEL >= 3)
          Log.d(TAG, "Stop bluetooth scanning!");
        mBluetoothAdapter.stopLeScan(mLeScanCallBack);
        mBluetoothTime = System.currentTimeMillis();
        mBluetoothScan = false;
        
        if (mBluetoothScanEnabled)
        {
          if (DEBUG_LEVEL >= 3)
            Log.d(TAG, "Start bluetooth scanning!");
          mBluetoothAdapter.startLeScan(mLeScanCallBack);
          mBluetoothTime = System.currentTimeMillis();
          mBluetoothScan = true;
        }
      }
    }
    catch (Throwable e)
    {
      if (DEBUG_LEVEL >= 1)
        Log.e(TAG, Log.getStackTraceString(e));
    }
  }
  
  // Get the last scan results
  public synchronized List<ScanResult> getScanResults(long timeLabel)
  {
    List<ScanResult> L = new ArrayList<ScanResult>();
    for(int i = 0; i < mScanResults.size(); ++i)
    {
      ScanResult result = mScanResults.get(i);
      if (result.timeLabel >= timeLabel)
        L.add(new ScanResult(result));
    }
    return L;
  }
  
  private synchronized void addBeacon(String address, String uuid, int major, int minor, int rssi, int power, int battery, float distance)
  {
    // Filtering out results with invalid RSSI
    if (rssi >= 0 || rssi < -120)
      return;
    
    if (DEBUG_LEVEL >= 3)
      Log.d(TAG, String.format(Locale.ENGLISH, "BEACON: address=%s, uuid=%s, major=%05d, minor=%05d, rssi=%d, distance=%.2f",
            address, uuid, major, minor, rssi, distance));
    
    ScanResult result = new ScanResult();
    result.address    = address;
    result.uuid       = uuid;
    result.major      = major;
    result.minor      = minor;
    result.rssi       = rssi;
    result.power      = power;
    result.battery    = battery;
    result.distance   = distance;
    result.timeLabel  = System.currentTimeMillis();    
    mScanResults.add(result);
  }
  
  @Override public void run()
  {
    Looper.prepare();
    
    while (!mStopFlag)
    {
      // Determine, if we must terminate
      long timeNow = System.currentTimeMillis();
      
      synchronized (this)
      {
        try
        {
          // Removing obsolete scan results
          int pos = 0;
          for(pos = 0; pos < mScanResults.size(); ++pos)
          {
            ScanResult result = mScanResults.get(pos);
            if (timeNow < result.timeLabel + STORAGE_TIMEOUT)
              break;
          }
          if (pos > 0)
            mScanResults.subList(0, pos).clear();
          
          // Restarting bluetooth scan (if necessary)
          if (isBluetoothEnabled() &&
              mBluetoothScanEnabled &&
              mLeScanCallBack != null)
          {
            if (mBluetoothScan && Math.abs(timeNow - mBluetoothTime) >= mBluetoothScanTimeout)
            {
              // Stop bluetooth scan
              if (DEBUG_LEVEL >= 3)
                Log.d(TAG, "Stop bluetooth scanning!");
              mBluetoothAdapter.stopLeScan(mLeScanCallBack);
              mBluetoothTime = timeNow;
              mBluetoothScan = false;
            }
            else if (!mBluetoothScan && Math.abs(timeNow - mBluetoothTime) >= mBluetoothSleepTimeout)
            {
              if (DEBUG_LEVEL >= 3)
                Log.d(TAG, "Start bluetooth scanning!");
              mBluetoothAdapter.startLeScan(mLeScanCallBack);
              mBluetoothTime = timeNow;
              mBluetoothScan = true;
            }
          }
        }
        catch (Throwable e)
        {
          if (DEBUG_LEVEL >= 1)
            Log.e(TAG, Log.getStackTraceString(e));
        }
      }
      
      // Sleeping some time
      try { Thread.sleep(SLEEP_TIMEOUT); } catch (Throwable e) { }
    } // end of loop
    
    synchronized (this)
    {
      try
      {
        if (mBluetoothAdapter != null && mLeScanCallBack != null)
          mBluetoothAdapter.stopLeScan(mLeScanCallBack);
      }
      catch (Throwable e)
      {
        if (DEBUG_LEVEL >= 1)
          Log.e(TAG, Log.getStackTraceString(e));
      }
    }
    
    Looper.myLooper().quit();
    if (DEBUG_LEVEL >= 2)
      Log.d(TAG, "Stopped");
  } // end of run()
}
