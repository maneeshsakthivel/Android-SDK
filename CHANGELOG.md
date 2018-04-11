# History of changes of NavigineSDK library

All notable changes to this project will be documented in this file. NavigineSDK
uses calendar versioning in the format `YYYYMMDD`.

## Version 20180411

* Improved navigation algorithms
* Fixed bugs in network API
* Added validation of parameter `server_url` according to the pattern:
`http[s]://hostname[:port]`
* DeviceInfo: added function getGlobalPoint for converting sublocation
coordinates of the device into the global geographic coordinates:
```java
public GlobalPoint getGlobalPoint()
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-DeviceInfo#function-getglobalpoint)

* Added abstract interface DeviceInfo.Listener:
```java
public interface DeviceInfo.Listener
{
  public void onUpdate(DeviceInfo info);
}
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-DeviceInfo.Listener)

* NavigationThread: added function setDeviceListener:
```java
void setDeviceListener(DeviceInfo.Listener listener)
```

Function is called every time when NavigationThread updates the device position.

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-setdevicelistener)

* LocationView.Listener: functions `onScroll(float x, float y)`, `onZoom(float
ratio)` are replaced with:
```java
onScroll(float x, float y, boolean isTouchEvent)
onZoom(float ratio, boolean isTouchEvent)
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-LocationView.Listener)

## Version 20180302

* NavigineSDK: function getLocationFile is marked as deprecated. In future it
will be removed.
* NavigationThread: function `loadLocation(String fileName)` replaced by:
```java
public boolean loadLocation(int locationId)
public boolean loadLocation(String location)
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-loadlocation)

## Version 20180221

* Improved navigation algorithms
* NavigationThread: added function for track recording:
```java
void setTrackFile(String fileName)
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-settrackfile)

## Version 20170601

* Support of PNG maps with large dimensions added.
* SVG maps support improved.
* Minor bugs were fixed.

## Version 20170427

* Fixed crush in location downloading.
* Navigation accuracy improved.

## Version 20170314

* Background service stability and battery usage improved.
* Navigation accuracy improved.

## Version 20170220

* Major updates in internal SDK storage - to support locations with local symbols in name.
* Navigation accuracy improved.
* Fixed bug with iBeacon battery level transmition to Navigine CMS.
