package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;
import com.caverock.androidsvg.SVG;

import android.app.*;
import android.content.*;
import android.database.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
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
import java.util.regex.*;

public class MeasuringActivity extends Activity
{
  // Constants
  private static final String TAG = "NAVIGINE.MeasuringActivity";
  private static final int REQUEST_PICK_FILE  = 1;  
  private static final int UPDATE_TIMEOUT     = 100;
  private static final int SCAN_MAX_NUMBER    = 30;
  private static final int MAGNET_MAX_NUMBER  = 100;
  private static final int MIN_MEASURING_TIME = 5000; // milliseconds
  
  // This context
  private Context mContext = this;
  
  // GUI parameters
  private LinearLayout  mMeasuringLayout      = null;
  private TextView      mMeasuringPrompt      = null;
  private Button        mMeasuringStartButton = null;
  private Button        mMeasuringStopButton  = null;
  private Button        mPrevFloorButton      = null;
  private Button        mNextFloorButton      = null;
  private Button        mAddPointButton       = null;
  private Button        mAddBeaconButton      = null;
  private TextView      mCurrentFloorTextView = null;
  private ImageView     mImageView  = null;
  private ImageView     mScaleView  = null;
  private ImageView     mTargetView = null;
  private TimerTask     mTimerTask  = null;
  private Timer         mTimer      = new Timer();
  private Handler       mHandler    = new Handler();
  private boolean       mDrawScale  = true;
  
  private boolean       mMapLoaded  = false;
  
  // Image parameters
  RectF mMapRect   = null;
  int mMapWidth    = 0;
  int mMapHeight   = 0;
  int mViewWidth   = 0;
  int mViewHeight  = 0;
  Drawable mMapDrawable = null;
  PictureDrawable mPicDrawable = null;
  LayerDrawable mDrawable = null;
  
  // Multi-touch parameters
  private static final int TOUCH_MODE_SCROLL = 1;
  private static final int TOUCH_MODE_ZOOM   = 2;
  private static final int TOUCH_MODE_ROTATE = 3;
  private static final int TOUCH_SENSITIVITY = 10;
  private int mTouchMode = 0;
  private int mTouchLength = 0;
  private long mTouchTimeout = 0;
  private PointF[] mTouchPoints = new PointF[] { new PointF(0.0f, 0.0f),
                                                 new PointF(0.0f, 0.0f),
                                                 new PointF(0.0f, 0.0f) };
  
  private static final int STATE_NONE           = 0;
  private static final int STATE_POINT_READY    = 1;
  private static final int STATE_POINT_RUN      = 2;
  private static final int STATE_BEACON_READY   = 6;
  private static final int STATE_BEACON_RUN     = 7;
  private int  mState = STATE_NONE;
  private long mScanTime = 0;
  
  // Geometry parameters
  private Matrix mMatrix = null;
  private float  mRatio  = 1.0f;
  
  // Config parameters
  private float mMaxX = 0.0f;
  private float mMaxY = 0.0f;
  private float mMinRatio = 0.1f;
  private float mMaxRatio = 10.0f;
  
  // Location parameters
  private Location mLocation = null;
  private int mCurrentSubLocationIndex = -1;
  
  private int mSelectedIndex = -1;
  private MeasureObject mSelectedObject = null;
  private Map<String, List<WScanResult>> mScanMap = new TreeMap<String, List<WScanResult>>(); // Scan map for the selected object
  private List<SensorResult> mSensorResults = new ArrayList<SensorResult>(); // Sensor vectors for the selected object
  private long mMeasuringTime = 0;
  
  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "MeasuringActivity: onCreate");
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.measuring_mode);
    
    // Setting up GUI parameters
    mImageView  = (ImageView)findViewById(R.id.measuring_mode__map_image);
    mScaleView  = (ImageView)findViewById(R.id.measuring_mode__scale_image);
    mTargetView = (ImageView)findViewById(R.id.measuring_mode__center_target_image);
    mMeasuringLayout = (LinearLayout)findViewById(R.id.measuring_mode__measuring_panel_layout);
    mMeasuringPrompt = (TextView)findViewById(R.id.measuring_mode__measuring_panel_prompt_label);
    mMeasuringStartButton = (Button)findViewById(R.id.measuring_mode__measuring_panel_start_button);
    mMeasuringStopButton  = (Button)findViewById(R.id.measuring_mode__measuring_panel_stop_button);
    mPrevFloorButton = (Button)findViewById(R.id.measuring_mode__prev_floor_button);
    mNextFloorButton = (Button)findViewById(R.id.measuring_mode__next_floor_button);
    mCurrentFloorTextView = (TextView)findViewById(R.id.measuring_mode__current_floor_label);
    mMeasuringLayout.setVisibility(View.GONE);
    mPrevFloorButton.setVisibility(View.INVISIBLE);
    mNextFloorButton.setVisibility(View.INVISIBLE);
    
    mAddPointButton  = (Button)findViewById(R.id.measuring_mode__add_point_button);
    mAddBeaconButton = (Button)findViewById(R.id.measuring_mode__add_beacon_button);
    
    mImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    mImageView.setBackgroundColor(Color.argb(255, 235, 235, 235));
    mScaleView.setImageBitmap(Bitmap.createBitmap(100, 30, Bitmap.Config.ARGB_8888));
    
    mTargetView.setVisibility(View.GONE);
    mAddPointButton.setVisibility(View.GONE);
    mAddBeaconButton.setVisibility(View.GONE);
    
    NavigineApp.startScanning();
    
    // Setting up touch listener
    mImageView.setOnTouchListener(
      new OnTouchListener()
      {
        @Override public boolean onTouch(View v, MotionEvent event)
        {
          doTouch(event);
          return true;
        }
      });
    
    mMeasuringStartButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          startMeasuring();
        }
      });
    
    mMeasuringStopButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          stopMeasuring();
        }
      });
    
    mPrevFloorButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          Log.d(TAG, "Previous sub-location button clicked");
          loadPrevSubLocation();
        }
      });
    
    mNextFloorButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          Log.d(TAG, "Next sub-location button clicked");
          loadNextSubLocation();
        }
      });
    
    mAddPointButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          Log.d(TAG, "Creating point");
          setPoint();
        }
      });
    
    mAddBeaconButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          Log.d(TAG, "Creating beacon");
          if (NavigineApp.Navigation == null)
            Toast.makeText(mContext, "Can't add beacon! Navigine SDK is not available!", Toast.LENGTH_LONG).show();
          else if (!NavigineApp.Navigation.isBluetoothEnabled())
            Toast.makeText(mContext, "Can't add beacon! Enable bluetooth please!", Toast.LENGTH_LONG).show();
          else
            setBeacon();
        }
      });
  }
  
  @Override public void onDestroy()
  {
    Log.d(TAG, "MeasuringActivity destroyed");
    super.onDestroy();
    
    //if (mLocation != null)
    //{
    //  Parser.saveMeasureXml(mLocation);
    //  Parser.saveBeaconsXml(mLocation);
    //}
    NavigineApp.stopScanning();
  }
  
  @Override public void onResume()
  {
    super.onResume();
    
    // Starting interface updates
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, 500, UPDATE_TIMEOUT);
  }
  
  @Override public void onPause()
  {
    super.onPause();
    
    mTimerTask.cancel();
    mTimerTask = null;
  }
  
  private boolean tryLoadMap()
  {
    if (mMapLoaded)
      return false;
    mMapLoaded = true;
    
    if (NavigineApp.Navigation == null)
    {
      Toast.makeText(mContext, "Can't load map! Navigine SDK is not available!", Toast.LENGTH_LONG).show();
      return false;
    }
    
    String filename = NavigineApp.Navigation.getArchivePath();
    if (filename == null || filename.length() == 0)
      return false;
    
    if (!NavigineApp.Navigation.loadArchive(filename))
    {
      String error = NavigineApp.Navigation.getLastError();
      if (error != null)
        Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
      SharedPreferences.Editor editor = NavigineApp.Settings.edit();
      editor.remove("map_file");
      editor.commit();
      return false;
    }
    
    mLocation = NavigineApp.Navigation.getLocation();
    mCurrentSubLocationIndex = -1;
    mMatrix = null;
    
    if (mLocation == null)
    {
      String text = "Load map failed: no location";
      Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
      Log.e(TAG, text);
      return false;
    }
    
    if (mLocation.subLocations.size() == 0)
    {
      String text = "Load map failed: no sublocations";
      Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
      Log.e(TAG, text);
      mLocation = null;
      return false;
    }
    
    if (!loadSubLocation(0))
    {
      String text = "Load map failed: unable to load default sublocation";
      Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
      Log.e(TAG, text);
      mLocation = null;
      return false;
    }
    
    // Loading measurements
    NavigineApp.Navigation.loadMeasurements();
    
    mHandler.post(mRunnable);
    return true;
  }
  
  private boolean loadSubLocation(int index)
  {
    if (mLocation == null || index < 0 || index >= mLocation.subLocations.size())
      return false;
    
    SubLocation subLoc = mLocation.subLocations.get(index);
    Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation %s", subLoc.name));
    
    subLoc.getPicture();
    subLoc.getBitmap();
    
    if (subLoc.picture == null && subLoc.bitmap == null)
    {
      String text = "Load sublocation failed: invalid image";
      Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
      Log.e(TAG, text);
      return false;
    }
    
    if (subLoc.width == 0.0f || subLoc.height == 0.0f)
    {
      String text = String.format(Locale.ENGLISH, "Load sublocation failed: invalid size: %.2f x %.2f",
                                  subLoc.width, subLoc.height);
      Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
      Log.e(TAG, text);
      return false;
    }
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation: %.2f x %.2f\n",
                             subLoc.width, subLoc.height));
    
    mViewWidth  = mImageView.getWidth();
    mViewHeight = mImageView.getHeight();
    Log.d(TAG, String.format(Locale.ENGLISH, "View size: %dx%d", mViewWidth, mViewHeight));
    
    // Updating image view size parameters
    float pixLength = 0.0f;
    if (mMatrix != null && mMapWidth > 0 && mRatio > 0)
      pixLength = mMaxX / mMapWidth / mRatio; // Pixel length in meters
    
    // Determine absolute coordinates of the screen center
    PointF P = null;
    if (mMatrix != null)
      P = getAbsCoordinates(mViewWidth / 2, mViewHeight / 2);
    
    mMapWidth   = subLoc.picture == null ? subLoc.bitmap.getWidth()  : subLoc.picture.getWidth();
    mMapHeight  = subLoc.picture == null ? subLoc.bitmap.getHeight() : subLoc.picture.getHeight();
    mMapRect    = new RectF(0, 0, mMapWidth, mMapHeight);
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Map size: %dx%d", mMapWidth, mMapHeight));
    
    mMapDrawable = subLoc.picture == null ? new BitmapDrawable(getResources(), subLoc.bitmap) : new PictureDrawable(subLoc.picture);
    mPicDrawable = new PictureDrawable(new Picture());
    
    Drawable[] drawables = {mMapDrawable, mPicDrawable};
    mDrawable = new LayerDrawable(drawables);
    mImageView.setImageDrawable(mDrawable);
    mImageView.setScaleType(ScaleType.MATRIX);
    
    // Reinitializing map/matrix parameters
    mMatrix      = new Matrix();
    mMaxX        = subLoc.width;
    mMaxY        = subLoc.height;
    mRatio       = 1.0f;
    mMinRatio    = Math.min((float)mViewWidth / mMapWidth, (float)mViewHeight / mMapHeight);
    mMaxRatio    = Math.min((float)mViewWidth / mMapWidth * subLoc.width / 2, (float)mViewHeight / mMapHeight * subLoc.height / 2);
    mMaxRatio    = Math.max(mMaxRatio, mMinRatio);
    mDrawScale   = true;
    
    // Calculating new pixel length in meters
    if (mMapWidth > 0 && pixLength > 0.0f)
      doZoom(subLoc.width / mMapWidth / pixLength);
    
    if (P != null)
    {
      PointF Q = getScreenCoordinates(P.x, P.y);
      doScroll(mViewWidth / 2 - Q.x, mViewHeight / 2 - Q.y);
    }
    else
    {
      doScroll(mViewWidth / 2 - mMapWidth / 2, mViewHeight / 2 - mMapHeight / 2);
      doZoom(mMinRatio);
    }
    
    mCurrentSubLocationIndex = index;
    mCurrentFloorTextView.setText(String.format(Locale.ENGLISH, "%s.%s", mLocation.name, subLoc.name));
    mPrevFloorButton.setVisibility(mCurrentSubLocationIndex == 0 ? View.INVISIBLE : View.VISIBLE);
    mNextFloorButton.setVisibility(mCurrentSubLocationIndex == mLocation.subLocations.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    mHandler.post(mRunnable);
    return true;
  }
  
  private boolean loadNextSubLocation()
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return false;
    return loadSubLocation(mCurrentSubLocationIndex + 1);
  }
  
  private boolean loadPrevSubLocation()
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return false;
    return loadSubLocation(mCurrentSubLocationIndex - 1);
  }
  
  private void doScroll(float deltaX, float deltaY)
  {
    if (mMatrix == null)
      return;
    //Log.d(TAG, String.format(Locale.ENGLISH, "Scroll by vector: (%.2f, %.2f)", deltaX, deltaY));
    float maxDeltaX = mViewWidth  / 2 - mMapRect.left;
    float minDeltaX = mViewWidth  / 2 - mMapRect.right;
    float maxDeltaY = mViewHeight / 2 - mMapRect.top;
    float minDeltaY = mViewHeight / 2 - mMapRect.bottom;
    //Log.d(TAG, String.format(Locale.ENGLISH, "Scroll bounds: dx: %.2f..%.2f, dy: %.2f..%.2f",
    //      minDeltaX, maxDeltaX, minDeltaY, maxDeltaY));
    deltaX = Math.max(Math.min(deltaX, maxDeltaX), minDeltaX);
    deltaY = Math.max(Math.min(deltaY, maxDeltaY), minDeltaY);
    
    mMatrix.postTranslate(deltaX, deltaY);
    mMatrix.mapRect(mMapRect, new RectF(0, 0, mMapWidth, mMapHeight));
    //Log.d(TAG, String.format(Locale.ENGLISH, "Map rect: (%.2f, %.2f) - (%.2f, %.2f)",
    //      mMapRect.left, mMapRect.top, mMapRect.right, mMapRect.bottom));
    mHandler.post(mRunnable);
  }
  
  private void doZoom(float ratio)
  {
    if (mMatrix == null)
      return;
    //Log.d(TAG, String.format(Locale.ENGLISH, "Zoom by ratio: %.2f", ratio));
    float r = Math.max(Math.min(ratio, mMaxRatio / mRatio), mMinRatio / mRatio);
    mMatrix.postScale(r, r, mViewWidth / 2, mViewHeight / 2);
    mMatrix.mapRect(mMapRect, new RectF(0, 0, mMapWidth, mMapHeight));
    mRatio *= r;
    mDrawScale = true;
    Log.d(TAG, String.format(Locale.ENGLISH, "Ratio limits: (%.3f, %.3f), current: %.3f", mMinRatio, mMaxRatio, mRatio));
    //Log.d(TAG, String.format(Locale.ENGLISH, "Map rect: (%.2f, %.2f) - (%.2f, %.2f)",
    //      mMapRect.left, mMapRect.top, mMapRect.right, mMapRect.bottom));
    mHandler.post(mRunnable);
  }
  
  // Convert absolute coordinates (x,y) to SVG coordinates
  private PointF getSvgCoordinates(float x, float y)
  {
    return new PointF(x / mMaxX * mMapWidth, (mMaxY - y) / mMaxY * mMapHeight);
  }
  
  private float getSvgLength(float d)
  {
    return Math.max(d * mMapWidth / mMaxX, d * mMapHeight / mMaxY);
  }
  
  // Convert absolute coordinates (x,y) to screen coordinates
  private PointF getScreenCoordinates(float x, float y)
  {
    float[] pts = {x / mMaxX * mMapWidth, (mMaxY - y) / mMaxY * mMapHeight};
    mMatrix.mapPoints(pts);
    return new PointF(pts[0], pts[1]);
  }
  
  // Convert screen coordinates (x,y) to absolute coordinates
  private PointF getAbsCoordinates(float x, float y)
  {
    Matrix invMatrix = new Matrix();
    mMatrix.invert(invMatrix);
    
    float[] pts = {x, y};
    invMatrix.mapPoints(pts);
    return new PointF( pts[0] / mMapWidth  * mMaxX,
                      -pts[1] / mMapHeight * mMaxY + mMaxY);
  }
  
  private void doTouch(MotionEvent event)
  {
    long timeNow = DateTimeUtils.currentTimeMillis();
    int actionMask = event.getActionMasked();
    int pointerIndex = event.getActionIndex();
    int pointerCount = event.getPointerCount();
    
    PointF[] points = new PointF[pointerCount];
    for(int i = 0; i < pointerCount; ++i)
      points[i] = new PointF(event.getX(i), event.getY(i));
    
    //Log.d(TAG, String.format(Locale.ENGLISH, "MOTION EVENT: %d", actionMask));
    
    if (actionMask == MotionEvent.ACTION_DOWN)
    {
      mTouchPoints[0].set(points[0]);
      mTouchTimeout = timeNow + 500;
      mTouchLength = 0;
      mTouchMode = 0;
      return;
    }
    
    if (actionMask != MotionEvent.ACTION_MOVE)
    {
      mTouchTimeout = 0;
      mTouchMode = 0;
      return;
    }
    
    if (mTouchLength > TOUCH_SENSITIVITY)
      mTouchTimeout = 0;
    
    // Handling move events
    switch (pointerCount)
    {
      case 1:
        if (mTouchMode == TOUCH_MODE_SCROLL)
        {
          float deltaX = points[0].x - mTouchPoints[0].x;
          float deltaY = points[0].y - mTouchPoints[0].y;
          mTouchLength += Math.abs(deltaX) + Math.abs(deltaY);
          doScroll(deltaX, deltaY);
          mImageView.setImageMatrix(mMatrix);
        }
        mTouchMode = TOUCH_MODE_SCROLL;
        mTouchPoints[0].set(points[0]);
        break;
        
      case 2:
        if (mTouchMode == TOUCH_MODE_ZOOM)
        {
          float oldDist = PointF.length(mTouchPoints[0].x - mTouchPoints[1].x, mTouchPoints[0].y - mTouchPoints[1].y);
          float newDist = PointF.length(points[0].x - points[1].x, points[0].y - points[1].y);
          oldDist = Math.max(oldDist, 1.0f);
          newDist = Math.max(newDist, 1.0f);
          float ratio = newDist / oldDist;
          doZoom(ratio);
          mImageView.setImageMatrix(mMatrix);
          drawScale();
        }
        mTouchMode = TOUCH_MODE_ZOOM;
        mTouchPoints[0].set(points[0]);
        mTouchPoints[1].set(points[1]);
        break;
    }
  }
  
  private void drawArrow(PointF A, PointF B, Paint paint, Canvas canvas)
  {
    float ux = B.x - A.x;
    float uy = B.y - A.y;
    float n = (float)Math.sqrt(ux * ux + uy * uy);
    float m = Math.min(15.0f, n / 3);
    float k = m / n;
    
    PointF C = new PointF(k * A.x + (1 - k) * B.x, k * A.y + (1 - k) * B.y);
    PointF D = new PointF(2 * k * A.x + (1 - 2 * k) * B.x, 2 * k * A.y + (1 - 2 * k) * B.y);
    
    float wx = -uy * m / n;
    float wy =  ux * m / n;
    
    PointF E = new PointF(C.x + wx / 3, C.y + wy / 3);
    PointF F = new PointF(C.x - wx / 3, C.y - wy / 3);
    
    Path path = new Path();
    path.moveTo(B.x, B.y);
    path.lineTo(E.x, E.y);
    path.lineTo(F.x, F.y);
    path.lineTo(B.x, B.y);
    
    canvas.drawLine(A.x, A.y, B.x, B.y, paint);
    canvas.drawPath(path, paint);
  }
  
  private void drawBeacon(PointF P, float size, Paint paint, Canvas canvas)
  {
    int color = paint.getColor();
    canvas.drawCircle(P.x, P.y, size, paint);
    paint.setARGB(255, 255, 255, 255);
    canvas.drawCircle(P.x, P.y, size * 0.66f, paint);
    paint.setColor(color);
    canvas.drawCircle(P.x, P.y, size * 0.33f, paint);
  }
  
  private void drawMeasureObjects(Canvas canvas)
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    float ratio = Math.min(Math.max(mRatio, mMinRatio), mMaxRatio);
    float textSize = Math.round(24.0f / ratio + 1.0f);
    float crossSize = 10.0f / ratio;
    
    Paint drawPaint = new Paint();
    drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    drawPaint.setStrokeWidth(0);
    
    Paint textPaint = new Paint();
    textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    textPaint.setStrokeWidth(0);
    textPaint.setTextSize(textSize);
    
    for(int i = 0; i < subLoc.measureList.size(); ++i)
    {
      MeasureObject object = subLoc.measureList.get(i);
      if (object.subLocation != subLoc.id)
        continue;
      
      // Ignore deleted objects
      if (object.status == MeasureObject.STATUS_DEL)
        continue;
      
      switch (object.type)
      {
        case MeasureObject.MEASURE_POINT:
        {
          drawPaint.setStrokeWidth(4.0f / ratio);
          drawPaint.setARGB(255, 160, 0, 0);
          textPaint.setARGB(255, 160, 0, 0);
          String text = object.status == 0 ?
                        String.format(Locale.ENGLISH, "%s (%d%%)", object.name, object.quality):
                        String.format(Locale.ENGLISH, "%s* (%d%%)", object.name, object.quality);
          PointF Q = new PointF(object.kx1 * subLoc.width, object.ky1 * subLoc.height);
          PointF P = getSvgCoordinates(Q.x, Q.y);
          canvas.drawLine(P.x - crossSize, P.y - crossSize, P.x + crossSize, P.y + crossSize, drawPaint);
          canvas.drawLine(P.x - crossSize, P.y + crossSize, P.x + crossSize, P.y - crossSize, drawPaint);
          //drawBeacon(P, crossSize, drawPaint, canvas);
          canvas.drawText(text, P.x + crossSize, P.y, textPaint);
          break;
        }
        
        case MeasureObject.BEACON:
        {
          drawPaint.setARGB(255, 0, 0, 255);
          textPaint.setARGB(255, 0, 0, 255);
          String text = object.status == 0 ?
                        String.format(Locale.ENGLISH, "%s", object.name):
                        String.format(Locale.ENGLISH, "%s*", object.name);
          PointF Q = new PointF(object.kx1 * subLoc.width, object.ky1 * subLoc.height);
          PointF P = getSvgCoordinates(Q.x, Q.y);
          drawBeacon(P, crossSize, drawPaint, canvas);
          canvas.drawText(text, P.x + crossSize, P.y, textPaint);
          break;
        }
      }
    }
  }
  
  private void drawGrid(Canvas canvas)
  {
    if (mMatrix == null)
      return;
    
    float ratio = Math.min(Math.max(mRatio, mMinRatio), mMaxRatio);
    float length = 100.0f / mRatio / mMapWidth * mMaxX;
    float textSize = Math.round(20.0f / ratio);
    
    Paint paint = new Paint();
    paint.setStrokeWidth(0);
    paint.setARGB(255, 40, 40, 40);
    paint.setTextSize(textSize);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    
    Paint paint2 = new Paint();
    paint2.setStrokeWidth(0);
    paint2.setARGB(160, 160, 160, 160);
    paint2.setTextSize(textSize);
    paint2.setStyle(Paint.Style.FILL_AND_STROKE);
    
    float[] steps = { 1.0f,     2.0f,     5.0f,
                      10.0f,    20.0f,    50.0f,
                      100.0f,   200.0f,   500.0f,
                      1000.0f,  2000.0f,  5000.0f };
    float step = steps[0];
    
    for(int i = 0; i < steps.length && steps[i] < length; ++i)
      step = steps[i];
    
    float x = 0.0f;
    while (x < mMaxX)
    {
      PointF W = getScreenCoordinates(x, 0.0f);
      if (W.x < -mViewWidth/2 || W.x > 3*mViewWidth/2)
      {
        x += step;
        continue;
      }
      
      for(int i = 0; i < 5; ++i)
      {
        PointF P = getSvgCoordinates(x, 0.0f);
        PointF Q = getSvgCoordinates(x, mMaxY);
        canvas.drawLine(P.x, P.y, Q.x, Q.y, i == 0 ? paint : paint2);
        if (i == 0)
        {
          PointF R = getScreenCoordinates(x, 0.0f);
          if (R.y > mViewHeight - 1)
            R.y = mViewHeight - 1;
          PointF S = getAbsCoordinates(R.x, R.y);
          PointF T = getSvgCoordinates(S.x, S.y);
          canvas.drawText(String.format(Locale.ENGLISH, "%.0f", x), T.x + 1, T.y - 1, paint);
        }
        x += step/5;
      }
    }
    
    float y = 0.0f;
    while (y < mMaxY)
    {
      PointF W = getScreenCoordinates(0.0f, y);
      if (W.y < -mViewHeight/2 || W.y > 3*mViewHeight/2)
      {
        y += step;
        continue;
      }
      
      for(int i = 0; i < 5; ++i)
      {
        PointF P = getSvgCoordinates(0.0f, y);
        PointF Q = getSvgCoordinates(mMaxX, y);
        canvas.drawLine(P.x, P.y, Q.x, Q.y, i == 0 ? paint : paint2);
        if (i == 0)
        {
          PointF R = getScreenCoordinates(0.0f, y);
          if (R.x < 1)
            R.x = 1;
          PointF S = getAbsCoordinates(R.x, R.y);
          PointF T = getSvgCoordinates(S.x, S.y);
          canvas.drawText(String.format(Locale.ENGLISH, "%.0f", y), T.x + 1, T.y - 1, paint);
        }
        y += step/5;
      }
    }
  }
  
  private void drawScale()
  {
    if (mMatrix == null)
      return;
    
    // Preparing canvas
    Bitmap bitmap = ((BitmapDrawable)mScaleView.getDrawable()).getBitmap();
    bitmap.eraseColor(Color.TRANSPARENT);
    Canvas canvas = new Canvas(bitmap);
    
    // Calculate scale meter-length
    float length = 70.0f / mRatio / mMapWidth * mMaxX;
    String text = "";
    
    if (length < 0.1f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length * 100), getString(R.string.centimeters));
    else if (length >= 0.1f && length < 1.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length * 10) * 10, getString(R.string.centimeters));
    else if (length >= 1.0f && length < 2.0f)
      text = String.format(Locale.ENGLISH, "%.1f %s", Math.round(length * 5) / 5.0f, getString(R.string.meters));
    else if (length >= 2.0f && length < 5.0f)
      text = String.format(Locale.ENGLISH, "%.1f %s", Math.round(length * 2) / 2.0f, getString(R.string.meters));
    else if (length >= 5.0f && length < 10.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length), getString(R.string.meters));
    else if (length >= 10.0f && length < 20.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 2) * 2, getString(R.string.meters));
    else if (length >= 20.0f && length < 50.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 5) * 5, getString(R.string.meters));
    else if (length >= 50.0f && length < 100.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 10) * 10, getString(R.string.meters));
    else if (length >= 100.0f && length < 200.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 20) * 20, getString(R.string.meters));
    else if (length >= 200.0f && length < 500.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 50) * 50, getString(R.string.meters));
    else if (length >= 500.0f && length < 1000.0f)
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 100) * 100, getString(R.string.meters));
    else if (length >= 1000.0f && length < 2000.0f)
      text = String.format(Locale.ENGLISH, "%.1f %s", Math.round(length / 200) / 5.0f, getString(R.string.kilometers));
    else if (length >= 2000.0f && length < 5000.0f)
      text = String.format(Locale.ENGLISH, "%.1f %s", Math.round(length / 500) / 2.0f, getString(R.string.kilometers));
    else
      text = String.format(Locale.ENGLISH, "%d %s", Math.round(length / 1000), getString(R.string.kilometers));
    
    // Draw text
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.BLACK);
    paint.setStrokeWidth(0);
    
    Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    fillPaint.setStyle(Paint.Style.FILL);
    fillPaint.setColor(Color.BLACK);
    
    canvas.drawRect(10, 18, 80, 28, fillPaint);
    fillPaint.setColor(Color.WHITE);
    canvas.drawRect(24, 18, 38, 28, fillPaint);
    canvas.drawRect(52, 18, 66, 28, fillPaint);
    
    float textWidth = paint.measureText(text);
    canvas.drawText(text, 45 - textWidth / 2, 15, paint);
    canvas.drawRect(10, 18, 80, 28, paint);
  }
  
  private String suggestObjectName(int type)
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return "";
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    switch (type)
    {
      case MeasureObject.MEASURE_POINT:
        return String.format(Locale.ENGLISH, "P.%d.%d", subLoc.id, subLoc.measureList.size() + 1);
      
      case MeasureObject.BEACON:
        return String.format(Locale.ENGLISH, "B.%d.%d", subLoc.id, subLoc.measureList.size() + 1);
      
      default:
        return String.format(Locale.ENGLISH, "%d.%d", subLoc.id, subLoc.measureList.size() + 1);
    }
  }
  
  // Function is called, when menu option "Create point" is selected
  private void setPoint()
  {
    if (NavigineApp.Navigation == null)
      return;
    
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    if (mState == STATE_NONE)
    {
      PointF P = getAbsCoordinates(mViewWidth / 2, mViewHeight / 2);
      mSelectedObject               = new MeasureObject();
      mSelectedObject.status        = MeasureObject.STATUS_NEW;
      mSelectedObject.type          = MeasureObject.MEASURE_POINT;
      mSelectedObject.name          = suggestObjectName(MeasureObject.MEASURE_POINT);
      mSelectedObject.uuid          = UUID.randomUUID().toString();
      mSelectedObject.kx1           = P.x / subLoc.width;
      mSelectedObject.ky1           = P.y / subLoc.height;
      mSelectedObject.location      = mLocation.id;
      mSelectedObject.subLocation   = subLoc.id;
      mSelectedObject.deviceId      = NavigineApp.Navigation.getDeviceId();
      mSelectedObject.deviceModel   = NavigineApp.Navigation.getDeviceName();
      mSelectedObject.deviceAddress = NavigineApp.Navigation.getMacAddress();
      showSelectedObjectDialog(false);
    }
  }
  
  // Function is called, when menu option "Create beacon" is selected
  private void setBeacon()
  {
    if (NavigineApp.Navigation == null)
      return;
    
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    if (mState == STATE_NONE)
    {
      PointF P = getAbsCoordinates(mViewWidth / 2, mViewHeight / 2);
      mSelectedObject             = new MeasureObject();
      mSelectedObject.status      = MeasureObject.STATUS_NEW;
      mSelectedObject.type        = MeasureObject.BEACON;
      mSelectedObject.name        = suggestObjectName(MeasureObject.BEACON);
      mSelectedObject.kx1         = P.x / subLoc.width;
      mSelectedObject.ky1         = P.y / subLoc.height;
      mSelectedObject.location    = mLocation.id;
      mSelectedObject.subLocation = subLoc.id;
      showSelectedObjectDialog(false);
    }
  }
  
  // Function is called, when menu option "Cancel point/line" is selected
  private void cancelObject()
  {
    if (NavigineApp.Navigation == null)
      return;
    
    if (mLocation == null)
      return;
    
    if (mSelectedObject != null &&
        mSelectedObject.status == MeasureObject.STATUS_NEW &&
        mSelectedIndex >= 0)
    {
      int type = mSelectedObject.type;
      Log.d(TAG, String.format(Locale.ENGLISH, "Cancel object: %s (index=%d)",
            mSelectedObject.name, mSelectedIndex));
      
      SubLocation subLoc = mLocation.getSubLocation(mSelectedObject.subLocation);
      Log.d(TAG, String.format(Locale.ENGLISH, "Cancel object: %s",
            subLoc.measureList.get(mSelectedIndex).name));
      
      if (subLoc != null)
      {
        subLoc.measureList.remove(mSelectedIndex);
        switch (type)
        {
          case MeasureObject.MEASURE_POINT:
            Parser.saveMeasureXml(mLocation);
            break;
          
          case MeasureObject.BEACON:
            Parser.saveBeaconsXml(mLocation);
            break;
        }
      }
    }
    mSelectedIndex = -1;
    mSelectedObject = null;
    mState = STATE_NONE;
  }
  
  private float lineDist(float ax, float ay, float bx, float by, float x, float y)
  {
    float ab = PointF.length(ax - bx, ay - by);
    float oa = PointF.length(ax - x, ay - y);
    float ob = PointF.length(bx - x, by - y);
    
    if (oa * oa > ob * ob + ab * ab)
      return ob;
    
    if (ob * ob > oa * oa + ab * ab)
      return oa;
    
    float p = (ab + oa + ob) / 2;
    float S = (float)Math.sqrt(p * (p - ab) * (p - oa) * (p - ob));
    return 2 * S / ab;
  }
  
  private void doLongTouch(float x, float y)
  {
    if (mState != STATE_NONE)
      return;
    
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    mSelectedObject = null;
    float minDist = -1.0f;
    
    // Given screen coordinates (x, y) determine if we
    // have clicked at some measure object or not
    for(int i = 0; i < subLoc.measureList.size(); ++i)
    {
      MeasureObject object = subLoc.measureList.get(i);
      if (object.subLocation != subLoc.id)
        continue;
      
      if (object.status == MeasureObject.STATUS_DEL)
        continue;
      
      float dist = -1.0f;
      switch (object.type)
      {
        case MeasureObject.MEASURE_POINT:
        {
          PointF Q = new PointF(object.kx1 * subLoc.width, object.ky1 * subLoc.height);
          PointF P = getScreenCoordinates(Q.x, Q.y);
          dist = PointF.length(x - P.x, y - P.y);
          break;
        }
        
        case MeasureObject.BEACON:
        {
          PointF Q = new PointF(object.kx1 * subLoc.width, object.ky1 * subLoc.height);
          PointF P = getScreenCoordinates(Q.x, Q.y);
          dist = PointF.length(x - P.x, y - P.y);
          break;
        }
        
        default:
          continue;
      }
      
      if (dist < 0.0f || dist > 30.0f)
        continue;
      
      if (mSelectedObject == null || dist < minDist)
      {
        mSelectedObject = object;
        mSelectedIndex  = i;
        minDist = dist;
      }
    }
    
    if (mSelectedObject != null)
      showSelectedObjectDialog(true);
  }
  
  private EditText _xEdit             = null;
  private EditText _yEdit             = null;
  private TextView _nameLabel         = null;
  private EditText _nameEdit          = null;
  private TextView _beaconMajorLabel  = null;
  private EditText _beaconMajorEdit   = null;
  private TextView _beaconMinorLabel  = null;
  private EditText _beaconMinorEdit   = null;
  private TextView _beaconUuidLabel   = null;
  private EditText _beaconUuidEdit    = null;
  private Button   _deleteButton      = null;
  private Button   _startButton       = null;
  private Button   _cancelButton      = null;
  private boolean  _existing          = false;
  
  private AlertDialog _alertDialog = null;
  
  private void showSelectedObjectDialog(boolean existing)
  {
    if (mLocation == null)
      return;
    
    if (mSelectedObject == null)
      return;
    
    if (mSelectedObject.status == MeasureObject.STATUS_DEL)
      return;
    
    if (mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Show measure object dialog: %s", mSelectedObject.name));
    
    _existing = existing;
    
    LayoutInflater inflater = getLayoutInflater();
    View view         = inflater.inflate(R.layout.measuring_object_dialog, null);
    _nameLabel        = (TextView)view.findViewById(R.id.measuring_object_dialog__name_label);
    _nameEdit         = (EditText)view.findViewById(R.id.measuring_object_dialog__name_edit);
    _beaconMajorLabel = (TextView)view.findViewById(R.id.measuring_object_dialog__beacon_major_label);
    _beaconMajorEdit  = (EditText)view.findViewById(R.id.measuring_object_dialog__beacon_major_edit);
    _beaconMinorLabel = (TextView)view.findViewById(R.id.measuring_object_dialog__beacon_minor_label);
    _beaconMinorEdit  = (EditText)view.findViewById(R.id.measuring_object_dialog__beacon_minor_edit);
    _beaconUuidLabel  = (TextView)view.findViewById(R.id.measuring_object_dialog__beacon_uuid_label);
    _beaconUuidEdit   = (EditText)view.findViewById(R.id.measuring_object_dialog__beacon_uuid_edit);
    _deleteButton     = (Button)view.findViewById(R.id.measuring_object_dialog__delete_button);
    _startButton      = (Button)view.findViewById(R.id.measuring_object_dialog__start_button);
    _cancelButton     = (Button)view.findViewById(R.id.measuring_object_dialog__cancel_button);
    
    _nameLabel.setVisibility(existing ? View.GONE : View.VISIBLE);
    _nameEdit.setVisibility (existing ? View.GONE : View.VISIBLE);
    
    if (!existing)
      _nameEdit.setText(suggestObjectName(mSelectedObject.type));
    
    _beaconMajorEdit.setFocusable(false); _beaconMajorEdit.setCursorVisible(false);
    _beaconMinorEdit.setFocusable(false); _beaconMinorEdit.setCursorVisible(false);
    _beaconUuidEdit.setFocusable(false);  _beaconUuidEdit.setCursorVisible(false);
    
    _cancelButton.setVisibility(View.GONE);
    _deleteButton.setVisibility(View.GONE);
    
    _xEdit  = (EditText)view.findViewById(R.id.measuring_object_dialog__x_edit);
    _yEdit  = (EditText)view.findViewById(R.id.measuring_object_dialog__y_edit);
    _xEdit.setFocusable(!existing); _xEdit.setCursorVisible(!existing);
    _yEdit.setFocusable(!existing); _yEdit.setCursorVisible(!existing);
    _xEdit.setText(String.format(Locale.ENGLISH, "%.2f", mSelectedObject.kx1 * subLoc.width));
    _yEdit.setText(String.format(Locale.ENGLISH, "%.2f", mSelectedObject.ky1 * subLoc.height));
    
    String title = getString(R.string.set_measuring_object);
    switch (mSelectedObject.type)
    {
      case MeasureObject.MEASURE_POINT:
        title = existing ? String.format(Locale.ENGLISH, "Point  '%s'", mSelectedObject.name) : "New measuring point:";
        _beaconMajorLabel.setVisibility(View.GONE);
        _beaconMajorEdit.setVisibility(View.GONE);
        _beaconMinorLabel.setVisibility(View.GONE);
        _beaconMinorEdit.setVisibility(View.GONE);
        _beaconUuidLabel.setVisibility(View.GONE);
        _beaconUuidEdit.setVisibility(View.GONE);
        break;
        
      case MeasureObject.BEACON:
        title = existing ? String.format(Locale.ENGLISH, "Beacon  '%s'", mSelectedObject.name) : "New beacon:";
        if (mSelectedObject.uuid.length() == 0)
        {
          _beaconMajorLabel.setVisibility(View.GONE);
          _beaconMajorEdit.setVisibility(View.GONE);
          _beaconMinorLabel.setVisibility(View.GONE);
          _beaconMinorEdit.setVisibility(View.GONE);
          _beaconUuidLabel.setVisibility(View.GONE);
          _beaconUuidEdit.setVisibility(View.GONE);
        }
        else
        {
          _beaconMajorLabel.setVisibility(View.VISIBLE);
          _beaconMajorEdit.setVisibility(View.VISIBLE);
          _beaconMinorLabel.setVisibility(View.VISIBLE);
          _beaconMinorEdit.setVisibility(View.VISIBLE);
          _beaconUuidLabel.setVisibility(View.VISIBLE);
          _beaconUuidEdit.setVisibility(View.VISIBLE);
          _beaconMajorEdit.setText(Integer.toString(mSelectedObject.beaconMajor));
          _beaconMinorEdit.setText(Integer.toString(mSelectedObject.beaconMinor));
          _beaconUuidEdit.setText(mSelectedObject.uuid);
        }
        break;
    }
    
    _startButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (_alertDialog == null)
            return;
          
          if (mLocation == null)
            return;
          
          if (mSelectedObject == null)
            return;
          
          SubLocation subLoc = mLocation.getSubLocation(mSelectedObject.subLocation);
          
          if (subLoc == null)
            return;
          
          Log.d(TAG, "Measuring dialog accepted");
          float x1, y1, x2, y2;
          try { x1 = Float.parseFloat(_xEdit.getText().toString()); } catch (Throwable e) { x1 = 0.0f; }
          try { y1 = Float.parseFloat(_yEdit.getText().toString()); } catch (Throwable e) { y1 = 0.0f; }
          mSelectedObject.name = _nameEdit.getText().toString();
          mSelectedObject.kx1 = x1 / subLoc.width;
          mSelectedObject.ky1 = y1 / subLoc.height;
          
          if (subLoc != null && !_existing)
          {
            Log.d(TAG, String.format(Locale.ENGLISH, "Saving object '%s'", mSelectedObject.name));
            subLoc.measureList.add(mSelectedObject);
            mSelectedIndex = subLoc.measureList.size() - 1;
          }
          
          if (mSelectedObject.status == 0)
            mSelectedObject.status = MeasureObject.STATUS_MOD;
          
          switch (mSelectedObject.type)
          {
            case MeasureObject.MEASURE_POINT: mState = STATE_POINT_READY;  break;
            case MeasureObject.BEACON:        mState = STATE_BEACON_READY; break;
          }
          startMeasuring();
          _alertDialog.cancel();
        }
      });
    
    if (!existing)
    {
      _cancelButton.setVisibility(View.VISIBLE);
      _cancelButton.setOnClickListener(
        new OnClickListener()
        {
          @Override public void onClick(View v)
          {
            if (_alertDialog != null)
              _alertDialog.cancel();
          }
        });
    }
    
    if (existing)
    {
      _deleteButton.setVisibility(View.VISIBLE);
      _deleteButton.setOnClickListener(
        new OnClickListener()
        {
          @Override public void onClick(View v)
          {
            if (_alertDialog == null)
              return;
            
            if (mSelectedObject == null)
              return;
            
            SubLocation subLoc = mLocation.getSubLocation(mSelectedObject.subLocation);
            if (subLoc != null)
            {
              Log.d(TAG, String.format(Locale.ENGLISH, "Removing object '%s'", mSelectedObject.name));
              
              int type = mSelectedObject.type;
              if (mSelectedObject.status == MeasureObject.STATUS_NEW)
              {
                mSelectedObject.status = MeasureObject.STATUS_DEL;
                mSelectedObject.entries.clear();
                subLoc.measureList.remove(mSelectedIndex);
              }
              else
              {
                mSelectedObject.status = MeasureObject.STATUS_DEL;
                mSelectedObject.entries.clear();
              }
              
              switch (type)
              {
                case MeasureObject.MEASURE_POINT:
                  Parser.saveMeasureXml(mLocation);
                  break;
                
                case MeasureObject.BEACON:
                  Parser.saveBeaconsXml(mLocation);
                  break;
              }
            }
            mSelectedIndex  = -1;
            mSelectedObject = null;
            mState = STATE_NONE;
            _alertDialog.cancel();
          }
        });
    }
    
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
    alertBuilder.setView(view);
    alertBuilder.setTitle(title);
    
    _alertDialog = alertBuilder.create();
    _alertDialog.setCanceledOnTouchOutside(false);
    _alertDialog.show();
  }
  
  private void addScanResult(WScanResult result)
  {
    if (mSelectedObject == null)
      return;
    
    // Adding result to the scan map
    if (!mScanMap.containsKey(result.BSSID))
      mScanMap.put(new String(result.BSSID), new ArrayList<WScanResult>());
    mScanMap.get(result.BSSID).add(result);
    
    String entryType = "";
    switch (result.type)
    {
      case WScanResult.TYPE_WIFI:   entryType = "WIFI";   break;
      case WScanResult.TYPE_BLE:    entryType = "BLE";    break;
      case WScanResult.TYPE_BEACON: entryType = "BEACON"; break;
    }
    
    Log.d(TAG, String.format(Locale.ENGLISH, "MeasureObject %s: %s; SSID=\"%s\"; BSSID=\"%s\", RSSI=\"%.2f\"",
          mSelectedObject.name, entryType, result.SSID, result.BSSID, (float)result.level));
  }
  
  private void addSensorResult(SensorResult result)
  {
    if (mSelectedObject == null)
      return;
    
    mSensorResults.add(result);
    
    String entryType = "";
    switch (result.type)
    {
      case SensorResult.TYPE_ACCELEROMETER: entryType = "ACCEL";  break;
      case SensorResult.TYPE_MAGNETOMETER:  entryType = "MAGNET"; break;
      case SensorResult.TYPE_GYROSCOPE:     entryType = "GYRO";   break;
    }
    
    Log.d(TAG, String.format(Locale.ENGLISH, "MeasureObject %s: %s; VALUES=\"(%.4f, %.4f, %.4f)\"",
          mSelectedObject.name, entryType, result.values[0], result.values[1], result.values[2]));
  }
  
  private MeasureObject findBeacon(String uuid, int major, int minor)
  {
    for(int i = 0; i < mLocation.subLocations.size(); ++i)
    {
      SubLocation subLoc = mLocation.subLocations.get(i);
      for(int j = 0; j < subLoc.measureList.size(); ++j)
      {
        MeasureObject object = subLoc.measureList.get(j);
        if (object.type == MeasureObject.BEACON &&
            object.status != MeasureObject.STATUS_DEL &&
            object.beaconMajor == major &&
            object.beaconMinor == minor &&
            object.uuid.equals(uuid))
          return object;
      }
    }
    return null;
  }
  
  private String getCloseBeacon()
  {
    long timeNow = DateTimeUtils.currentTimeMillis();
    if (timeNow - mMeasuringTime < MIN_MEASURING_TIME)
      return null;
    
    float [] minDist  = {1e10f, 1e10f};
    int   [] minPower = {0, 0};
    String[] minBssid = {"", ""};
    
    for(Map.Entry<String, List<WScanResult>> entry : mScanMap.entrySet())
    {
      List<WScanResult> scanResults = entry.getValue();
      List<WScanResult> scanResultsNew = new ArrayList<WScanResult>();
      for(int i = 0; i < scanResults.size(); ++i)
      {
        WScanResult result = scanResults.get(i);
        if (Math.abs(timeNow - result.time) < 5000)
          scanResultsNew.add(new WScanResult(result));
      }
      
      if (scanResultsNew.isEmpty())
        continue;
      
      String bssid  = entry.getKey();
      int    type   = scanResultsNew.get(0).type;
      int    freq   = scanResultsNew.get(0).frequency;
      int    power  = scanResultsNew.get(0).power;
      
      if (type != WScanResult.TYPE_BEACON)
        continue;
      
      float dist = 1e10f;
      for(int i = 0; i < scanResultsNew.size(); ++i)
      {
        WScanResult result = scanResultsNew.get(i);
        if (dist > result.distance)
          dist = result.distance;
      }
      
      if (dist > 1e9f)
        continue;
      
      Log.d(TAG, String.format(Locale.ENGLISH, "Beacon %s: distance: %.2fm", bssid, dist));
      
      if (dist < minDist[0] || minDist[0] > 1e9f)
      {
        minDist[1]  = minDist[0];
        minDist[0]  = dist;
        minBssid[1] = minBssid[0];
        minBssid[0] = bssid;
        minPower[1] = minPower[0];
        minPower[0] = power;
      }
      else if (dist < minDist[1] || minDist[1] > 1e9f)
      {
        minDist[1] = dist;
        minBssid[1] = bssid;
        minPower[1] = power;
      }
    }
    
    if (minDist[0] < Math.min(0.5f, minDist[1] - 0.5f))
    {
      Log.d(TAG, String.format(Locale.ENGLISH, "Close beacon found: %s (d0=%.2fm, power=%d)", minBssid[0], minDist[0], minPower[0]));
      return String.format(Locale.ENGLISH, "%s,%d", minBssid[0], minPower[0]);
    }
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Unable to detect close beacon (d0=%.2fm, d1=%.2fm)! Continue measuring!",
          minDist[0], minDist[1]));
    return null;
  }
  
  private int getMeasuringQuality()
  {
    long timeNow = DateTimeUtils.currentTimeMillis();
    if (timeNow - mMeasuringTime < MIN_MEASURING_TIME)
      return 0;
    
    int i = 0;
    int[] scanNumbers = new int[mScanMap.size()];
    for(Map.Entry<String, List<WScanResult>> entry : mScanMap.entrySet())
      scanNumbers[i++] = entry.getValue().size();
    
    Arrays.sort(scanNumbers);
    int num1 = scanNumbers.length >= 1 ? scanNumbers[scanNumbers.length - 1] : 0;
    int num2 = scanNumbers.length >= 2 ? scanNumbers[scanNumbers.length - 2] : 0;
    int num3 = scanNumbers.length >= 3 ? scanNumbers[scanNumbers.length - 3] : 0;
    int num4 = scanNumbers.length >= 4 ? scanNumbers[scanNumbers.length - 4] : 0;
    int num5 = scanNumbers.length >= 5 ? scanNumbers[scanNumbers.length - 5] : 0;
    return (num5 * 50 + num4 * 30 + num3 * 20) / SCAN_MAX_NUMBER;
  }
  
  private int getEntryCount(int type, long timeLabel)
  {
    int counter = 0;
    for(Map.Entry<String, List<WScanResult>> entry : mScanMap.entrySet())
    {
      List<WScanResult> results = entry.getValue();
      for(int i = 0; i < results.size(); ++i)
      {
        WScanResult result = results.get(i);
        if (result.type == type && result.time >= timeLabel)
          ++counter;
      }
    }
    return counter;
  }
  
  private void startMeasuring()
  {
    if (mSelectedObject == null)
      return;
    
    Log.d(TAG, String.format(Locale.ENGLISH, "Start measuring for selected object '%s'",
                             mSelectedObject.name));
    
    String text = "";
    switch (mState)
    {
      case STATE_POINT_READY:
        mState = STATE_POINT_RUN;
        text = "Keep your device motionless while measuring";
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        break;
      
      case STATE_BEACON_READY:
        mState = STATE_BEACON_RUN;
        text = "Keep beacon close to the device while measuring";
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        break;
      
      default:
        return;
    }
    
    long timeNow = DateTimeUtils.currentTimeMillis();
    mSelectedObject.entries = new ArrayList<String>();
    mSelectedObject.timeLabel = DateTimeUtils.currentDate(timeNow);
    mMeasuringTime = mScanTime = timeNow;
    mSensorResults.clear();
    mScanMap.clear();
  }
  
  private void stopMeasuring()
  {
    if (mSelectedObject == null)
      return;
    
    if (mSelectedObject.status == MeasureObject.STATUS_DEL)
      return;
    
    long timeNow = DateTimeUtils.currentTimeMillis();
    String message = "";
    Log.d(TAG, String.format(Locale.ENGLISH, "Stop measuring for selected object '%s'",
                             mSelectedObject.name));
    
    long duration = timeNow - mMeasuringTime;
    int  quality  = getMeasuringQuality();

    switch (mState)
    {
      case STATE_POINT_RUN:
      {
        if (quality == 0)
        {
          cancelObject();
          return;
        }
        for(Map.Entry<String, List<WScanResult>> entry : mScanMap.entrySet())
        {
          List<WScanResult> scanResults = entry.getValue();
          if (scanResults.isEmpty())
            continue;
          
          String bssid    = entry.getKey();
          String ssid     = scanResults.get(0).SSID;
          int    type     = scanResults.get(0).type;
          int    freq     = scanResults.get(0).frequency;
          int    power    = scanResults.get(0).power;
          int    battery  = scanResults.get(0).battery;
          
          SparseArray<Integer> levels = new SparseArray<Integer>();
          
          for(int i = 0; i < scanResults.size(); ++i)
          {
            WScanResult result = scanResults.get(i);
            int key = -result.level;
            Integer value = null;
            if ((value = levels.get(key)) == null)
              levels.put(key, new Integer(1));
            else
              levels.put(key, new Integer(value.intValue() + 1)); 
          }
          
          String value = "";
          for(int i = 0; i < levels.size(); ++i)
          {
            if (value.length() > 0)
              value += " ";
            value += String.format(Locale.ENGLISH, "%d:%d", levels.keyAt(i), levels.valueAt(i));
          }
          
          switch (type)
          {
            case WScanResult.TYPE_WIFI:
              message = String.format(Locale.ENGLISH, "<entry type=\"WIFI\" bssid=\"%s\" frequency=\"%d\" value=\"%s\"/>",
                                      bssid, freq, value);
              break;
            
            case WScanResult.TYPE_BLE:
              message = String.format(Locale.ENGLISH, "<entry type=\"BLE\" bssid=\"%s\" value=\"%s\"/>",
                                      bssid, value);
              break;
            
            case WScanResult.TYPE_BEACON:
              message = String.format(Locale.ENGLISH, "<entry type=\"BEACON\" bssid=\"%s\" power=\"%d\" battery=\"%d\" value=\"%s\"/>",
                                      bssid, power, battery, value);
              break;
          }
          
          mSelectedObject.entries.add(message);
        }
        
        float magnetIntensity = 0;
        int magnetCounter = 0;
        for(int i = 0; i < mSensorResults.size(); ++i)
        {
          SensorResult result = mSensorResults.get(i);
          if (result.type == SensorResult.TYPE_MAGNETOMETER)
          {
            magnetCounter += 1;
            magnetIntensity += (float)Math.sqrt(result.values[0] * result.values[0] +
                                                result.values[1] * result.values[1] +
                                                result.values[2] * result.values[2]);
          }
        }
        magnetIntensity /= Math.max(magnetCounter, 1);
        message = String.format(Locale.ENGLISH, "<entry type=\"MAGNET\" value=\"%.4f\"/>", magnetIntensity);
        mSelectedObject.entries.add(message);
        mSelectedObject.quality = quality;
        mSelectedObject.duration = duration;
        Parser.saveMeasureXml(mLocation);
        break;
      }
      
      case STATE_BEACON_RUN:
      {
        String beaconId = getCloseBeacon();
        if (beaconId == null)
        {
          Log.d(TAG, "Unable to detect close beacon! Cancel beacon!");
          cancelObject();
          return;
        }
        
        Pattern beaconPattern = Pattern.compile("\\(([0-9]+),([0-9]+),([0-9A-F\\-]+)\\),(\\-[0-9]+)");
        Matcher m = beaconPattern.matcher(beaconId);
        if (m.find())
        {
          try
          {
            String uuid = new String(m.group(3));
            int major   = Integer.parseInt(m.group(1));
            int minor   = Integer.parseInt(m.group(2));
            int power   = Integer.parseInt(m.group(4));
            
            MeasureObject beacon = findBeacon(uuid, major, minor);
            if (beacon == null)
            {
              mSelectedObject.uuid = uuid;
              mSelectedObject.beaconMajor = major;
              mSelectedObject.beaconMinor = minor;
              mSelectedObject.beaconPower = power;
            }
            else
            {
              String text = String.format(Locale.ENGLISH, "Beacon already exists: '%s'! Please, try another one!", beacon.name);
              Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
              cancelObject();
              setBeacon();
              return;
            }
          }
          catch (Throwable e)
          {
            Log.d(TAG, "Unable to detect close beacon! Cancel beacon!");
            cancelObject();
            return;
          }
        }
        Log.d(TAG, String.format(Locale.ENGLISH, "Object %s: assigned beacon %s", mSelectedObject.name, beaconId));
        Parser.saveBeaconsXml(mLocation);
        break;
      }
        
      default:
        return;
    }
    
    mScanMap.clear();
    mSensorResults.clear();
    mSelectedObject = null;
    mState = STATE_NONE;
  }
  
  private void updateMeasuringState()
  {
    if (NavigineApp.Navigation == null)
      return;
    
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    int quality  = 0, nsecs = 0;
    long timeNow = DateTimeUtils.currentTimeMillis();
    float[] magnetVector = null;
    
    switch (mState)
    {
      case STATE_NONE:
        mMeasuringLayout.setVisibility(View.GONE);
        mPrevFloorButton.setVisibility(mCurrentSubLocationIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        mNextFloorButton.setVisibility(mCurrentSubLocationIndex == mLocation.subLocations.size() - 1 ? View.INVISIBLE : View.VISIBLE);
        break;
      
      case STATE_POINT_READY:
        mPrevFloorButton.setVisibility(View.INVISIBLE);
        mNextFloorButton.setVisibility(View.INVISIBLE);
        if (mSelectedObject != null)
        {
          mMeasuringLayout.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setVisibility(View.VISIBLE);
          mMeasuringStartButton.setVisibility(View.VISIBLE);
          mMeasuringStopButton.setVisibility(View.GONE);
          mMeasuringPrompt.setText(String.format(Locale.ENGLISH, "Point '%s': start",
                                                 mSelectedObject.name));
          mSensorResults.clear();
          mScanMap.clear();
        }
        break;
      
      case STATE_POINT_RUN:
        mPrevFloorButton.setVisibility(View.INVISIBLE);
        mNextFloorButton.setVisibility(View.INVISIBLE);
        if (mSelectedObject != null)
        {
          List<WScanResult> scanResults = NavigineApp.Navigation.getScanResults(mScanTime);
          List<SensorResult> sensorResults = NavigineApp.Navigation.getSensorResults(mScanTime);
          mScanTime = timeNow + 1;
          
          for(int i = 0; i < scanResults.size(); ++i)
          {
            WScanResult result = scanResults.get(i);
            if (result.time <= timeNow)
              addScanResult(result);
          }
          
          for(int i = 0; i < sensorResults.size(); ++i)
          {
            SensorResult result = sensorResults.get(i);
            if (result.time <= timeNow)
              addSensorResult(result);
          }
          
          mSelectedObject.quality = getMeasuringQuality();
          nsecs = (int)((timeNow - mMeasuringTime) / 1000);
          mMeasuringLayout.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setVisibility(View.VISIBLE);
          mMeasuringStartButton.setVisibility(View.GONE);
          mMeasuringStopButton.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setText(String.format(Locale.ENGLISH, "Point '%s': %d sec\n(W:%.1f, B:%.1f, quality %d%%)",
                                                 mSelectedObject.name, nsecs,
                                                 (float)getEntryCount(WScanResult.TYPE_WIFI, timeNow - 5000) / 5,
                                                 (float)getEntryCount(WScanResult.TYPE_BEACON, timeNow - 5000) / 5,
                                                 mSelectedObject.quality));
          if (mSelectedObject.quality >= 100)
            stopMeasuring();
        }
        break;
      
      case STATE_BEACON_READY:
        mPrevFloorButton.setVisibility(View.INVISIBLE);
        mNextFloorButton.setVisibility(View.INVISIBLE);
        if (mSelectedObject != null)
        {
          mMeasuringLayout.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setVisibility(View.VISIBLE);
          mMeasuringStartButton.setVisibility(View.VISIBLE);
          mMeasuringStopButton.setVisibility(View.GONE);
          mMeasuringPrompt.setText(String.format(Locale.ENGLISH, "Beacon '%s': start",
                                                 mSelectedObject.name));
          mSensorResults.clear();
          mScanMap.clear();
        }
        break;
      
      case STATE_BEACON_RUN:
        mPrevFloorButton.setVisibility(View.INVISIBLE);
        mNextFloorButton.setVisibility(View.INVISIBLE);
        if (mSelectedObject != null)
        {
          List<WScanResult> scanResults = NavigineApp.Navigation.getScanResults(mScanTime);
          mScanTime = timeNow + 1;
          
          for(int i = 0; i < scanResults.size(); ++i)
          {
            WScanResult result = scanResults.get(i);
            if (result.time <= timeNow)
              addScanResult(result);
          }
          
          nsecs = (int)((timeNow - mMeasuringTime) / 1000);
          mMeasuringLayout.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setVisibility(View.VISIBLE);
          mMeasuringStartButton.setVisibility(View.GONE);
          mMeasuringStopButton.setVisibility(View.VISIBLE);
          mMeasuringPrompt.setText(String.format(Locale.ENGLISH, "Beacon '%s': %d sec",
                                                 mSelectedObject.name, nsecs));
          
          if (getCloseBeacon() != null)
            stopMeasuring();
        }
        break;
      
    }
  }
  
  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (NavigineApp.Navigation == null)
          return;
        
        if (mMatrix == null)
        {
          tryLoadMap();
          return;
        }
        
        long timeNow = DateTimeUtils.currentTimeMillis();
        
        // Drawing scale, if necessary
        if (mDrawScale)
        {
          drawScale();
          mDrawScale = false;
        }
        
        mTargetView.setVisibility(View.VISIBLE);
        mAddPointButton.setVisibility(View.VISIBLE);
        mAddBeaconButton.setVisibility(View.VISIBLE);
        
        // Handling long touch gesture
        if (mTouchTimeout > 0 && timeNow >= mTouchTimeout)
        {
          Log.d(TAG, String.format(Locale.ENGLISH, "Long click at (%.2f, %.2f)",
                mTouchPoints[0].x, mTouchPoints[0].y));
          mTouchTimeout = 0;
          doLongTouch(mTouchPoints[0].x, mTouchPoints[0].y);
        }
        
        Picture pic = mPicDrawable.getPicture();
        Canvas canvas = pic.beginRecording(mMapWidth, mMapHeight);
        drawGrid(canvas);
        drawMeasureObjects(canvas);    // Drawing measuring objects
        pic.endRecording();
        
        mImageView.invalidate();
        mImageView.setImageMatrix(mMatrix);
        
        // Updating measuring state
        updateMeasuringState();
      }
    };
}
