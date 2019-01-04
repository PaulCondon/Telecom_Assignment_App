package gps_application.evismar.tutorials.com.servicesample;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;


public class BluetoothGPSService extends Service implements LocationListener {

    private static final String TAG = "BluetoothGPSService";
    private LocationManager locationManager;
    private BluetoothAdapter bluetoothAdapter;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private LocationData newLocation;
    private int fifteenMinutes = 900000;
    private int numDevices;
    private ArrayList<String> deviceList;
    private ArrayList<String> devicesInCurrLocation;
    private ArrayList<String> list;
    private Handler handler;
    private boolean GPSEnabled;
    private boolean GPSAvailable;
    private boolean networkEnabled;
    private boolean networkAvailable;

    public BluetoothGPSService(){ }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate() {
        newLocation = new LocationData(0, 0, 0);
        //set up Firebase reference
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();
        //initialise array lists
        list = new ArrayList<>();
        deviceList = new ArrayList<>();
        devicesInCurrLocation = new ArrayList<>();
        //get devices stored in database
        getKnownDevices();
        //set up location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 1000,
                    0, this);
        }
        catch (SecurityException e) {
            e.getMessage();
        }
        catch (Exception e) {
            e.getMessage();
        }
        //set up bluetooth scanner
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(bluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //if adapter not enabled then enable it
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
        Log.e(TAG, "Bluetooth & GPS Service Started");
        handler = new Handler();
        delayPeriod.run();
    }


    //creates the 15 minute interval between recordings
    Runnable delayPeriod = new Runnable() {
        @Override
        public void run() {
            try{}
            finally {
                handler.postDelayed(delayPeriod, fifteenMinutes);
                //check whether to use network or gps
                checkUseGPSorNetwork();
                //scan for bluetooth devices
                bluetoothScan();
                //record location with number of devices found in scan
                newLocation(newLocation);
            }
        }
    };

    private void checkUseGPSorNetwork() {
        //check gps permission is in manifest
        if (ContextCompat.checkSelfPermission( this,
                android.Manifest.permission.ACCESS_FINE_LOCATION ) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED)                          
        {
            //Get status of GPS and Network services
            try{
                GPSEnabled =locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
            catch(Exception ex){}
            try{
                networkEnabled =locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
            catch(Exception ex){}

            //If GPS now enabled and was unavailable then use it
            if (GPSEnabled && !GPSAvailable){
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, fifteenMinutes,
                        0,this);
                GPSAvailable = true;
                networkAvailable = false;
            }
            //Otherwise If Network now enabled and was unavailable then use it
            else if (networkEnabled && !networkAvailable){
                locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER,
                        fifteenMinutes ,0,this);
                GPSAvailable = false;
            }
            //Not granted permission by user and cannot continue
            else{
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT);
            }
        }
    }

    public void newLocation(LocationData location) {
        //set the number of devices found
        location.setNumBluetoothDevices(numDevices);
        //get a time stamp
        String time = String.valueOf(new Date().getTime());
        //upload location and number of devices found to firebase
        dbRef.child("Locations").child(time).setValue(location);
        Log.e(TAG, "Location recorded");
        //empty list for next recording
        devicesInCurrLocation.clear();
        //update user
        Toast.makeText(this, "Location Uploaded - "+ numDevices+ " devices found",
                Toast.LENGTH_LONG);
    }



    private void bluetoothScan() {
        //set numDevices to default
        numDevices = 0;
        //if a previous scan is occurring, override it
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        //begin scan
        bluetoothAdapter.startDiscovery();
        Log.e(TAG, "Started scanning for bluetooth devices");
        //wait for scan to complete
        while (bluetoothAdapter.isDiscovering()){ }
        Log.e(TAG, "Finished scanning for bluetooth devices");
        //if devices are found then set numDevices to the amount of devices found
        if(!devicesInCurrLocation.isEmpty()){
            numDevices = devicesInCurrLocation.size();
            Log.e(TAG, "" + numDevices + " device(s) nearby");
        }
        //otherwise leave numDevices as its default value
        else {
            Log.e(TAG, "No nearby bluetooth devices");
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if a device is found
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                Log.e(TAG, "Bluetooth device nearby");
                //extract device information
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                //add device name to list of devices at this location
                devicesInCurrLocation.add(deviceName);


                boolean newDevice = true;
                //set deviceList = to devices in database
                for (String item: getKnownDevices()) {
                    deviceList.add(item);
                    //if deviceName is already in the list it will trigger newDevice
                    // to switch to false
                    if (deviceName.contentEquals(item)) {
                        newDevice = false;
                        Log.e(TAG, "Device "+ item+" is not new");
                    }

                }
                //if device is new then add it to database
                if (newDevice) {
                    dbRef.child("Devices").child(deviceAddress).setValue(deviceName);
                    Log.e(TAG, "New Bluetooth device detected: "+ deviceName);
                }
            }
        }
    };

    //pulls down all known devices from database
    private ArrayList<String> getKnownDevices() {
        //database reference
        dbRef.child("Devices").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //empty list
                list.clear();
                //loop through all values
                for (DataSnapshot reference : dataSnapshot.getChildren()) {
                    String device = reference.getValue(String.class);
                    // add to list if value isn't null
                    if (device != null){
                        list.add(device);
                        //Log.e(TAG, "Known Device: " + device);
                    }
                }
                //release event listener
                dbRef.child("Devices").removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //return the list of devices from database
        return list;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
        // Remove any pending posts in message queue
        handler.removeCallbacks(delayPeriod);
    }

    @Override
    public void onLocationChanged(Location location) {
        newLocation = new LocationData(location.getLatitude(), location.getLongitude(), 0);
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
