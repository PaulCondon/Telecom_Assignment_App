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
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;


public class BluetoothGPSService extends Service implements LocationListener {

    private LocationManager locationManager;
    private BluetoothAdapter bluetoothAdapter;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private int fifteenMinutes = 30000; //900000;
    private ArrayList<String> deviceList;
    private ArrayList<String> devicesInCurrLocation;
    private Handler handler;
    private boolean GPS_on = false;
    private boolean network_on = false;
    private boolean GPS_running = false;
    private boolean network_running =false;

    public BluetoothGPSService(){ }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate() {

        database = FirebaseDatabase.getInstance();
        deviceList = new ArrayList<>();
        devicesInCurrLocation = new ArrayList<>();
        getKnownDevices();
        dbRef = database.getReference();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, fifteenMinutes,
                    10000, this);
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

        Toast.makeText(BluetoothGPSService.this,"Bluetooth & GPS Service Started",
                Toast.LENGTH_SHORT).show();

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
            }
        }
    };

    private void checkUseGPSorNetwork() {
        if (ContextCompat.checkSelfPermission( this,
                android.Manifest.permission.ACCESS_FINE_LOCATION ) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED){
            try{
                GPS_on = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
            catch(Exception e){}
            try{
                network_on = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
            catch(Exception e){}

            if(GPS_on && !GPS_running){
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, fifteenMinutes,
                        5000, this);
                GPS_running = true;
                network_running = false;
            }
            else {
                locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, fifteenMinutes,
                        5000, this);
                GPS_running = false;
                network_running = true;
            }
        }
    }

    private void getKnownDevices() {
        final DatabaseReference tempRef = database.getReference().child("Devices");
        tempRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot reference : dataSnapshot.getChildren()) {             //Loop over all unique device entries in the database
                    String device = reference.getValue().toString();
                    if (device != null){
                        device = device.replaceAll("\\[", "").replaceAll("\\]","");
                        deviceList.add(device);
                    }
                }
                tempRef.removeEventListener(this);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void onLocationChanged(Location location) {
        bluetoothAdapter.startDiscovery();
        while (bluetoothAdapter.isDiscovering()){Toast.makeText(BluetoothGPSService.this,
                "Scanning for devices", Toast.LENGTH_LONG).show();}
        Toast.makeText(BluetoothGPSService.this,
                "Finished scanning for devices", Toast.LENGTH_LONG).show();
        int numDevices;
        if(!devicesInCurrLocation.isEmpty()){
            numDevices = devicesInCurrLocation.size();
            Toast.makeText(BluetoothGPSService.this,
                    "" + numDevices + " device(s) nearby", Toast.LENGTH_SHORT).show();
        }
        else {
            numDevices = 0;
            Toast.makeText(BluetoothGPSService.this,
                    "No devices nearby", Toast.LENGTH_SHORT).show();
        }
        LocationData locationData = new LocationData(location.getLatitude(),location.getLongitude(), numDevices);
        devicesInCurrLocation.clear();
        String time = String.valueOf(new Date().getTime());
        dbRef.child("Locations").child(time).setValue(locationData);
        Toast.makeText(BluetoothGPSService.this,
                "Location recorded", Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                Toast.makeText(BluetoothGPSService.this,
                        "Bluetooth device nearby", Toast.LENGTH_SHORT).show();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                devicesInCurrLocation.add(deviceName);

                //check to see if device is new
                boolean newDevice = true;
                for (String devName : deviceList) {
                    if (deviceName.equals(devName)) {
                        newDevice = false;
                    }
                }
                //if new add it to list and database
                if (newDevice) {
                    deviceList.add(deviceName);
                    dbRef.child("Devices").child(deviceAddress).setValue(deviceName);
                    Toast.makeText(BluetoothGPSService.this,
                            "New Bluetooth device detected.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
        handler.removeCallbacks(delayPeriod);
    }
}
