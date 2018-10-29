package net.arkellyga.gpstracker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.arkellyga.gpstracker.utils.LocationFactory;
import net.arkellyga.gpstracker.R;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TrackerService extends Service {
    private static final String TAG = "TrackerService";
    private static final String LOCATION_INTERVAL = "location_interval";
    private int mUpdateInterval;

    private LocationFactory mFactory;

    private LocationManager mLocationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ContextCompat.checkSelfPermission(TrackerService.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
        }
        mListener.onProviderEnabled("GPS");
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                mUpdateInterval, 0, mListener);
        Log.d(TAG, "run: timer set location");
        setNotification(getResources().getString(R.string.start_tracking));
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mFactory = new LocationFactory(this);
        mFactory.startRecord();
        String interval = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(LOCATION_INTERVAL, "60");
        if (interval != null)
            mUpdateInterval = Integer.parseInt(interval) * 1000;
        else
            mUpdateInterval = 60 * 1000;
    }

    private void setNotification(String content) {
        Notification notification;
        Notification.Builder builder = new Notification.Builder(this)
                .setOnlyAlertOnce(true)
                .setContentText(content)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "Gps tracker";
            String description = "Tracking for me";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("gps_service", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId("gps_service");
        }
        notification = builder.build();
        startForeground(100, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mLocationManager.removeUpdates(mListener);
        stopForeground(false);
    }

    private LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: location changed");
            mFactory.addPoint(location.getLongitude(), location.getLatitude(), location.getTime());
            String data = String.format(Locale.ENGLISH,
                    "Longitude = %f, \n Latitude = %f", location.getLongitude(), location.getLatitude());
            setNotification(data);
            mFactory.save();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: ");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: ");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: ");
        }
    };
}
