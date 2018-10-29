package net.arkellyga.gpstracker.utils;

public class GeoPoint {
    private double mLatitude;
    private double mLongitude;
    private long mDate;

    public GeoPoint() {}

    public GeoPoint(double latitude, double longitude, long date) {
        mLatitude = latitude;
        mLongitude = longitude;
        mDate = date;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long mDate) {
        this.mDate = mDate;
    }
}
