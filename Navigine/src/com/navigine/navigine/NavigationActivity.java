package com.navigine.navigine;
import com.navigine.navigine.*;
import com.navigine.naviginesdk.*;
import com.navigine.imu.*;

import android.app.*;
import android.content.*;
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
import java.nio.*;
import java.net.*;
import java.util.*;

public class NavigationActivity extends Activity
{
  // Constants
  private static final String TAG = "NAVIGINE.NavigationActivity";
  private static final int UPDATE_TIMEOUT = 100;
  
  // This context
  private Context mContext = this;
  
  // GUI parameters
  private ImageView  mImageView    = null;
  private ImageView  mScaleView    = null;
  private ImageView  mIconsView    = null;
  private Button     mPrevFloorButton = null;
  private Button     mNextFloorButton = null;
  private TextView   mCurrentFloorTextView = null;
  private TextView   mNavigationInfoTextView = null;
  private TimerTask  mTimerTask    = null;
  private Timer      mTimer        = new Timer();
  private Handler    mHandler      = new Handler();
  private boolean    mAdjustMode   = true;
  private boolean    mDrawScale    = true;
  
  private boolean    mMapLoaded    = false;
  
  // Image parameters
  int mMapWidth    = 0;
  int mMapHeight   = 0;
  int mViewWidth   = 0;
  int mViewHeight  = 0;
  RectF mMapRect   = null;
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
                                                 new PointF(0.0f, 0.0f)};
  
  // Geometry parameters
  private Matrix  mMatrix        = null;
  private float   mRatio         = 1.0f;
  private float   mAdjustAngle   = 0.0f;
  private long    mAdjustTime    = 0;
  private long    mAdjustTimeout = 7000;
  
  // Config parameters
  private float   mMaxX = 0.0f;
  private float   mMaxY = 0.0f;
  private float   mMinRatio = 0.1f;
  private float   mMaxRatio = 10.0f;
  
  // Device parameters
  private DeviceInfo mDeviceInfo = null;          // Current device
  private LocationPoint mTargetPoint = null;  // Current device target
  
  // Location parameters
  private Location mLocation = null;
  private int mCurrentSubLocationIndex = -1;
  
  private int mBackgroundNavigationMode = NavigationThread.MODE_NORMAL;
  private boolean mImuMode = false;
  
  /** Called when the activity is first created */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "NavigationActivity created");
    Log.d(TAG, String.format(Locale.ENGLISH, "Android API LEVEL: %d",
          android.os.Build.VERSION.SDK_INT));
    
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.navigation);
    
    // Setting up GUI parameters
    mImageView = (ImageView)findViewById(R.id.map_image_view);
    mScaleView = (ImageView)findViewById(R.id.scale_image_view);
    mIconsView = (ImageView)findViewById(R.id.icons_image_view);
    mPrevFloorButton = (Button)findViewById(R.id.navigation_prev_floor_button);
    mNextFloorButton = (Button)findViewById(R.id.navigation_next_floor_button);
    mCurrentFloorTextView = (TextView)findViewById(R.id.navigation_current_floor_text_view);
    mNavigationInfoTextView = (TextView)findViewById(R.id.navigation_info_text_view);
    mPrevFloorButton.setVisibility(View.INVISIBLE);
    mNextFloorButton.setVisibility(View.INVISIBLE);
    
    mImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    mImageView.setBackgroundColor(Color.argb(255, 235, 235, 235));
    mScaleView.setImageBitmap(Bitmap.createBitmap(100, 30, Bitmap.Config.ARGB_8888));
    mIconsView.setImageBitmap(Bitmap.createBitmap(100, 30, Bitmap.Config.ARGB_8888));
    
    if (NavigineApp.Settings != null)
    {
      mBackgroundNavigationMode = NavigineApp.Settings.getInt("background_navigation_mode", NavigationThread.MODE_NORMAL);
    }
    
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
    
    mPrevFloorButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (loadPrevSubLocation())
            mAdjustTime = DateTimeUtils.currentTimeMillis() + mAdjustTimeout;
        }
      });
    
    mNextFloorButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (loadNextSubLocation())
            mAdjustTime = DateTimeUtils.currentTimeMillis() + mAdjustTimeout;
        }
      });
  }
  
  @Override public void onDestroy()
  {
    super.onDestroy();
    NavigineApp.stopNavigation();
  }
  
  @Override public void onStart()
  {
    super.onStart();
    
    // Starting interface updates
    mTimerTask = 
      new TimerTask()
      {
        @Override public void run() 
        {
          mHandler.post(mRunnable);
        }
      };
    mTimer.schedule(mTimerTask, UPDATE_TIMEOUT, UPDATE_TIMEOUT);
  }
  
  @Override public void onStop()
  {
    super.onStop();
    mTimerTask.cancel();
    mTimerTask = null;
  }
  
  @Override public void onResume()
  {
    super.onResume();
    if (!mImuMode)
      NavigineApp.setForegroundMode();
  }
  
  @Override public void onPause()
  {
    super.onPause();    
    if (!mImuMode)
      NavigineApp.setBackgroundMode();
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
    
    mHandler.post(mRunnable);
    NavigineApp.startNavigation();
    return true;
  }
  
  private boolean loadSubLocation(int index)
  {
    if (mLocation == null || index < 0 || index >= mLocation.subLocations.size())
      return false;
    
    SubLocation subLoc = mLocation.subLocations.get(index);
    Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation %s", subLoc.name));
    
    double[] gpsCoords = subLoc.getGpsCoordinates(0, 0);
    Log.d(TAG, String.format(Locale.ENGLISH, "GPS: (%.8f, %.8f)",
          gpsCoords[0], gpsCoords[1]));
    
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
    mAdjustAngle = 0.0f;
    mAdjustTime  = 0;
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
    //Log.d(TAG, String.format(Locale.ENGLISH, "Map rect: (%.2f, %.2f) - (%.2f, %.2f)",
    //      mMapRect.left, mMapRect.top, mMapRect.right, mMapRect.bottom));
  }
  
  private void doRotate(float angle, float x, float y)
  {
    if (mMatrix == null)
      return;
    //Log.d(TAG, String.format(Locale.ENGLISH, "Rotate: angle=%.2f, center=(%.2f, %.2f)", angle, x, y));
    float angleInDegrees = angle * 180.0f / (float)Math.PI;
    mMatrix.postRotate(angleInDegrees, x, y);
    mMatrix.mapRect(mMapRect, new RectF(0, 0, mMapWidth, mMapHeight));
    //Log.d(TAG, String.format(Locale.ENGLISH, "Map rect: (%.2f, %.2f) - (%.2f, %.2f)",
    //      mMapRect.left, mMapRect.top, mMapRect.right, mMapRect.bottom));
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
    
    // Handling move events
    switch (pointerCount)
    {
      case 1:
        if (mTouchMode == TOUCH_MODE_SCROLL)
        {
          float deltaX = points[0].x - mTouchPoints[0].x;
          float deltaY = points[0].y - mTouchPoints[0].y;
          mTouchLength += Math.abs(deltaX);
          mTouchLength += Math.abs(deltaY);
          if (mTouchLength > TOUCH_SENSITIVITY)
            mTouchTimeout = 0;
          
          doScroll(deltaX, deltaY);
          mAdjustTime = timeNow + mAdjustTimeout;
          mImageView.setImageMatrix(mMatrix);
          //mHandler.post(mRunnable);
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
          //ratio = (ratio + 1) / 2;
          doZoom(ratio);
          mImageView.setImageMatrix(mMatrix);
          //mHandler.post(mRunnable);
          drawScale();
        }
        mTouchMode = TOUCH_MODE_ZOOM;
        mTouchPoints[0].set(points[0]);
        mTouchPoints[1].set(points[1]);
        break;
    }
  }
  
  private void doLongTouch(float x, float y)
  {
    // Exec target popup dialog
    showTargetPopupDialog(getAbsCoordinates(x, y));
  }
  
  private TreeMap<String, Integer> mColorMap = new TreeMap<String, Integer>();
  private int getClientColor(String id)
  {
    if (mColorMap.containsKey(id))
      return mColorMap.get(id).intValue();
    
    Integer color = null;
    switch (mColorMap.size() % 6)
    {
      case 0: color = Color.argb(255, 255, 0, 0); break;
      case 1: color = Color.argb(255, 0, 255, 0); break;
      case 2: color = Color.argb(255, 0, 0, 255); break;
      case 3: color = Color.argb(255, 255, 255, 0); break;
      case 4: color = Color.argb(255, 255, 0, 255); break;
      case 5: color = Color.argb(255, 0, 255, 255); break;
    }
    mColorMap.put(id, color);
    return color.intValue();
  }
  
  private void drawDevice(DeviceInfo info, Canvas canvas)
  {
    if (info == null)
      return;
    
    // Ignoring Raspberry devices
    if (info.type.equals("raspberry"))
      return;
    
    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    // Check if device belongs to the location loaded
    if (info.location != mLocation.id)
      return;
    
    // Get current sublocation displayed
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    
    // Drawing device path (if it exists)
    if (info.path != null && info.path.length > 1)
    {
      Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      pathPaint.setStrokeWidth(3.0f);
      pathPaint.setStyle(Paint.Style.STROKE);
      
      pathPaint.setARGB(255, 0, 0, 255);
      
      for(int j = 1; j < info.path.length; ++j)
      {
        LocationPoint P = info.path[j-1];
        LocationPoint Q = info.path[j];
        
        if (P.subLocation == subLoc.id && Q.subLocation == subLoc.id)
          drawArrow(getSvgCoordinates(P.x, P.y),
                    getSvgCoordinates(Q.x, Q.y),
                    pathPaint, canvas);
      }
    }
    
    // Check if device belongs to the current sublocation
    if (info.subLocation != subLoc.id)
      return;
    
    float x = info.x;
    float y = info.y;
    float r = info.r;
    float angle = info.azimuth;
    float sinA = (float)Math.sin(angle);
    float cosA = (float)Math.cos(angle);
    float radius = getSvgLength(r);
    
    PointF P = getSvgCoordinates(x, y);
    PointF Q = getSvgCoordinates(x + r * sinA, y + r * cosA);
    PointF R = getSvgCoordinates(x + r * cosA * 0.66f - r * sinA * 0.25f, y - r * sinA * 0.66f - r * cosA * 0.25f);
    PointF S = getSvgCoordinates(x - r * cosA * 0.66f - r * sinA * 0.25f, y + r * sinA * 0.66f - r * cosA * 0.25f);
    
    // Preparing paints
    Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint fillPaintSolid = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint fillPaintTransparent = new Paint(Paint.ANTI_ALIAS_FLAG);
    strokePaint.setStrokeWidth(0.0f);
    strokePaint.setStyle(Paint.Style.STROKE);
    fillPaintSolid.setStyle(Paint.Style.FILL);
    fillPaintTransparent.setStyle(Paint.Style.FILL);
    
    int solidColor = getClientColor(info.id);
    int textColor  = Color.argb(255, Color.red(solidColor) / 3, Color.green(solidColor) / 3, Color.blue(solidColor) / 3);
    int fillColor  = Color.argb(100, Color.red(solidColor), Color.green(solidColor), Color.blue(solidColor));
    strokePaint.setColor(textColor);
    fillPaintSolid.setColor(solidColor);
    fillPaintTransparent.setColor(fillColor);
    
    // Drawing circle
    canvas.drawCircle(P.x, P.y, radius, fillPaintTransparent);
    
    // Drawing orientation
    Path path = new Path();
    path.moveTo(Q.x, Q.y);
    path.lineTo(R.x, R.y);
    path.lineTo(P.x, P.y);
    path.lineTo(S.x, S.y);
    path.lineTo(Q.x, Q.y);
    canvas.drawPath(path, fillPaintSolid);
    canvas.drawPath(path, strokePaint);
  }
  
  private void adjustDevice(DeviceInfo info)
  {
    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    // Check if device belongs to the location loaded
    if (info.location != mLocation.id)
      return;
    
    long timeNow = DateTimeUtils.currentTimeMillis();
    
    // Adjust map, if necessary
    if (timeNow >= mAdjustTime)
    {
      // Firstly, set the correct sublocation
      SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
      if (info.subLocation != subLoc.id)
      {
        for(int i = 0; i < mLocation.subLocations.size(); ++i)
          if (mLocation.subLocations.get(i).id == info.subLocation)
            loadSubLocation(i);
      }
      
      // Secondly, adjust device to the center of the screen
      PointF center = getScreenCoordinates(info.x, info.y);
      float deltaX  = mViewWidth  / 2 - center.x;
      float deltaY  = mViewHeight / 2 - center.y;
      doScroll(deltaX, deltaY);
      
      // Thirdly, adjust device direction to the top of screen
      //float angle = info.azimuth;
      //float deltaA = mAdjustAngle - angle;
      //doRotate(deltaA, center.x, center.y);
      //mAdjustAngle -= deltaA;
      
      //Log.d(TAG, String.format(Locale.ENGLISH, "Adjusted by: (%.2f, %.2f), %.2f (%.2f)",
      //      deltaX, deltaY, deltaA, angle));
      mAdjustTime = timeNow;
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
      text = String.format(Locale.ENGLISH, "%.1f m", Math.round(length * 5) / 5.0f, getString(R.string.meters));
    else if (length >= 2.0f && length < 5.0f)
      text = String.format(Locale.ENGLISH, "%.1f m", Math.round(length * 2) / 2.0f, getString(R.string.meters));
    else if (length >= 5.0f && length < 10.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length), getString(R.string.meters));
    else if (length >= 10.0f && length < 20.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 2) * 2, getString(R.string.meters));
    else if (length >= 20.0f && length < 50.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 5) * 5, getString(R.string.meters));
    else if (length >= 50.0f && length < 100.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 10) * 10, getString(R.string.meters));
    else if (length >= 100.0f && length < 200.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 20) * 20, getString(R.string.meters));
    else if (length >= 200.0f && length < 500.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 50) * 50, getString(R.string.meters));
    else if (length >= 500.0f && length < 1000.0f)
      text = String.format(Locale.ENGLISH, "%d m", Math.round(length / 100) * 100, getString(R.string.meters));
    else if (length >= 1000.0f && length < 2000.0f)
      text = String.format(Locale.ENGLISH, "%.1f km", Math.round(length / 200) / 5.0f, getString(R.string.kilometers));
    else if (length >= 2000.0f && length < 5000.0f)
      text = String.format(Locale.ENGLISH, "%.1f km", Math.round(length / 500) / 2.0f, getString(R.string.kilometers));
    else
      text = String.format(Locale.ENGLISH, "%d km", Math.round(length / 1000), getString(R.string.kilometers));
    
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
  
  private void drawArrow(PointF A, PointF B, Paint paint, Canvas canvas)
  {
    float ux = B.x - A.x;
    float uy = B.y - A.y;
    float n = (float)Math.sqrt(ux * ux + uy * uy);
    float m = Math.min(15.0f, n / 3);
    float k = m / n;
    
    PointF C = new PointF(k * A.x + (1 - k) * B.x, k * A.y + (1 - k) * B.y);
    
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
  
  private PointF _targetPoint = null;
  private AlertDialog _alertDialog = null;
  private void showTargetPopupDialog(PointF P)
  {
    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;
    
    _targetPoint = P;
    
    LayoutInflater inflater = getLayoutInflater();
    View view = inflater.inflate(R.layout.target_point_dialog, null);
    String title = String.format(Locale.ENGLISH, "Target point (%.1f, %.1f)", P.x, P.y);
    TextView textView = (TextView)view.findViewById(R.id.target_point_dialog_description);
    textView.setText("Select your action:");
    
    Button connectImuButton    = (Button)view.findViewById(R.id.target_point_dialog__connect_imu_button);
    Button disconnectImuButton = (Button)view.findViewById(R.id.target_point_dialog__disconnect_imu_button);
    Button makeRouteButton     = (Button)view.findViewById(R.id.target_point_dialog__make_route_button);
    Button cancelRouteButton   = (Button)view.findViewById(R.id.target_point_dialog__cancel_route_button);
    
    connectImuButton.setVisibility(View.GONE);
    disconnectImuButton.setVisibility(View.GONE);
    makeRouteButton.setVisibility(View.GONE);
    cancelRouteButton.setVisibility(View.GONE);
    
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
    alertBuilder.setView(view);
    alertBuilder.setTitle(title);
    
    makeRouteButton.setVisibility(View.VISIBLE);
    makeRouteButton.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(View v)
        {
          if (_alertDialog != null)
          {
            // Get the current sub-location
            SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
            if (NavigineApp.Navigation != null && subLoc != null)
            {
              mTargetPoint = new LocationPoint(subLoc.id, _targetPoint.x, _targetPoint.y);
              NavigineApp.Navigation.setTarget(mTargetPoint);
            }
            _alertDialog.cancel();
          }
        }
      });
    
    if (mDeviceInfo != null && mDeviceInfo.path != null && mDeviceInfo.path.length > 1)
    {
      cancelRouteButton.setVisibility(View.VISIBLE);
      cancelRouteButton.setOnClickListener(
        new OnClickListener()
        {
          @Override public void onClick(View v)
          {
            if (_alertDialog != null)
            {
              mDeviceInfo.path = null;
              NavigineApp.Navigation.cancelTarget();
              _alertDialog.cancel();
            }
          }
        });
    }
    
    if (NavigineApp.IMU.getConnectionState() == IMU_Thread.STATE_IDLE)
    {
      connectImuButton.setVisibility(View.VISIBLE);
      connectImuButton.setOnClickListener(
        new OnClickListener()
        {
          @Override public void onClick(View v)
          {
            if (_alertDialog != null)
            {
              SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
              if (subLoc != null)
                connectToIMU(subLoc.id, _targetPoint.x, _targetPoint.y);
              _alertDialog.cancel();
            }
          }
        });
    }
    else
    {
      disconnectImuButton.setVisibility(View.VISIBLE);
      disconnectImuButton.setOnClickListener(
        new OnClickListener()
        {
          @Override public void onClick(View v)
          {
            if (_alertDialog != null)
            {
              disconnectFromIMU();
              _alertDialog.cancel();
            }
          }
        });
    }
    
    _alertDialog = alertBuilder.create();
    _alertDialog.setCanceledOnTouchOutside(false);
    _alertDialog.show();
  }
  
  private void connectToIMU(int subLocId, float x0, float y0)
  {
    if (mLocation == null)
      return;
    
    SubLocation subLoc = mLocation.getSubLocation(subLocId);
    if (subLoc == null)
      return;
    
    if (NavigineApp.IMU.getConnectionState() != IMU_Thread.STATE_IDLE)
    {
      Log.e(TAG, "Can't connect to IMU: not in IDLE state!");
      return;
    }
    
    NavigineApp.stopNavigation();
    
    if (NavigineApp.Navigation != null)
    {
      String logFile = null;
      String arhivePath = NavigineApp.Navigation.getArchivePath();
      if (arhivePath != null && arhivePath.length() > 0)
      {
        for(int i = 1; i < 10; ++i)
        {
          String suffix = String.format(Locale.ENGLISH, ".IMU.%d.log", i);
          String filename = arhivePath.replaceAll("\\.zip$", suffix);
          if (!(new File(filename)).exists())
          {
            logFile = filename;
            break;
          }
        }
      }
      NavigineApp.IMU.setLogFile(logFile);
    }
    
    NavigineApp.IMU_Location = mLocation.id;
    NavigineApp.IMU_SubLocation = subLocId;
    NavigineApp.IMU.setStartPoint(x0, y0, 0.0f);
    NavigineApp.IMU.connect();
    mImuMode = true;
  }
  
  private void disconnectFromIMU()
  {
    if (NavigineApp.IMU.getConnectionState() != IMU_Thread.STATE_NORMAL)
    {
      Log.e(TAG, "Can't disconnect from IMU: not in NORMAL state!");
      return;
    }
    NavigineApp.IMU.disconnect();
    mImuMode = false;
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
        
        mDeviceInfo = null;
        String infoText = null;
        
        if (mImuMode)
        {
          String error = NavigineApp.IMU.getConnectionError();
          switch (NavigineApp.IMU.getConnectionState())
          {
            case IMU_Thread.STATE_IDLE:
              infoText = error;
              break;
            
            case IMU_Thread.STATE_CONNECT:
              infoText = new String("IMU: connecting...");
              break;
            
            case IMU_Thread.STATE_DISCONNECT:
              infoText = new String("IMU: disconnecting...");
              break;
            
            case IMU_Thread.STATE_NORMAL:
            {
              IMU_Device imuDevice = NavigineApp.IMU.getDevice();
              if (imuDevice != null)
              {
                mDeviceInfo = NavigineApp.getDeviceInfoByIMU(imuDevice);
                infoText = String.format(Locale.ENGLISH, "IMU: packet #%d", imuDevice.packetNumber);
              }
              break;
            }
          }
        }
        else
        {
          // Start navigation if necessary
          if (NavigineApp.Navigation.getMode() == NavigationThread.MODE_IDLE)
            NavigineApp.startNavigation();
          
          // Get device info from NavigationThread
          mDeviceInfo = NavigineApp.Navigation.getDeviceInfo();
          infoText = (mDeviceInfo == null) ?
                      String.format(Locale.ENGLISH, " Error code: %d ",  NavigineApp.Navigation.getErrorCode()) :
                      String.format(Locale.ENGLISH, " Step %d [%.2fm] ", mDeviceInfo.stepCount, mDeviceInfo.stepLength);
        }
        
        mNavigationInfoTextView.setText(infoText != null ? " " + infoText + " " : "");
        
        if (mDeviceInfo != null)
          drawDevice(mDeviceInfo, canvas);
        
        if (mAdjustMode && mDeviceInfo != null)
          adjustDevice(mDeviceInfo);
        
        //else if (Math.abs(mAdjustAngle) > EPS)
        //{
        //  // Restore normal map orientation
        //  doRotate(mAdjustAngle, mViewWidth / 2, mViewHeight / 2);
        //  mAdjustAngle = 0.0f;
        //}
        
        pic.endRecording();
        
        mImageView.invalidate();
        mImageView.setImageMatrix(mMatrix);
      }
    };
}
