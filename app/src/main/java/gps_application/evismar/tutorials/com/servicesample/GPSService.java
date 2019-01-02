package gps_application.evismar.tutorials.com.servicesample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GPSService extends Service implements android.location.LocationListener {
    private LocationManager lm;
    private static final String TAG = "#GPS_Service";
    private DatabaseReference mDatabase;
    public GPSService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            //App 2 todo: request updates here
            //update every 15 minutes (in milliseconds, 1000 * 60 * 15)
            lm.requestLocationUpdates(lm.NETWORK_PROVIDER,1000 * 60 * 15, 10,this );
        }
        catch (SecurityException e) {
            Log.e(TAG, "exception occured " + e.getMessage());
        }
        catch (Exception e) {
            Log.e(TAG, "exception occured " + e.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (lm != null) {
            try {
                lm.removeUpdates(this);
            }
            catch (SecurityException e) {
                Log.e(TAG, "exception occured " + e.getMessage());
            }
            catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    public void onLocationChanged(Location arg0) {

        //App 2  todo: upload location to Firebase
        LocationData obj = new LocationData(arg0.getLatitude(),arg0.getLongitude());
        mDatabase.child("locations").push().setValue(obj);
        Log.e(TAG, "Location logged: " + obj.latitude + " " + obj.longitude);
    }

    public void onProviderDisabled(String arg0) {
        Log.e(TAG, "provider disabled " + arg0);
    }

    public void onProviderEnabled(String arg0) {
        Log.e(TAG, "provider enabled " + arg0);
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        Log.e(TAG, "status changed to " + arg0 + " [" + arg1 + "]");
    }
}
