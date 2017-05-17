package com.navigine.geo_service;

import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class Logger
{
  private static int    mLogLevel = 0;
  private static String mLogFile  = null;
  
  public static void d(String tag, int level, String message)
  {
    if (level <= 0 || level > mLogLevel)
      return;
    
    Log.d(tag, message);
    if (mLogFile != null)
    {
      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      calendar.setTimeInMillis(System.currentTimeMillis());
      try
      {
        FileWriter fw = new FileWriter(mLogFile, true);
        fw.write(String.format(Locale.ENGLISH, "[%04d-%02d-%02d %02d:%02d:%02d - %s]: %s\n",
                               calendar.get(Calendar.YEAR),
                               calendar.get(Calendar.MONTH) + 1,
                               calendar.get(Calendar.DAY_OF_MONTH),
                               calendar.get(Calendar.HOUR_OF_DAY),
                               calendar.get(Calendar.MINUTE),
                               calendar.get(Calendar.SECOND),
                               tag, message));
        fw.close();
      }
      catch (Throwable e)
      {
      }
    }
  }
  
  public static synchronized int getLogLevel()
  {
    return mLogLevel;
  }
  
  public static synchronized void setLogLevel(int level)
  {
    mLogLevel = Math.max(level, 0);
  }
  
  public static synchronized void setLogFile(String logFile)
  {
    mLogFile = (logFile == null ? null : new String(logFile));
  }
}
