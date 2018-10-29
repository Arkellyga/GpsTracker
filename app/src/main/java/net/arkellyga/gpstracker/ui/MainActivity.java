package net.arkellyga.gpstracker.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;

import net.arkellyga.gpstracker.BuildConfig;
import net.arkellyga.gpstracker.utils.GeoPoint;
import net.arkellyga.gpstracker.utils.LocationFactory;
import net.arkellyga.gpstracker.R;
import net.arkellyga.gpstracker.services.TrackerService;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 0;

    private Intent mService;
    private MapView mMapView;
    private Spinner mSpFiles;
    private LocationFactory mFactory;
    private boolean mIsServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLocale();
        requestStorage();
        MapKitFactory.setApiKey(BuildConfig.MapKitKey);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_main);
        initializeMapView();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFactory = new LocationFactory();
        mService = new Intent(MainActivity.this, TrackerService.class);
        setupSpinner();
    }

    private void setupSpinner() {
        mSpFiles = findViewById(R.id.main_spinner_files);
        String[] tracks = mFactory.getTracks();
        if (tracks == null)
            return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                tracks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpFiles.setAdapter(adapter);
        mSpFiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMapView.getMap().getMapObjects().clear();
                loadPoints(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        loadPoints(tracks.length - 1);
    }

    private void initializeMapView() {

        mMapView = findViewById(R.id.main_map_view);
        mMapView.getMap().move(
                new CameraPosition(new Point(48, 38), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

    }

    private void addPoint(double latitude, double longitude) {
        mMapView.getMap().getMapObjects().addPlacemark(new Point(latitude, longitude));
    }

    private void loadPoints(int index) {
        ArrayList<GeoPoint> points = mFactory.getPoints(index);
        if (points.isEmpty())
            return;
        for (GeoPoint point: points) {
            addPoint(point.getLatitude(), point.getLongitude());
        }
        Point ypoint = new Point(points.get(0).getLatitude(), points.get(0).getLongitude());
        mMapView.getMap().move(
                new CameraPosition(ypoint, 15.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);
    }

    private void requestStorage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                                      Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                      Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    recreate();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                    dialog.setTitle(android.R.string.dialog_alert_title);
                    dialog.setMessage(getResources().getString(R.string.alert_dialog_content));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                            getResources().getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    MainActivity.this.finish();
                                }
                            });
                    dialog.show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isMyServiceRunning(TrackerService.class)) {
            menu.findItem(R.id.menu_item_switch_service).setTitle(getResources().getString(R.string.menu_item_stop_service));
            mIsServiceRunning = true;
        }
        else {
            menu.findItem(R.id.menu_item_switch_service).setTitle(getResources().getString(R.string.menu_item_start_service));
            mIsServiceRunning = false;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_switch_service:
                if (mIsServiceRunning) {
                    item.setTitle(getResources().getString(R.string.menu_item_start_service));
                    stopService(mService);
                    mIsServiceRunning = false;
                } else {
                    item.setTitle(getResources().getString(R.string.menu_item_stop_service));
                    startService(mService);
                    mIsServiceRunning = true;
                }
                break;
            case R.id.menu_item_about:
                Toast.makeText(this, R.string.about_toast, Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
        MapKitFactory.getInstance().onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        MapKitFactory.getInstance().onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setLocale() {
        String lang = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", null);
        if (lang == null)
            return;
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, null);
    }
}
