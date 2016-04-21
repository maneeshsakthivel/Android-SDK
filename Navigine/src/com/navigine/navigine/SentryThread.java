package com.navigine.navigine;
import com.navigine.navigine.*;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

public class SentryThread extends Thread
{
  public static final String  TAG             = "NAVIGINE.Sentry";
  public static final String  SENTRY_HOST     = "sentry.navigine.com";
  public static final String  SENTRY_URL      = "http://sentry.navigine.com/api/8/store/";
  public static final String  SENTRY_KEY      = "ab01e87cba7044eea00422643f4624b4";
  public static final String  SENTRY_SECRET   = "02caf6e906c944e4a41150bb71dbd7fa";
  public static final String  SENTRY_CLIENT   = "raven-python/1.0";
  public static final int     CONN_TIMEOUT    = 6000;   // milliseconds
  public static final int     SEND_TIMEOUT    = 3000;   // milliseconds
  public static final int     RECV_TIMEOUT    = 3000;   // milliseconds
  public static final int     WAIT_TIMEOUT    = 60000;  // milliseconds
  
  private boolean mStopFlag  = false;
  private String  mCrashDir  = null;
  
  private String  mCurrentFile = null;
  
  public SentryThread(String crashDir)
  {
    mCrashDir = crashDir;
    super.start();
  }
  
  public synchronized void terminate()
  {
    mStopFlag = true;
  }
  
  @Override public void run()
  {
    SocketChannel channel       = null;
    ByteBuffer    sendBuffer    = null;
    InetAddress   hostAddress   = null;
    DnsResolver   dnsResolver   = null;
    Thread        dnsThread     = null;
    
    String        httpRequest   = "";
    String        httpResponse  = "";
    
    long timeNow  = System.currentTimeMillis();
    long connTime = timeNow;
    long sendTime = timeNow;
    long recvTime = timeNow;
    long waitTime = 0;
    
    while (!mStopFlag)
    {
      // Sleeping for some time...
      try { Thread.sleep(100); } catch (Throwable e) { }
      
      timeNow = System.currentTimeMillis();
      
      if (waitTime > timeNow)
      {
        //Log.d(TAG, String.format(Locale.ENGLISH, "Waiting for %d sec", (waitTime - timeNow) / 1000));
        continue;
      }
      else if (mCurrentFile == null)
      {
        // Selecting a new file to upload
        File dir = new File(mCrashDir);
        File files[] = dir.listFiles();
        if (files == null)
          continue;
        for(int i = 0; i < files.length; ++i)
        {
          if (files[i].getName().endsWith(".json"))
          {
            Log.d(TAG, "Start uploading file: " + files[i].getName());
            mCurrentFile = files[i].getAbsolutePath();
            String content = readFile(mCurrentFile);
            if (content != null)
            {
              StringBuilder builder = new StringBuilder();
              builder.append(String.format(Locale.ENGLISH, "POST %s HTTP/1.1\r\n", SENTRY_URL));
              builder.append(String.format(Locale.ENGLISH, "Host: %s\r\n", SENTRY_HOST));
              builder.append(String.format(Locale.ENGLISH, "Connection: close\r\n"));
              builder.append(String.format(Locale.ENGLISH, "Content-Type: application/json\r\n"));
              builder.append(String.format(Locale.ENGLISH, "Content-Length: %d\r\n", content.length()));
              builder.append(String.format(Locale.ENGLISH, "X-Sentry-Auth: Sentry sentry_version=7,sentry_timestamp=%d,sentry_key=%s,sentry_secret=%s,sentry_client=%s\r\n\r\n",
                                           timeNow / 1000, SENTRY_KEY, SENTRY_SECRET, SENTRY_CLIENT));
              builder.append(content);
              
              httpRequest = builder.toString();
              sendBuffer = ByteBuffer.wrap(httpRequest.getBytes());
              sendBuffer.order(ByteOrder.LITTLE_ENDIAN);
              Log.d(TAG, httpRequest);
              break;
            }
          }
        }
        continue;
      }
      else if (channel == null)
      {
        // Resolving server address
        try
        {
          if (dnsThread == null || dnsResolver == null)
          {
            dnsResolver = new DnsResolver(SENTRY_HOST);
            dnsThread = new Thread(dnsResolver);
            dnsThread.start();
            continue;
          }
          else
          {
            if (dnsThread.isAlive())
              continue;
            
            hostAddress = dnsResolver.getAddress();
            dnsResolver = null;
            dnsThread   = null;
            
            Log.d(TAG, String.format(Locale.ENGLISH, "Hostname '%s' resolved to '%s'",
                  hostAddress.getHostName(),
                  hostAddress.getHostAddress()));
            
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(hostAddress.getHostAddress(), 80));
            connTime = timeNow;
            continue;
          }
        }
        catch (Throwable e)
        {
          closeChannel(channel);
          hostAddress = null;
          dnsResolver = null;
          dnsThread   = null;
          channel     = null;
          //Log.e(TAG, "Connection error!");
          //Log.e(TAG, Log.getStackTraceString(e));
        }
        continue;
      }
      else if (!channel.isConnected())
      {
        // Connecting to the server
        try
        {
          if (channel.finishConnect())
          {
            Log.d(TAG, "The connection has been established");
            sendTime = timeNow;
            continue;
          }
          else if (timeNow - connTime > CONN_TIMEOUT)
          {
            // Connection timeout
            closeChannel(channel);
            hostAddress = null;
            dnsResolver = null;
            dnsThread   = null;
            channel     = null;
            waitTime    = timeNow + CONN_TIMEOUT;
          }
        }
        catch (Throwable e)
        {
          //Log.e(TAG, "Connection error!");
          //Log.e(TAG, Log.getStackTraceString(e));
          //error = String.format(Locale.ENGLISH, "Unable to connect to %s:%d!\n" +
          //                    "Please, check connection parameters " +
          //                    "and network settings!",
          //                    host, port);
          //Log.e(TAG, error);
          closeChannel(channel);
          hostAddress = null;
          dnsResolver = null;
          dnsThread   = null;
          channel     = null;
        }
      }
      else if (sendBuffer.hasRemaining())
      {
        // Sending request to the server
        try
        {
          int count = channel.write(sendBuffer);
          if (count < 0)
          {
            // Send error
            Log.e(TAG, String.format(Locale.ENGLISH, "Unable to send data to %s!", SENTRY_HOST));
            closeChannel(channel);
            hostAddress = null;
            dnsResolver = null;
            dnsThread   = null;
            channel     = null;
            waitTime    = timeNow + SEND_TIMEOUT;
          }
          if (count == 0)
          {
            // Send timeout
            if (timeNow - sendTime > SEND_TIMEOUT)
            {
              Log.e(TAG, String.format(Locale.ENGLISH, "Unable to send data to %s: timeout!", SENTRY_HOST));
              closeChannel(channel);
              hostAddress = null;
              dnsResolver = null;
              dnsThread   = null;
              channel     = null;
              waitTime    = timeNow + SEND_TIMEOUT;
            }
            continue;
          }
          
          sendTime = timeNow;
          
          if (!sendBuffer.hasRemaining())
          {
            Log.d(TAG, "The request is sent to the server. Waiting for server response");
            httpResponse = "";
            recvTime = timeNow;
          }
          continue;
        }
        catch (Throwable e)
        {
          // Send error
          Log.e(TAG, String.format(Locale.ENGLISH, "Unable to send data to %s!", SENTRY_HOST));
          //Log.e(TAG, Log.getStackTraceString(e));
          closeChannel(channel);
          hostAddress = null;
          dnsResolver = null;
          dnsThread   = null;
          channel     = null;
          waitTime    = timeNow + SEND_TIMEOUT;
        }
      }
      else
      {
        // Receiving response from the server
        try
        {
          ByteBuffer recvBuffer = ByteBuffer.wrap(new byte[32768]);
          int count = channel.read(recvBuffer);
          if (count < 0)
          {
            // Recv error
            closeChannel(channel);
            hostAddress = null;
            dnsResolver = null;
            dnsThread   = null;
            channel     = null;
            waitTime    = timeNow + WAIT_TIMEOUT;
          }
          if (count == 0)
          {
            // Recv timeout
            if (timeNow - recvTime > RECV_TIMEOUT)
            {
              closeChannel(channel);
              hostAddress = null;
              dnsResolver = null;
              dnsThread   = null;
              channel     = null;
              waitTime    = timeNow + WAIT_TIMEOUT;
            }
            continue;
          }
          
          httpResponse += new String(recvBuffer.array(), 0, count);
          
          recvTime = timeNow;
          continue;
        }
        catch (Throwable e)
        {
          // Recv finished or error
          Log.d(TAG, httpResponse);
          String httpResponseUpperCase = httpResponse.toUpperCase();
          if (httpResponseUpperCase.contains("200 OK") ||
              (httpResponseUpperCase.contains("403 FORBIDDEN") &&
               httpResponseUpperCase.contains("X-SENTRY-ERROR: AN EVENT WITH THE SAME ID ALREADY EXISTS")))
          {
            Log.d(TAG, String.format(Locale.ENGLISH, "File %s was successfully processed, removing it", mCurrentFile));
            new File(mCurrentFile).renameTo(new File(mCurrentFile + ".old"));
            mCurrentFile = null;
            waitTime = 0;
          }
          else
          {
            waitTime = timeNow + WAIT_TIMEOUT;
          }
          //Log.e(TAG, Log.getStackTraceString(e));
          closeChannel(channel);
          hostAddress = null;
          dnsResolver = null;
          dnsThread   = null;
          channel     = null;
          continue;
        }
      }
      
    } // end of loop
    
    closeChannel(channel);
    hostAddress = null;
    dnsResolver = null;
    dnsThread   = null;
    channel     = null;
  } // end of run()
  
  private static String readFile(String filename)
  {
    try
    {
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      StringBuilder builder = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null)
      {
        builder.append(line);
        builder.append("\n");
      }
      reader.close();
      return builder.toString();
    }
    catch (Throwable e)
    {
      return null;
    }
  }
  
  private static void closeChannel(SocketChannel channel)
  {
    // Trying to shutdown input
    if (channel == null)
      return;
    
    try
    {
      channel.socket().shutdownInput();
    }
    catch (Throwable e)
    {
      //Log.e(TAG, Log.getStackTraceString(e));
    }
    
    // Trying to shutdown output
    try
    {
      channel.socket().shutdownOutput();
    }
    catch (Throwable e)
    {
      //Log.e(TAG, Log.getStackTraceString(e));
    }
    
    // Trying to close channel
    try
    {
      channel.close();
    }
    catch (Throwable e)
    {
      //Log.e(TAG, "Close error!");
      //Log.e(TAG, Log.getStackTraceString(e));
    }
  }
}
