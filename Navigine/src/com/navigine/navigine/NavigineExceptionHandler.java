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
import java.lang.Thread.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class NavigineExceptionHandler implements UncaughtExceptionHandler
{
  private UncaughtExceptionHandler mExceptionHandler = null;
  private String mCrashDir = null;
  
  public NavigineExceptionHandler(String homeDir)
  {
    mCrashDir = homeDir + "/crashes";
    mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
  }
  
  public void uncaughtException(Thread t, Throwable e)
  {
    addException(e);
    mExceptionHandler.uncaughtException(t, e);
  }
  
  public void addException(Throwable e)
  {
    final long timestamp = System.currentTimeMillis();
    Calendar calendNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendNow.setTimeInMillis(timestamp);
    final String filename = String.format(Locale.ENGLISH, "%04d%02d%02d_%02d%02d%02d.json",
                                          calendNow.get(Calendar.YEAR),
                                          calendNow.get(Calendar.MONTH) + 1,
                                          calendNow.get(Calendar.DAY_OF_MONTH),
                                          calendNow.get(Calendar.HOUR_OF_DAY),
                                          calendNow.get(Calendar.MINUTE),
                                          calendNow.get(Calendar.SECOND));
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    e.printStackTrace(printWriter);
    String stacktrace = result.toString();
    printWriter.close();
    
    stacktrace = stacktrace.replace("\t", "    ");
    stacktrace = stacktrace.replace("\"", "\\\"");
    
    String lines[] = stacktrace.split("\\r?\\n");
    String message = lines.length == 0 ? "Exception in com.navigine.navigine" : lines[0];
    String eventId = UUID.randomUUID().toString().replace("-", "");
    
    //String logcat = getLogcat();
    //logcat = logcat.replaceAll("[^\\x00-\\x7F]", "");
    //String lines2[] = logcat.split("\\r?\\n");
    
    final String deviceManufacturer = Build.MANUFACTURER.toUpperCase();
    final String deviceModel        = Build.MODEL.toUpperCase();
    final String deviceName         = deviceModel.startsWith(deviceManufacturer) ? 
                                        new String(deviceModel) :
                                        new String(deviceManufacturer) + " " + deviceModel;
    
    StringBuilder builder = new StringBuilder();
    builder.append("{\n");
    builder.append(String.format(Locale.ENGLISH, "  \"event_id\": \"%s\",\n", eventId));
    builder.append(String.format(Locale.ENGLISH, "  \"culprit\": \"com.navigine.navigine\",\n"));
    builder.append(String.format(Locale.ENGLISH, "  \"timestamp\": \"%d\",\n", timestamp/1000));
    builder.append(String.format(Locale.ENGLISH, "  \"message\": \"%s\",\n", message));
    builder.append(String.format(Locale.ENGLISH, "  \"tags\":[[\"app_version\", \"%s (%d)\"],\n" +
                                                 "            [\"build_version\", \"%s\"],\n" +
                                                 "            [\"device_api\", \"%d\"],\n" +
                                                 "            [\"device_name\",\"%s\"]],\n",
                                                 NavigineApp.VersionName,
                                                 NavigineApp.VersionCode,
                                                 A.BUILD_VERSION_BRIEF,
                                                 Build.VERSION.SDK_INT,
                                                 deviceName));
    
    builder.append(String.format(Locale.ENGLISH, "  \"extra\": {\n    \"Stack trace\": [\n"));
    for(int i = 0; i < lines.length; ++i)
    {
      String line = lines[i];
      builder.append("      \"" + lines[i] + "\"");
      if (i + 1 < lines.length)
        builder.append(",\n");
    }
    builder.append(String.format(Locale.ENGLISH, "\n    ]"));
    //builder.append(",\n    \"Logcat\": [\n"));
    //for(int i = 0; i < lines2.length; ++i)
    //{
    //  String line = lines2[i];
    //  builder.append("      \"" + lines2[i] + "\"");
    //  if (i + 1 < lines2.length)
    //    builder.append(",\n");
    //}
    //builder.append("\n    ]");
    builder.append("\n  }\n");
    builder.append("}\n");
    
    writeToFile(builder.toString(), filename);
  }
  
  private void writeToFile(String data, String fileName)
  {
    try
    {
      new File(mCrashDir).mkdirs();
      final String filePath = mCrashDir + "/" + fileName;
      final String tmpFilePath = filePath + ".part";
      BufferedWriter bos = new BufferedWriter(new FileWriter(tmpFilePath));
      bos.write(data);
      bos.flush();
      bos.close();
      new File(tmpFilePath).renameTo(new File(filePath));
    }
    catch (Throwable e)
    {
      //e.printStackTrace();
    }
  }
  
  private String getLogcat()
  {
    final String processId = Integer.toString(android.os.Process.myPid());
    try
    {
      StringBuilder builder = new StringBuilder();
      String[] command = new String[] { "logcat", "-d", "T", "200" };
      java.lang.Process process = Runtime.getRuntime().exec(command);
      
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      
      String line;
      while ((line = bufferedReader.readLine()) != null)
      {
        if (line.contains("NAVIGINE"))
        {
          builder.append(line);
          builder.append("\n");
        }
      }
      return builder.toString();
    }
    catch (Throwable e)
    {
      return null;
    }
  }
}
