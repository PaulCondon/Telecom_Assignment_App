package gps_application.evismar.tutorials.com.servicesample;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private double lastLocationLat;
    private double lastLocationLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //set up Firebase reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        startService(new Intent(this, BluetoothGPSService.class));
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            setUpMap();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */

    private void setUpMap() {
        final DatabaseReference ref = mDatabase.child("Locations").getRef();
        // Attach a listener to read the data at our posts reference
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long max = 0;
                //read from database
                for (DataSnapshot locSnapshot : dataSnapshot.getChildren()) {
                    //get location from database
                    LocationData loc = locSnapshot.getValue(LocationData.class);
                    if (loc != null) {
                        //add marker to the map
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(loc.latitude, loc.longitude))
                                //give marker a title of the number of devices at that location
                                .title("No. Bluetooth Devices: "+loc.getNumBluetoothDevices()));
                        //get the most recent location coordinates
                        long recordedTime  = Long.parseLong(locSnapshot.getKey());
                        if (recordedTime > max) {
                            max = recordedTime;
                            lastLocationLat = loc.latitude;
                            lastLocationLong = loc.longitude;
                            Log.i("MyTag", "onDataChange:" + lastLocationLat +
                                    ", "+ lastLocationLong);
                        }
                    }
                }
                Log.i("MyTag", "camera:" + lastLocationLat +", "+ lastLocationLong);
                //center the map on the most recent marker
                CameraUpdate center =
                        CameraUpdateFactory.newLatLng(new LatLng(lastLocationLat,lastLocationLong));
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(8);
                mMap.moveCamera(center);
                mMap.animateCamera(zoom);
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}


