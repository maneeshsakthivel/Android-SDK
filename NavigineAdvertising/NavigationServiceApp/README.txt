Прикрепленный архив содержит:

1) Набор библиотек, необходимых для сборки и работы приложения:
 - NavigationServiceApp/libs/NavigationService.jar;
 - NavigationServiceApp/libs/android-support-v4.jar;
 - NavigationServiceApp/libs/play-services-base.jar;
 - NavigationServiceApp/libs/play-services-basement.jar;
 - NavigationServiceApp/libs/play-services-gcm.jar;
 - NavigationServiceApp/libs/play-services-iid.jar;
 - NavigationServiceApp/libs/play-services-tasks.jar.

Библиотека NavigationService.jar является основной и должна быть обязательно
добавлена в проект. Остальные библиотеки также могут быть добавлены, либо быть
подключены путем указания зависимости в gradle:

dependencies {
  compile 'com.android.support:appcompat-v7:24.0.0'
  compile "com.google.android.gms:play-services-gcm:9.2.+"
}

2) Пример кода (приложение NavigationServiceApp) для встраивания сервиса
уведомлений, в том числе его сборку (NavigationServiceApp-debug.apk)

Для встраивания сервиса уведомлений в приложение необходимо выполнить следующие
шаги.

1. Добавить jar-архив с NavigineServiceApp/libs/NavigationService.jar в проект.
2. Добавить в AndroidManifest.xml следующие строки (заменив всюду
"com.navigine.navigation_service_app" на package name вашего приложения):

Разрешения:
    
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <uses-permission android:name="com.navigine.navigation_service_app.gcm.permission.C2D_MESSAGE"/>

Объявление сервиса уведомлений и активити для показа уведомлений:
    
    <!-- Declaring NavigationService -->
    <service android:name="com.navigine.navigation_service.NavigationService"
             android:exported="true"/>
    
Объявление BroadcastReceiver для автоматического запуска сервиса при перезагрузке
телефона:

    <!-- Declaring broadcast receiver for BOOT_COMPLETED, PACKAGE_REPLACED events -->
    <receiver android:name="BootReceiver"
              android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED"/>
            <action android:name="android.intent.action.PACKAGE_REPLACED"/>
        </intent-filter>
    </receiver>

Для получения пуш-уведомлений посредством Google Cloud Platform:

    <!-- Google Cloud Messaging: start -->
    <receiver android:name="com.google.android.gms.gcm.GcmReceiver"
              android:permission="com.google.android.c2dm.permission.SEND"
              android:exported="true">
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
            <category android:name="com.navigine.navigation_service_app.gcm" />
        </intent-filter>
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
            <category android:name="com.navigine.navigation_service_app.gcm" />
        </intent-filter>
    </receiver>        
    <service android:name="GcmListenerService" android:exported="false">
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        </intent-filter>
    </service>
    <!-- Google Cloud Messaging: end -->

3. В исходный код приложения добавить классы:
 - BootReceiver (для автоматического запуска сервиса).
 - GcmListenerService (для построения уведомлений, полученных от Google Cloud Platform).
Пример класса: NavigineApp/src/com/navigine/navigation_service_app/GcmListenerService.java

4. В исходном коде приложения перед запуском сервиса уведомлений выполнить его
настройку:

Y.initialize(getApplication(), USER_HASH, SERVER_URL);

USER_HASH  - секретный код пользователя;
SERVER_URL - адрес сервера "https://api.navigine.com"

5. Запуск сервиса уведомлений:

Y.startService(getApplicationContext());

Сервис уведомлений должен запускаться автоматически при старте главной Activity.
В случае, если сервис должен работать в background (при выключенном приложении),
сервис приложений нужно также запускать из BootReceiver.

6. Остановка сервиса уведомлений:

Y.stopService();

Сервис уведомлений не должен останавливаться никогда!

7. Проверка, запущен ли сервис уведомлений:

Y.checkService();

8. Установка параметров сервиса:

Y.setUserHash(Context context, String userHash);
Y.setDebugLevel(Context context, int level);

BootReceiver также является точкой входа в приложение (сервис), поэтому в нем
перед его запуском сервиса необходимо установить секретный код пользователя
и отладочный уровень в logcat.

Поддерживается 4 уровня отладки:
 * 0 - отладочных сообшений нет (рекомендовано к использованию в финальной сборке приложения);
 * 1 - выводятся только сообщения об ошибках;
 * 2 - выводятся помимо ошибок также небольшое количество отладочных сообщений;
 * 3 - максимальное количество отладочных сообщений.

9. Получение параметров пуш-уведомления осуществляется следующим образом:

    public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService
    {
      final static String TAG = "NavigationService";
      @Override public void onMessageReceived(String from, Bundle data)
      {
        // Extracting data from Bundle
        String  id                  = data.getString("id");
        String  title               = data.getString("title");
        String  contentUrl          = data.getString("content");
        String  description         = data.getString("description");
        String  imageUrl            = data.getString("image");
        String  barcode             = data.getString("barcode");
        String  expirationDate      = data.getString("expirationDate");
        String  expirationDateLabel = data.getString("expirationDateLabel");
        
        // Do something... (build notification here)
      }

Все поля являются строковыми. Прои необходимости, их следует конвертировать в
нужный тип.
