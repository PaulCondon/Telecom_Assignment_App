package gps_application.evismar.tutorials.com.servicesample;

import java.util.ArrayList;

/**
 * Created by finnk on 11/5/2016.
 */

public class LocationData {

    public double longitude;
    public double latitude;
    public int numBluetoothDevices;

    public LocationData() {

    }

    public LocationData(double lati, double longi, int BTDs) {
        this.longitude = longi;
        this.latitude = lati;
        this.numBluetoothDevices = BTDs;
    }


    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getNumBluetoothDevices() { return numBluetoothDevices; }
}
