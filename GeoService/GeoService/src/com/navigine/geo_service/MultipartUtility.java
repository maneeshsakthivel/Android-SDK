package com.navigine.geo_service;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;

// This utility class provides an abstraction layer for sending multipart HTTP
// POST requests to a web server.

public class MultipartUtility
{
  private static final String BOUNDARY    = "iuJiWoa6hiGud8hi";
  
  private HttpURLConnection mHttpConn     = null;
  private OutputStream      mOutputStream = null;
  private PrintWriter       mWriter       = null;
  
  // This constructor initializes a new HTTP POST request with content type
  // is set to multipart/form-data
  public MultipartUtility(String requestURL) throws IOException
  {
    URL url = new URL(requestURL);
    mHttpConn = (HttpURLConnection)url.openConnection();
    mHttpConn.setUseCaches(false);
    mHttpConn.setDoOutput(true);
    mHttpConn.setDoInput(true);
    mHttpConn.setRequestMethod("POST");
    mHttpConn.setRequestProperty("Connection", "close");
    mHttpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    mOutputStream = mHttpConn.getOutputStream();
    mWriter = new PrintWriter(new OutputStreamWriter(mOutputStream), true);
  }
  
  // Adds a form field to the request
  public void addFormField(String name, String value)
  {
    mWriter.append("--" + BOUNDARY + "\r\n");
    mWriter.append("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
    mWriter.append("\r\n");
    mWriter.append(value);
    mWriter.append("\r\n");
    mWriter.flush();
  }

  // Completes the request and receives response from the server
  public List<String> finish() throws IOException
  {
    List<String> response = new ArrayList<String>();
    
    mWriter.append("\r\n").flush();
    mWriter.append("--" + BOUNDARY + "--").append("\r\n");
    mWriter.close();
    
    // checks server's status code first
    int status = mHttpConn.getResponseCode();
    if (status == HttpURLConnection.HTTP_OK)
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(mHttpConn.getInputStream()));
      String line = null;
      while ((line = reader.readLine()) != null)
        response.add(line);
      reader.close();
      mHttpConn.disconnect();
    }
    else
      throw new IOException("Server returned non-OK status: " + status);
    
    return response;
  }
}
