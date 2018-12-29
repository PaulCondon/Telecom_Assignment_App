package gps_application.evismar.tutorials.com.servicesample;

/**
 * Created by finnk on 11/5/2016.
 */

public class LocationData {

    public double latitude;
    public double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    LocationData(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public LocationData() {
    }



}
