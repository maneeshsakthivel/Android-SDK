# Back log of changes in NavigineSDK library

All notable changes to this project will be documented in this file. NavigineSDK
uses calendar versioning in the format `YYYYMMDD`.

## Version 20181207

Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/9f908e86d0b3869256d028d79f27a5d453cdb8c9/libs/NavigineSDK.jar?raw=true)

* Navigation algorithms updated to version 1.10

## Version 20181109
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/1f1c82ff1ec591cc0804899997ecec21acac1534/libs/NavigineSDK.jar?raw=true)

* Reworked notification handling (More information [here](https://github.com/Navigine/Android-SDK/wiki/Push-notifications))
* Added methods to LocationView.Listener
```java
  void onLoadFinished()
  void onLoadFailed()
```
* Navigation algorithms updated to version 1.8

## Version 20181009
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/d9466f503ac33dfd383dae03f3774fb7a92e417e/libs/NavigineSDK.jar?raw=true)

* Navigation algorithms updated to version 1.7

## Version 20180907
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/47688f140521e483fbdd342a9291062a10fa23b4/libs/NavigineSDK.jar?raw=true)

* Fixed memory leak in LocationView
* Navigation algorithms updated to version 1.5

## Version 20180720
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/de4e177cf41bbc2f71b6564710abf46b8763efbc/libs/NavigineSDK.jar?raw=true)

The following functions were made public:
```java
public static boolean NavigineSDK.loadLocation(String location)
public static boolean NavigineSDK.loadLocation(String location, int timeout)
```

## Version 20180712
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/d233ffbeb0874ba8c05bbece389f71f63166b433/libs/NavigineSDK.jar?raw=true)

* Fixed bug in NavigineSDK.loadLocationInBackgroundCancel
* Navigation algorithms updated to version 1.3

## Version 20180620
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/4e129d99487168b105e51981deaa2aa2dc3f2837/libs/NavigineSDK.jar?raw=true)

* [Location.LoadListener](https://github.com/Navigine/Android-SDK/wiki/Class-Location.LoadListener):
functions
```java
Location.LoadListener.onFinished ( String location )
Location.LoadListener.onFailed   ( String location, int error )
Location.LoadListener.onUpdate   ( String location, int error )

Location.LoadListener.onFinished ( int locationId)
Location.LoadListener.onFailed   ( int locationId, int error )
Location.LoadListener.onUpdate   ( int locationId, int error )
```
are replaced by:
```java
Location.LoadListener.onFinished ( )
Location.LoadListener.onFailed   ( int error )
Location.LoadListener.onUpdate   ( int error )
```

## Version 20180523
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/2f46c63ff66b60d34de2be3490a2ab1820d83d91/libs/NavigineSDK.jar?raw=true)

* Navigation algorithms updated to version 1.1.0

* Added functions:
```java
DeviceInfo.inZone(int id)
DeviceInfo.inZone(String alias)
SubLocation.getZone(String alias)
Location.getZone(String alias)
Location.getZones(LocationPoint P) 
```

## Version 20180513
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/f1c725d4b138351002001fed7e48d428d765466b/libs/NavigineSDK.jar?raw=true)

* Fixed synchronization bugs (deadlocks, ANRs)

## Version 20180503
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/5a74248a388685f5532976482162b1c5124dc253/NavigineSDK/NavigineSDK.jar?raw=true)

* Improved navigation algorithms

* Fixed validation regex for `server_url` parameter

* Added validation regex for `user_hash` parameter

* Added class [Location.LoadListener](https://github.com/Navigine/Android-SDK/wiki/Class-Location.LoadListener)

* Added asynchronous functions for loading locations for `NavigineSDK`:
  * [loadLocationInBackground](https://github.com/Navigine/Android-SDK/wiki/Class-NavigineSDK#function-loadlocationinbackground)
  * [loadLocationInBackgroundCancel](https://github.com/Navigine/Android-SDK/wiki/Class-NavigineSDK#function-loadlocationinbackgroundCancel)

* Added beacon emulation functions for `NavigationThread`:
  * [addBeaconGenerator](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-addbeacongenerator),
  * [removeBeaconGenerator](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-removebeacongenerator),
  * [removeBeaconGenerators](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-removebeacongenerators)

## Version 20180423
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/blob/b9eb40e39166ab873490ef09a8a602863559d466/NavigineSDK/NavigineSDK.jar?raw=true)

* Fixed bug in `DeviceInfo` radius (member field `DeviceInfo.r`)

* Fixed bug in `DeviceInfo` conversion to `GlobalPoint` (function `DeviceInfo.getGlobalPoint`)

* Added periodic BLE scan restart (to fix the problem of stopping BLE measurements after 30 minutes of continuous scanning on Android 7 or higher)

* Added extra validation of the location archives after downloading

## Version 20180411
Download link: [NavigineSDK.jar](https://github.com/Navigine/Android-SDK/raw/d4c0e75ed5b40c266da4561f91a9f1070fd3196c/NavigineSDK/NavigineSDK.jar)

* Improved navigation algorithms

* Fixed bugs in network API

* Added validation of parameter `server_url` according to the pattern:
`http[s]://hostname[:port]`

* DeviceInfo: added function `getGlobalPoint` for converting sublocation
coordinates of the device into the global geographic coordinates:
[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-DeviceInfo#function-getglobalpoint)

* Added abstract interface `DeviceInfo.Listener`:
[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-DeviceInfo.Listener)

* NavigationThread: added function `setDeviceListener`. Function is called every time when NavigationThread updates the device position: [wiki](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-setdevicelistener)

* LocationView.Listener: functions
```java
void onScroll (float x, float y)
void onZoom   (float ratio)
```
are replaced by:
```java
void onScroll (float x, float y, boolean isTouchEvent)
void onZoom   (float ratio,      boolean isTouchEvent)
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-LocationView.Listener)

## Version 20180302

* NavigineSDK: function `getLocationFile` is marked as **deprecated**. In future it
will be removed.

* NavigationThread: function `loadLocation`, taking the location archive file
path as argument, is replaced by:
```java
public boolean loadLocation(int locationId)
public boolean loadLocation(String location)
```

[wiki](https://github.com/Navigine/Android-SDK/wiki/Class-NavigationThread#function-loadlocation)

## Version 20180221

* Improved navigation algorithms

* NavigationThread: added function `setTrackFile` for track recording:
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
