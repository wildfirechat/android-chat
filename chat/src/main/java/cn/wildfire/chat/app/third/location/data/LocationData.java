package cn.wildfire.chat.app.third.location.data;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class LocationData implements Serializable {
    private double lat;
    private double lng;
    private String poi;
    private byte[] thumbnailData;


    public LocationData(double lat, double lng, String poi, Bitmap thumbnail) {
        this.lat = lat;
        this.lng = lng;
        this.poi = poi;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, baos);
        this.thumbnailData = baos.toByteArray();
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getPoi() {
        return poi;
    }

    public void setPoi(String poi) {
        this.poi = poi;
    }

    public Bitmap getThumbnail() {
        return BitmapFactory.decodeByteArray(thumbnailData, 0, thumbnailData.length);
    }
}
