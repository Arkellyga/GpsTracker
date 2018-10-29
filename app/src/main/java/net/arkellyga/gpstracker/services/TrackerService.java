package net.arkellyga.gpstracker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.arkellyga.gpstracker.utils.LocationFactory;
import net.arkellyga.gpstracker.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackerService extends Service {
    private static final String TAG = "TrackerService";
    private static final String LOCATION_INTERVAL = "location_interval";
    private static final String COMMAND_EXTRA = "command";
    private static final int COMMAND_STOP = 0;

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
            Toast.makeText(this, R.string.service_error, Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        int command = intent.getIntExtra(COMMAND_EXTRA, -1);
        if (command == COMMAND_STOP) {
            stopForeground(true);
        }
        mListener.onProviderEnabled("GPS");
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                mUpdateInterval, 0, mListener);
        Log.d(TAG, "run: timer set location");
        setNotification(getResources().getString(R.string.start_tracking), null);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mFactory = new LocationFactory();
        mFactory.startRecord();
        String interval = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(LOCATION_INTERVAL, "60");
        if (interval != null)
            mUpdateInterval = Integer.parseInt(interval) * 1000;
        else
            mUpdateInterval = 60 * 1000;
    }

    private void setNotification(String content, @Nullable String bigText) {
        Notification notification;
        Notification.Builder builder = new Notification.Builder(this)
                .setOnlyAlertOnce(true)
                .setContentText(content)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher);
        // Add button for cancel service
        // TODO: 30.10.18 : Add button for SDK < KITKAT_WATCH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            Intent intent = new Intent(this, TrackerService.class);
            intent.putExtra(COMMAND_EXTRA, COMMAND_STOP);
            PendingIntent pI = PendingIntent.getService(this, 0, intent, 0);
            builder.addAction(new Notification.Action(android.R.drawable.ic_delete,
                    getResources().getString(R.string.notification_stop),
                    pI));
        }
        // Add bigtext for coordinates
        if (bigText != null)
            builder.setStyle(new Notification.BigTextStyle().bigText(bigText));
        // For android O add notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Resources res = getResources();
            CharSequence name = res.getString(R.string.notification_channel_name);
            String description = res.getString(R.string.notification_channel_description);
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
            StringBuilder data = new StringBuilder();
            Resources res = getResources();
            data.append(res.getString(R.string.notification_longitude, location.getLongitude()));
            data.append("\n");
            data.append(res.getString(R.string.notification_latitude, location.getLatitude()));
            data.append("\n");
            SimpleDateFormat date = new SimpleDateFormat("hh:mm:ss dd-MM-y", Locale.getDefault());
            String time = res.getString(R.string.notification_time, date.format(new Date(location.getTime())));
            data.append(time);
            setNotification(time, data.toString());
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
