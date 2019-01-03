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
    private int fifteenMinutes = 30000; //900000;
    private ArrayList<String> deviceList;
    private ArrayList<String> devicesInCurrLocation;
    private Handler handler;
    private int numDevices;
    private LocationData newLocation;
    private boolean GPSEnabled;
    private boolean GPSAvailable;
    private boolean networkEnabled;
    private boolean networkAvailable;
    private ArrayList<String> list = new ArrayList<>();

    public BluetoothGPSService(){ }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate() {
        numDevices = 0;
        newLocation = new LocationData(0, 0, 0);
        database = FirebaseDatabase.getInstance();
        deviceList = new ArrayList<>();
        devicesInCurrLocation = new ArrayList<>();
        dbRef = database.getReference();
        list = new ArrayList<>();
        getKnownDevices();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, fifteenMinutes,
                    1, this);
        }
        catch (SecurityException e) {
            e.getMessage();
        }
        catch (Exception e) {
            e.getMessage();
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(bluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
        Log.e(TAG, "Bluetooth & GPS Service Started");
        handler = new Handler();
        delayPeriod.run();
    }



    Runnable delayPeriod = new Runnable() {
        @Override
        public void run() {
            try{}
            finally {
                handler.postDelayed(delayPeriod, fifteenMinutes);
                checkUseGPSorNetwork();
                bluetoothScan();
                newLocation(newLocation);
            }
        }
    };

    private void checkUseGPSorNetwork() {
        if (ContextCompat.checkSelfPermission( this,
                android.Manifest.permission.ACCESS_FINE_LOCATION ) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED)                          
        {
            //Get status of GPS and Network services
            try{
                GPSEnabled =locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
            try{
                networkEnabled =locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

            //If GPS now enabled and was unavailable then use it
            if (GPSEnabled && !GPSAvailable){
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER,fifteenMinutes ,1,this);   //Start using GPS - update every 10 mins
                GPSAvailable = true;
                networkAvailable = false;
            }
            //Otherwise If Network now enabled and was unavailable then use it
            else if (networkEnabled && !networkAvailable){
                locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER,fifteenMinutes ,1,this); //Start using GPS - update every 10 mins
                networkAvailable = true;
                GPSAvailable = false;
            }
            else{
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT);
            }
        }
    }

    public void newLocation(LocationData location) {
        location.setNumBluetoothDevices(numDevices);
        String time = String.valueOf(new Date().getTime());
        dbRef.child("Locations").child(time).setValue(location);
        Log.e(TAG, "Location recorded");
        devicesInCurrLocation.clear();

    }

    private ArrayList<String> getKnownDevices() {

        dbRef.child("Devices").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                for (DataSnapshot reference : dataSnapshot.getChildren()) {             //Loop over all unique device entries in the database
                    String device = reference.getValue().toString();
                    if (device != null){
                        device = device.replaceAll("\\[", "").replaceAll("\\]","");
                        list.add(device);
                        Log.e(TAG, "Known Device: " + device);

                    }
                }
                dbRef.child("Devices").removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return list;
    }

    private void bluetoothScan() {
        numDevices = 0;
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        Log.e(TAG, "Started scanning for bluetooth devices");
        while (bluetoothAdapter.isDiscovering()){
            Log.e(TAG, "Scanning for bluetooth devices");
        }
        Log.e(TAG, "Finished scanning for bluetooth devices");

        if(!devicesInCurrLocation.isEmpty()){
            numDevices = devicesInCurrLocation.size();
            Log.e(TAG, "" + numDevices + " device(s) nearby");
        }
        else {
            Log.e(TAG, "No nearby bluetooth devices");
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                Log.e(TAG, "Bluetooth device nearby");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                devicesInCurrLocation.add(deviceName);


                boolean newDevice = true;
                if(getKnownDevices().isEmpty()){
                    Log.e(TAG, "EMPTY METHOD");
                }
                for (String item: getKnownDevices()) {
                    deviceList.add(item);
                    if (item == deviceName) {
                        newDevice = false;

                    }
                    Log.e(TAG, "FALSE TRIGGERED "+"---"+ item+"---");
                }
                //if new add it to list and database
                if (newDevice) {
                    deviceList.add(deviceName);
                    dbRef.child("Devices").child(deviceAddress).setValue(deviceName);
                    Log.e(TAG, "New Bluetooth device detected: " +"---"+ deviceName+"---");
                }
            }
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
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
