package com.navigine.naviginedemo;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.ImageView.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

import com.navigine.naviginesdk.*;

public class MainActivity extends Activity
{
  private static final String   TAG                     = "Navigine.Demo";
  private static final int      UPDATE_TIMEOUT          = 100;  // milliseconds
  private static final int      ADJUST_TIMEOUT          = 5000; // milliseconds
  private static final int      ERROR_MESSAGE_TIMEOUT   = 5000; // milliseconds
  private static final boolean  ORIENTATION_ENABLED     = true; // Show device orientation?

  // UI Parameters
  private ImageView   mMapImageView             = null;
  private ImageView   mPicImageView             = null;
  private Button      mPrevFloorButton          = null;
  private Button      mNextFloorButton          = null;
  private View        mPrevFloorView            = null;
  private View        mNextFloorView            = null;
  private View        mZoomInView               = null;
  private View        mZoomOutView              = null;
  private View        mAdjustModeView           = null;
  private TextView    mCurrentFloorLabel        = null;
  private TextView    mErrorMessageLabel        = null;
  private Button      mMakeRouteButton          = null;
  private Button      mCancelRouteButton        = null;
  private TimerTask   mTimerTask                = null;
  private Timer       mTimer                    = new Timer();
  private Handler     mHandler                  = new Handler();

  private boolean     mAdjustMode               = false;
  private long        mErrorMessageTime         = 0;

  // Map parameters
  private int         mMapWidth                 = 0;
  private int         mMapHeight                = 0;
  private int         mViewWidth                = 0;
  private int         mViewHeight               = 0;
  private long        mAdjustTime               = 0;
  private float       mMaxX                     = 0.0f;
  private float       mMaxY                     = 0.0f;
  private float       mRatio                    = 1.0f;
  private float       mMinRatio                 = 0.1f;
  private float       mMaxRatio                 = 10.0f;
  private RectF       mMapRect                  = null;
  private Matrix      mMatrix                   = null;
  private Drawable    mMapDrawable              = null;
  private PictureDrawable mPicDrawable          = null;

  // Location parameters
  private Location    mLocation                 = null;
  private int         mCurrentSubLocationIndex  = -1;

  // Device parameters
  private int           mErrorCode              = 0;    // Current error code
  private DeviceInfo    mDeviceInfo             = null; // Current device
  private LocationPoint mPinPoint               = null; // Potential device target
  private LocationPoint mTargetPoint            = null; // Current device target

  // Touch parameters
  private static final int TOUCH_MODE_SCROLL    = 1;
  private static final int TOUCH_MODE_ZOOM      = 2;
  private static final int TOUCH_MODE_ROTATE    = 3;
  private static final int TOUCH_SENSITIVITY    = 20;
  private static final int TOUCH_SHORT_TIMEOUT  = 200;
  private static final int TOUCH_LONG_TIMEOUT   = 600;
  private long mTouchTime   = 0;
  private int  mTouchMode   = 0;
  private int  mTouchLength = 0;
  private PointF[] mTouchPoints = new PointF[] { new PointF(0.0f, 0.0f),
    new PointF(0.0f, 0.0f),
    new PointF(0.0f, 0.0f) };

  @Override protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_main);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
      WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

    // Setting up GUI parameters
    mMapImageView = (ImageView)findViewById(R.id.navigation__map_image);
    mPicImageView = (ImageView)findViewById(R.id.navigation__ext_image);
    mPrevFloorButton = (Button)findViewById(R.id.navigation__prev_floor_button);
    mNextFloorButton = (Button)findViewById(R.id.navigation__next_floor_button);
    mPrevFloorView = (View)findViewById(R.id.navigation__prev_floor_view);
    mNextFloorView = (View)findViewById(R.id.navigation__next_floor_view);
    mCurrentFloorLabel = (TextView)findViewById(R.id.navigation__current_floor_label);
    mZoomInView  = (View)findViewById(R.id.navigation__zoom_in_view);
    mZoomOutView = (View)findViewById(R.id.navigation__zoom_out_view);
    mAdjustModeView = (View)findViewById(R.id.navigation__adjust_mode_view);
    mMakeRouteButton = (Button)findViewById(R.id.navigation__make_route_button);
    mCancelRouteButton = (Button)findViewById(R.id.navigation__cancel_route_button);
    mErrorMessageLabel = (TextView)findViewById(R.id.navigation__error_message_label);

    mMapImageView.setBackgroundColor(Color.argb(255, 235, 235, 235));
    mPicImageView.setBackgroundColor(Color.argb(0, 0, 0, 0));
    mPicImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    mMakeRouteButton.setVisibility(View.GONE);
    mCancelRouteButton.setVisibility(View.GONE);
    mErrorMessageLabel.setVisibility(View.GONE);

    mPrevFloorView.setVisibility(View.INVISIBLE);
    mNextFloorView.setVisibility(View.INVISIBLE);
    mCurrentFloorLabel.setVisibility(View.INVISIBLE);
    mZoomInView.setVisibility(View.INVISIBLE);
    mZoomOutView.setVisibility(View.INVISIBLE);
    mAdjustModeView.setVisibility(View.INVISIBLE);

    // Setting up touch listener
    mMapImageView.setOnTouchListener(
      new OnTouchListener() {
        @Override public boolean onTouch(View view, MotionEvent motionEvent)
        {
          doTouch(motionEvent);
          return true;
        }
      });

    // Starting interface updates
    mTimerTask = new TimerTask()
    {
      @Override public void run()
      {
        mHandler.post(mRunnable);
      }
    };
    mTimer.schedule(mTimerTask, UPDATE_TIMEOUT, UPDATE_TIMEOUT);
  }

  @Override public void onDestroy()
  {
    DemoApp.finish();
    mTimerTask.cancel();
    mTimerTask = null;
    super.onDestroy();
  }

  @Override public void onBackPressed()
  {
    moveTaskToBack(true);
  }

  public void toggleAdjustMode(View v)
  {
    mAdjustMode = !mAdjustMode;
    mAdjustTime = 0;
    Button adjustModeButton = (Button)findViewById(R.id.navigation__adjust_mode_button);
    adjustModeButton.setBackgroundResource(mAdjustMode ?
      R.drawable.btn_adjust_mode_on :
      R.drawable.btn_adjust_mode_off);
    mHandler.post(mRunnable);
  }

  public void onNextFloor(View v)
  {
    if (loadNextSubLocation())
      mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
  }

  public void onPrevFloor(View v)
  {
    if (loadPrevSubLocation())
      mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
  }

  public void onZoomIn(View v)
  {
    doZoom(1.25f);
  }

  public void onZoomOut(View v)
  {
    doZoom(0.8f);
  }

  public void onMakeRoute(View v)
  {
    if (DemoApp.Navigation == null)
      return;

    if (mPinPoint == null)
      return;

    mTargetPoint = mPinPoint;
    mPinPoint = null;

    DemoApp.Navigation.setTarget(mTargetPoint);
    mMakeRouteButton.setVisibility(View.GONE);
    mHandler.post(mRunnable);
  }

  public void onCancelRoute(View v)
  {
    if (DemoApp.Navigation == null)
      return;

    mTargetPoint = null;
    mPinPoint = null;

    DemoApp.Navigation.cancelTargets();
    mMakeRouteButton.setVisibility(View.GONE);
    mCancelRouteButton.setVisibility(View.GONE);
    mHandler.post(mRunnable);
  }

  public void onCloseMessage(View v)
  {
    mErrorMessageLabel.setVisibility(View.GONE);
    mErrorMessageTime = 0;
  }

  private void setErrorMessage(String message)
  {
    mErrorMessageLabel.setText(message);
    mErrorMessageLabel.setVisibility(View.VISIBLE);
    mErrorMessageTime = NavigineSDK.currentTimeMillis();
  }

  private boolean mMapLoaded = false;
  private boolean loadMap(String fileName)
  {
    if (mMapLoaded)
      return false;
    mMapLoaded = true;

    if (DemoApp.Navigation == null)
    {
      Log.e(TAG, "Can't load map! Navigine SDK is not available!");
      return false;
    }

    File f = new File(fileName);
    if (!f.exists())
    {
      Log.e(TAG, "Can't load map! Location archive doesn't exist!");
      return false;
    }

    if (!DemoApp.Navigation.loadArchive(fileName))
    {
      String error = DemoApp.Navigation.getLastError();
      if (error != null)
        Log.e(TAG, error);
      return false;
    }

    mLocation = DemoApp.Navigation.getLocation();
    mCurrentSubLocationIndex = -1;
    mMatrix = null;

    if (mLocation == null)
    {
      Log.e(TAG, "Loading map failed: no location");
      return false;
    }

    if (mLocation.subLocations.size() == 0)
    {
      Log.e(TAG, "Loading map failed: no sublocations");
      mLocation = null;
      return false;
    }

    if (!loadSubLocation(0))
    {
      Log.e(TAG, "Loading map failed: unable to load default sublocation");
      mLocation = null;
      return false;
    }

    if (mLocation.subLocations.size() >= 2)
    {
      mPrevFloorView.setVisibility(View.VISIBLE);
      mNextFloorView.setVisibility(View.VISIBLE);
      mCurrentFloorLabel.setVisibility(View.VISIBLE);
    }
    mZoomInView.setVisibility(View.VISIBLE);
    mZoomOutView.setVisibility(View.VISIBLE);
    mAdjustModeView.setVisibility(View.VISIBLE);

    mHandler.post(mRunnable);
    DemoApp.Navigation.setMode(NavigationThread.MODE_NORMAL);
    return true;
  }

  private boolean loadSubLocation(int index)
  {
    if (DemoApp.Navigation == null)
      return false;

    if (mLocation == null || index < 0 || index >= mLocation.subLocations.size())
      return false;

    SubLocation subLoc = mLocation.subLocations.get(index);
    Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation %s", subLoc.name));

    subLoc.getPicture();
    subLoc.getBitmap();

    if (subLoc.picture == null && subLoc.bitmap == null)
    {
      Log.e(TAG, "Loading sublocation failed: invalid image");
      return false;
    }

    if (subLoc.width == 0.0f || subLoc.height == 0.0f)
    {
      Log.e(TAG, String.format(Locale.ENGLISH, "Loading sublocation failed: invalid size %.2f x %.2f", subLoc.width, subLoc.height));
      return false;
    }

    Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation: %.2f x %.2f\n", subLoc.width, subLoc.height));

    mViewWidth  = mMapImageView.getWidth();
    mViewHeight = mMapImageView.getHeight();
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

    Picture pic = new Picture();
    pic.beginRecording(mViewWidth, mViewHeight);
    pic.endRecording();

    mMapDrawable = subLoc.picture == null ?
      new BitmapDrawable(getResources(), subLoc.bitmap) :
      new PictureDrawable(subLoc.picture);
    mPicDrawable = new PictureDrawable(pic);

    mMapImageView.setImageDrawable(mMapDrawable);
    mMapImageView.setScaleType(ScaleType.MATRIX);
    mPicImageView.setImageDrawable(mPicDrawable);

    // Reinitializing map/matrix parameters
    mMatrix      = new Matrix();
    mMaxX        = subLoc.width;
    mMaxY        = subLoc.height;
    mRatio       = 1.0f;
    mMinRatio    = Math.min((float)mViewWidth / mMapWidth, (float)mViewHeight / mMapHeight);
    mMaxRatio    = Math.min((float)mViewWidth / mMapWidth * subLoc.width / 2, (float)mViewHeight / mMapHeight * subLoc.height / 2);
    mMaxRatio    = Math.max(mMaxRatio, mMinRatio);
    mAdjustTime  = 0;

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
    mCurrentFloorLabel.setText(String.format(Locale.ENGLISH, "%d", mCurrentSubLocationIndex));

    if (mCurrentSubLocationIndex > 0)
    {
      mPrevFloorButton.setEnabled(true);
      mPrevFloorView.setBackgroundColor(Color.parseColor("#90aaaaaa"));
    }
    else
    {
      mPrevFloorButton.setEnabled(false);
      mPrevFloorView.setBackgroundColor(Color.parseColor("#90dddddd"));
    }

    if (mCurrentSubLocationIndex + 1 < mLocation.subLocations.size())
    {
      mNextFloorButton.setEnabled(true);
      mNextFloorView.setBackgroundColor(Color.parseColor("#90aaaaaa"));
    }
    else
    {
      mNextFloorButton.setEnabled(false);
      mNextFloorView.setBackgroundColor(Color.parseColor("#90dddddd"));
    }

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

  private float getScreenLength(float d)
  {
    return getSvgLength(d) * mRatio;
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
    long timeNow = NavigineSDK.currentTimeMillis();
    int actionMask = event.getActionMasked();
    int pointerIndex = event.getActionIndex();
    int pointerCount = event.getPointerCount();

    if (DemoApp.Navigation == null)
      return;

    PointF[] points = new PointF[pointerCount];
    for(int i = 0; i < pointerCount; ++i)
      points[i] = new PointF(event.getX(i), event.getY(i));

    //Log.d(TAG, String.format(Locale.ENGLISH, "MOTION EVENT: %d", actionMask));

    switch (actionMask)
    {
      case MotionEvent.ACTION_DOWN:
      {
        // Gesture started
        mTouchPoints[0].set(points[0]);
        mTouchTime   = timeNow;
        mTouchMode   = 0;
        mTouchLength = 0;
        break;
      }

      case MotionEvent.ACTION_MOVE:
      {
        if (pointerCount == 1)
        {
          if (mTouchMode == TOUCH_MODE_SCROLL)
          {
            float deltaX = points[0].x - mTouchPoints[0].x;
            float deltaY = points[0].y - mTouchPoints[0].y;
            mTouchLength += Math.abs(deltaX);
            mTouchLength += Math.abs(deltaY);
            if (mTouchLength > TOUCH_SENSITIVITY * DemoApp.DisplayDensity)
              mTouchTime = 0;
            doScroll(deltaX, deltaY);
            mAdjustTime = timeNow + ADJUST_TIMEOUT;
            mHandler.post(mRunnable);
          }
          mTouchMode = TOUCH_MODE_SCROLL;
          mTouchPoints[0].set(points[0]);
        }
        else if (pointerCount == 2)
        {
          if (mTouchMode == TOUCH_MODE_ZOOM)
          {
            float oldDist = PointF.length(mTouchPoints[0].x - mTouchPoints[1].x, mTouchPoints[0].y - mTouchPoints[1].y);
            float newDist = PointF.length(points[0].x - points[1].x, points[0].y - points[1].y);
            oldDist = Math.max(oldDist, 1.0f);
            newDist = Math.max(newDist, 1.0f);
            float ratio = newDist / oldDist;
            doZoom(ratio);
            mHandler.post(mRunnable);
          }
          mTouchMode = TOUCH_MODE_ZOOM;
          mTouchPoints[0].set(points[0]);
          mTouchPoints[1].set(points[1]);
        }
        break;
      }

      case MotionEvent.ACTION_UP:
      {
        // Gesture stopped. Check if it was a single tap
        //Log.d(TAG, String.format(Locale.ENGLISH, "ACTION UP: %d %d\n", (int)(timeNow - mTouchTime), mTouchLength));
        if (mTouchTime > 0 &&
          mTouchTime + TOUCH_SHORT_TIMEOUT > timeNow &&
          mTouchLength < TOUCH_SENSITIVITY * DemoApp.DisplayDensity)
        {
          doShortTouch(mTouchPoints[0].x, mTouchPoints[0].y);
        }
        mTouchTime    = 0;
        mTouchMode    = 0;
        mTouchLength  = 0;
        break;
      }

      default:
      {
        mTouchTime    = 0;
        mTouchMode    = 0;
        mTouchLength  = 0;
        break;
      }
    }
  }

  private void doShortTouch(float x, float y)
  {
    Log.d(TAG, String.format(Locale.ENGLISH, "Short click at (%.2f, %.2f)", x, y));

    if (mPinPoint != null)
    {
      cancelPin();
      return;
    }

    if (mTargetPoint == null)
      return;

    if (mCancelRouteButton.getVisibility() == View.VISIBLE)
    {
      mCancelRouteButton.setVisibility(View.GONE);
      return;
    }

    // Check if we touched pin point => highlight cancel button
    PointF P = getScreenCoordinates(mTargetPoint.x, mTargetPoint.y);
    float dist = Math.abs(x - P.x) + Math.abs(y - P.y);
    if (dist < 30.0f * DemoApp.DisplayDensity)
      mCancelRouteButton.setVisibility(View.VISIBLE);
  }

  private void doLongTouch(float x, float y)
  {
    Log.d(TAG, String.format(Locale.ENGLISH, "Long click at (%.2f, %.2f)", x, y));
    makePin(getAbsCoordinates(x, y));
  }

  private void makePin(PointF P)
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;

    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    if (subLoc == null)
      return;

    if (P.x < 0.0f || P.x > mMaxX ||
        P.y < 0.0f || P.y > mMaxY)
    {
      // Missing the map
      return;
    }

    if (mTargetPoint != null)
    {
      setErrorMessage("Unable to make route: you must cancel the previous route first!");
      return;
    }

    if (mDeviceInfo == null)
    {
      setErrorMessage("Unable to make route: navigation is not available!");
      return;
    }

    mPinPoint = new LocationPoint(subLoc.id, P.x, P.y);
    mHandler.post(mRunnable);
  }

  private void cancelPin()
  {
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;

    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
    if (subLoc == null)
      return;

    if (mTargetPoint != null || mPinPoint == null)
      return;

    mPinPoint = null;
    mMakeRouteButton.setVisibility(View.GONE);
    mHandler.post(mRunnable);
  }

  private void drawPoints(Canvas canvas)
  {
    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;

    // Get current sublocation displayed
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);

    if (subLoc == null)
      return;

    final int solidColor = Color.argb(255, 64, 163, 205); // Light-blue color
    final int circleColor = Color.argb(127, 64, 163, 205); // Semi-transparent light-blue color
    final int arrowColor = Color.argb(255, 255, 255, 255); // White color
    final float dp = DemoApp.DisplayDensity;

    // Preparing paints
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);

    // Drawing pin point (if it exists and belongs to the current sublocation)
    if (mPinPoint != null && mPinPoint.subLocation == subLoc.id)
    {
      final PointF T = getScreenCoordinates(mPinPoint.x, mPinPoint.y);
      final float tRadius = 10 * dp;

      paint.setARGB(255, 0, 0, 0);
      paint.setStrokeWidth(4 * dp);
      canvas.drawLine(T.x, T.y, T.x, T.y - 3 * tRadius, paint);

      paint.setColor(solidColor);
      canvas.drawCircle(T.x, T.y - 3 * tRadius, tRadius, paint);

      ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mMakeRouteButton.getLayoutParams();
      layoutParams.leftMargin = (int) (T.x - (float) mMakeRouteButton.getWidth() / 2.0f);
      layoutParams.topMargin = (int) (T.y - (float) mMakeRouteButton.getHeight() - tRadius * 5);
      layoutParams.rightMargin = (int) (layoutParams.leftMargin + (float) mMakeRouteButton.getWidth());
      layoutParams.bottomMargin = (int) (layoutParams.topMargin + (float) mMakeRouteButton.getHeight());
      mMakeRouteButton.setLayoutParams(layoutParams);
      mMakeRouteButton.setVisibility(View.VISIBLE);
    }
    else
      mMakeRouteButton.setVisibility(View.GONE);

    // Drawing target point (if it exists and belongs to the current sublocation)
    if (mTargetPoint != null && mTargetPoint.subLocation == subLoc.id)
    {
      final PointF T = getScreenCoordinates(mTargetPoint.x, mTargetPoint.y);
      final float tRadius = 10 * dp;

      paint.setARGB(255, 0, 0, 0);
      paint.setStrokeWidth(4 * dp);
      canvas.drawLine(T.x, T.y, T.x, T.y - 3 * tRadius, paint);

      paint.setColor(solidColor);
      canvas.drawCircle(T.x, T.y - 3 * tRadius, tRadius, paint);

      ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mCancelRouteButton.getLayoutParams();
      layoutParams.leftMargin = (int) (T.x - (float) mCancelRouteButton.getWidth() / 2.0f);
      layoutParams.topMargin = (int) (T.y - (float) mCancelRouteButton.getHeight() - 100.0f);
      layoutParams.rightMargin = (int) (layoutParams.leftMargin + (float) mCancelRouteButton.getWidth());
      layoutParams.bottomMargin = (int) (layoutParams.topMargin + (float) mCancelRouteButton.getHeight());
      mCancelRouteButton.setLayoutParams(layoutParams);
    }
    else
      mCancelRouteButton.setVisibility(View.GONE);
  }

  private void drawDevice(DeviceInfo info, Canvas canvas)
  {
    if (info == null)
      return;

    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;

    // Check if device belongs to the location loaded
    if (info.location != mLocation.id)
      return;

    // Get current sublocation displayed
    SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);

    if (subLoc == null)
      return;

    final int solidColor  = Color.argb(255, 64,  163, 205); // Light-blue color
    final int circleColor = Color.argb(127, 64,  163, 205); // Semi-transparent light-blue color
    final int arrowColor  = Color.argb(255, 255, 255, 255); // White color
    final float dp = DemoApp.DisplayDensity;

    // Preparing paints
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStyle(Paint.Style.FILL_AND_STROKE);

    /// Drawing device path (if it exists)
    if (info.paths != null && info.paths.size() > 0)
    {
      DevicePath p = info.paths.get(0);
      if (p.path.length >= 2)
      {
        paint.setColor(solidColor);

        for(int j = 1; j < p.path.length; ++j)
        {
          LocationPoint P = p.path[j-1];
          LocationPoint Q = p.path[j];
          if (P.subLocation == subLoc.id && Q.subLocation == subLoc.id)
          {
            paint.setStrokeWidth(3 * dp);
            PointF P1 = getScreenCoordinates(P.x, P.y);
            PointF Q1 = getScreenCoordinates(Q.x, Q.y);
            canvas.drawLine(P1.x, P1.y, Q1.x, Q1.y, paint);
          }
        }
      }
    }

    // Check if device belongs to the current sublocation
    if (info.subLocation != subLoc.id)
      return;

    final float x  = info.x;
    final float y  = info.y;
    final float r  = info.r;
    final float angle = info.azimuth;
    final float sinA = (float)Math.sin(angle);
    final float cosA = (float)Math.cos(angle);
    final float radius  = getScreenLength(r);   // External radius: navigation-determined, transparent
    final float radius1 = 25 * dp;              // Internal radius: fixed, solid

    PointF O = getScreenCoordinates(x, y);
    PointF P = new PointF(O.x - radius1 * sinA * 0.22f, O.y + radius1 * cosA * 0.22f);
    PointF Q = new PointF(O.x + radius1 * sinA * 0.55f, O.y - radius1 * cosA * 0.55f);
    PointF R = new PointF(O.x + radius1 * cosA * 0.44f - radius1 * sinA * 0.55f, O.y + radius1 * sinA * 0.44f + radius1 * cosA * 0.55f);
    PointF S = new PointF(O.x - radius1 * cosA * 0.44f - radius1 * sinA * 0.55f, O.y - radius1 * sinA * 0.44f + radius1 * cosA * 0.55f);

    // Drawing transparent circle
    paint.setStrokeWidth(0);
    paint.setColor(circleColor);
    canvas.drawCircle(O.x, O.y, radius, paint);

    // Drawing solid circle
    paint.setColor(solidColor);
    canvas.drawCircle(O.x, O.y, radius1, paint);

    if (ORIENTATION_ENABLED)
    {
      // Drawing arrow
      paint.setColor(arrowColor);
      Path path = new Path();
      path.moveTo(Q.x, Q.y);
      path.lineTo(R.x, R.y);
      path.lineTo(P.x, P.y);
      path.lineTo(S.x, S.y);
      path.lineTo(Q.x, Q.y);
      canvas.drawPath(path, paint);
    }
  }

  private void adjustDevice(DeviceInfo info)
  {
    // Check if location is loaded
    if (mLocation == null || mCurrentSubLocationIndex < 0)
      return;

    // Check if device belongs to the location loaded
    if (info.location != mLocation.id)
      return;

    long timeNow = NavigineSDK.currentTimeMillis();

    // Adjust map, if necessary
    if (timeNow >= mAdjustTime)
    {
      // Firstly, set the correct sublocation
      SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);
      if (info.subLocation != subLoc.id)
      {
        for(int i = 0; i < mLocation.subLocations.size(); ++i)
          if (mLocation.subLocations.get(i).id == info.subLocation)
            if (mLocation.subLocations.get(i).id == info.subLocation)
              loadSubLocation(i);
      }

      // Secondly, adjust device to the center of the screen
      PointF center = getScreenCoordinates(info.x, info.y);
      float deltaX  = mViewWidth  / 2 - center.x;
      float deltaY  = mViewHeight / 2 - center.y;
      doScroll(deltaX, deltaY);

      mAdjustTime = timeNow;
    }
  }

  final Runnable mRunnable =
    new Runnable()
    {
      public void run()
      {
        if (DemoApp.Navigation == null)
        {
          Log.d(TAG, "Sorry, navigation is not supported on your device!");
          return;
        }

        if (mMatrix == null)
        {
          String archiveFile = NavigineSDK.getLocationFile(DemoApp.LOCATION_NAME);
          loadMap(archiveFile);
          return;
        }

        long timeNow = NavigineSDK.currentTimeMillis();

        // Handling long touch gesture
        if (mTouchTime > 0 &&
            mTouchTime + TOUCH_LONG_TIMEOUT < timeNow &&
            mTouchLength < TOUCH_SENSITIVITY * DemoApp.DisplayDensity)
        {
          doLongTouch(mTouchPoints[0].x, mTouchPoints[0].y);
          mTouchTime = 0;
          mTouchLength = 0;
        }

        if (mErrorMessageTime > 0 && timeNow > mErrorMessageTime + ERROR_MESSAGE_TIMEOUT)
        {
          mErrorMessageTime = 0;
          mErrorMessageLabel.setVisibility(View.GONE);
        }

        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
          return;

        // Get current sublocation displayed
        SubLocation subLoc = mLocation.subLocations.get(mCurrentSubLocationIndex);

        // Start navigation if necessary
        if (DemoApp.Navigation.getMode() == NavigationThread.MODE_IDLE)
          DemoApp.Navigation.setMode(NavigationThread.MODE_NORMAL);

        // Get device info from NavigationThread
        mDeviceInfo = DemoApp.Navigation.getDeviceInfo();
        mErrorCode  = DemoApp.Navigation.getErrorCode();

        if (mDeviceInfo != null)
        {
          mErrorMessageTime = 0;
          mErrorMessageLabel.setVisibility(View.GONE);
          
          if (mAdjustMode)
            adjustDevice(mDeviceInfo);

          // Drawing the device
          Picture pic = mPicDrawable.getPicture();
          Canvas canvas = pic.beginRecording(mViewWidth, mViewHeight);
          drawPoints(canvas);
          drawDevice(mDeviceInfo, canvas);
          pic.endRecording();
        }
        else
        {
          switch (mErrorCode)
          {
            case 4:
              setErrorMessage("You are out of navigation zone! Please, check that your bluetooth is enabled!");
              break;

            case 8:
            case 30:
              setErrorMessage("Not enough beacons on the location! Please, add more beacons!");
              break;

            default:
              setErrorMessage(String.format(Locale.ENGLISH,
                              "Something is wrong with location '%s' (error code %d)! " +
                              "Please, contact technical support!",
                              mLocation.name, mErrorCode));
              break;
          }
          mMakeRouteButton.setVisibility(View.GONE);
          mCancelRouteButton.setVisibility(View.GONE);
        }
        
        mPicImageView.invalidate();
        mMapImageView.setImageMatrix(mMatrix);
        mMapImageView.invalidate();
      }
    };
}
