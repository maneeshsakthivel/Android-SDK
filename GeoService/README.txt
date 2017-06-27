Проект содержит:

 - библиотеку GeoService, позволяющую в фоновом режиме осуществлять периодическое
 сканирование текущих географических координат устройства и отправку их на
 сервер;
    
 - демо-приложение GeoServiceApp (с исходными кодами), демонстрирующее
использование библиотеки GeoService.

Для встраивания сервиса GeoService в приложение необходимо выполнить следующие
шаги.

1. Добавить библиотеку GeoService/GeoService.jar в проект.

2. Добавить в AndroidManifest.xml:

 - следующие разрешения:

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

 - объявление GeoService и GeoReceiver:
    
    <!-- Declaring GeoService -->
    <service android:name="com.navigine.geo_service.GeoService"/>
  
    <!-- Declaring GeoReceiver -->
    <receiver android:name="com.navigine.geo_service.GeoReceiver">
      <intent-filter>
        <action android:name="com.navigine.geo_service.SERVICE_START"/>
        <action android:name="com.navigine.geo_service.SERVICE_STOP"/>
        <action android:name="com.navigine.geo_service.SERVICE_WAKE"/>
      </intent-filter>
    </receiver>

 - объявление ресивера для автоматического запуска гео-сервиса при перезагрузке   
телефона:

    <!-- Declaring BootReceiver (for GeoService to start automatically on device reboot) -->
    <receiver android:name="BootReceiver" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <action android:name="android.intent.action.PACKAGE_REPLACED"/>
      </intent-filter>
    </receiver>

3. В исходный код приложения добавить класс BootReceiver, в котором при
наступлении события BOOT_COMPLETED или PACKAGE_REPLACED выполнить настройку
параметров и запуск GeoService.

Пример класса: NavigineApp/src/com/navigine/navigation_service_app/BootReceiver.java

4. В исходном коде приложения:

 - выполнить настройку параметров и запуск GeoService:

    // Initializing GeoService parameters
    GeoService.setParameter(this, "debug_level",      Default.DEBUG_LEVEL);
    GeoService.setParameter(this, "gps_scan_timeout", Default.GPS_SCAN_TIMEOUT);
    GeoService.setParameter(this, "wake_frequency",   Default.WAKE_FREQUENCY);
    
    // Starting GeoService
    GeoService.startService(getApplicationContext());

  Сервис должен запускаться автоматически при старте главной Activity. При
  остановке или выходе из приложения останавливать GeoService не следует! Он
  продолжит работать в фоновом режиме и собирать данные о местоположении
  устройства.

 - зарегистрировать обработчик события com.navigine.geo_service.LOCATION_UPDATE:

    mLocationReceiver =
      new BroadcastReceiver()
      {
        @Override public void onReceive(Context ctxt, Intent intent)
        {
          final int id = intent.getIntExtra("id", 0);
          Location location = intent.getParcelableExtra("location");
          ...
        }
      };
    registerReceiver(mLocationReceiver, new IntentFilter("com.navigine.geo_service.LOCATION_UPDATE"));

Пример класса: NavigineApp/src/com/navigine/navigation_service_app/MainActivity.java

5. Проверка, запущен ли сервис:

GeoService.isStarted();

6. Параметры сервиса:

 - debug_level - желаемый уровень отладки.
 
  Поддерживается 4 уровня отладки:
   * 0 - отладочных сообшений нет (рекомендовано к использованию в финальной сборке приложения);
   * 1 - выводятся только сообщения об ошибках;
   * 2 - выводятся помимо ошибок также небольшое количество отладочных сообщений;
   * 3 - максимальное количество отладочных сообщений.

 - gps_scan_timeout - продолжительность непрерывного сканирования GPS сигнала в
 течении одного проббуждения сервиса (в секундах). Диапазон значений: 10-60 сек.
 
 - wake_frequency - частота пробуждения сервиса (в секундах).
 Диапазон значений: 60-3600 сек.
 