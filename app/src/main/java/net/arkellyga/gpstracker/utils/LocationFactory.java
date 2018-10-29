package net.arkellyga.gpstracker.utils;

import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LocationFactory {
    private static final String DATE  = "date";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";

    private static final String mFileDir = "GpsTracker";
    private JSONArray mArray;
    private String mFileName;
    private String[] mFiles;

    public LocationFactory() {
        //openFile();
        loadFiles();
    }

    private void loadFiles() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/" + mFileDir);
        if (!dir.exists())
            dir.mkdir();
        if (dir.listFiles().length == 0)
            return;
        String[] files = new String[dir.listFiles().length];
        for (int i = 0; i < files.length; i++)
            files[i] = dir.listFiles()[i].getName();
        mFiles = files;
    }

    public void addPoint(double longitude, double latitude, long date) {
        JSONObject object = new JSONObject();
        try {
            object.put(LONGITUDE, longitude);
            object.put(LATITUDE, latitude);
            object.put(DATE, date);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        mArray.put(object);
    }

    public ArrayList<GeoPoint> getPoints(int index) {
        loadFile(index);
        ArrayList<GeoPoint> list = new ArrayList<>();
        JSONObject object;
        GeoPoint point;
        try {
            for (int i = 0; i < mArray.length(); i++) {
                object = mArray.getJSONObject(i);
                point = new GeoPoint();
                point.setDate(object.getLong(DATE));
                point.setLatitude(object.getDouble(LATITUDE));
                point.setLongitude(object.getDouble(LONGITUDE));
                list.add(point);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void loadFile(int index) {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + mFileDir);
        if (!file.exists() || mFiles == null) {
            return;
        }
        file = new File(file.getPath() + "/" + mFiles[index]);
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            int content;
            try {
                FileInputStream is = new FileInputStream(file);
                while ((content = is.read()) != -1) {
                    sb.append((char) content);
                }
                mArray = new JSONArray(sb.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        } else
            mArray = new JSONArray();
    }

    public void startRecord() {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + mFileDir);
        if (!file.exists()) {
            file.mkdir();
        }
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-y_hh:mm:ss", Locale.getDefault());
        mFileName = "track-" + df.format(new Date()) + ".js";
        mArray = new JSONArray();
    }

    public String[] getTracks() {
        return mFiles;
    }

    public void save() {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/" + mFileDir + "/" + mFileName);
            FileWriter writer = new FileWriter(file);
            writer.write(mArray.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
