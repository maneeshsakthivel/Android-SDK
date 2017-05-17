Проект содержит:

 - библиотеку GeoService, позволяющую в фоновом режиме осуществлять периодическое
 сканирование текущих географических координат устройства и отправку их на
 сервер:
    
    GeoServiceApp/libs/GeoService.jar

 - демо-приложение GeoServiceApp (с исходными кодами), демонстрирующее
использование библиотеки GeoService:

    GeoServiceApp/src/com/navigine/geo_service_app/
    GeoServiceApp/GeoServiceApp-debug.apk
 
Для встраивания сервиса уведомлений в приложение необходимо выполнить следующие
шаги.

1. Добавить библиотеку GeoServiceApp/libs/GeoService.jar в проект.

2. Добавить в AndroidManifest.xml:

 - следующие разрешения:

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

 - объявление GeoService и GeoReceiver:
    
    <!-- Declaring GeoService -->
    <service android:name="com.navigine.geo_service.GeoService" android:exported="true"/>
  
    <!-- Declaring GeoReceiver -->
    <receiver android:name="com.navigine.geo_service.GeoReceiver" android:exported="true">
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
    GeoService.setParameter(this, "max_cache_size",   Default.MAX_CACHE_SIZE);
    GeoService.setParameter(this, "wake_frequency",   Default.WAKE_FREQUENCY);
    GeoService.setParameter(this, "send_frequency",   Default.SEND_FREQUENCY);
    GeoService.setParameter(this, "send_timeout",     Default.SEND_TIMEOUT);
    GeoService.setParameter(this, "send_url",         Default.SEND_URL);
    
    // Starting GeoService
    GeoService.startService(getApplicationContext());

  Сервис уведомлений должен запускаться автоматически при старте главной
  Activity. Останавливать сервис уведомлений не следует никогда!

 - зарегистрировать обработчик события com.navigine.geo_service.LOCATION_UPDATE:

    mLocationReceiver =
      new BroadcastReceiver()
      {
        @Override public void onReceive(Context ctxt, Intent intent)
        {
          final String latitude  = intent.getStringExtra("latitude");
          final String longitude = intent.getStringExtra("longitude");
          final String accuracy  = intent.getStringExtra("accuracy");
          final String time      = intent.getStringExtra("time");
          ...
        }
      };
    registerReceiver(mLocationReceiver, new IntentFilter("com.navigine.geo_service.LOCATION_UPDATE"));

Пример класса: NavigineApp/src/com/navigine/navigation_service_app/MainActivity.java

5. Проверка, запущен ли сервис уведомлений:

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
 
 - max_cache_size - максимальное количество записей во внутреннем хранилище
 приложения. Диапазон значений: 1000-10000.
 
 - wake_frequency - частота пробуждения сервиса (в секундах).
 Диапазон значений: 60-3600 сек.
 
 - send_frequency - частота отправки данных (в секундах).
 Диапазон значений: 60-3600 сек.
 
 - send_timeout - таймаут отправки данных в течение одного пробуждения (включает
 таймаут соединения с сервером, таймаут передачи данных, таймаут получения
 подтверждения от сервера). Диапазон значений: 10-60 сек.
 
 - send_url - HTTP URL для отправки данных.
