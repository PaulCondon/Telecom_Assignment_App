package gps_application.evismar.tutorials.com.servicesample;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


public class BluetoothGPSService extends Service implements LocationListener {

    private BluetoothManager bluetoothManager;
    private LocationManager locationManager;
    private BluetoothAdapter bluetoothAdapter;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private int fifteenMinutes = 10000; //900000;
    private ArrayList<String> deviceList;
    private ArrayList<String> devicesInCurrLocation;
    private Handler handler;

    public BluetoothGPSService(){

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate() {

        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, fifteenMinutes,
                    100, this);
        }
        catch (SecurityException e) {
            e.getMessage();
        }
        catch (Exception e) {
            e.getMessage();
        }


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();

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
                searchBluetoothDevices();

            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                Toast.makeText(BluetoothGPSService.this,
                        "Detected a nearby Bluetooth device.", Toast.LENGTH_SHORT).show();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                //check to see if device is new
                boolean newDevice = true;
                for(String devName : deviceList){
                    if(deviceName.equals(devName)){
                        newDevice = false;
                    }
                }
                //if new add it to list and database
                if(newDevice){
                    deviceList.add(deviceName);
                    dbRef.child("Devices").child(deviceAddress).setValue(deviceName);
                    Toast.makeText(BluetoothGPSService.this,
                            "New Bluetooth device recorded.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    public void onLocationChanged(Location location) {

        //App 2  todo: upload location to Firebase
        LocationData locationData = new LocationData(location.getLatitude(),location.getLongitude(),devicesInCurrLocation);

        dbRef.child("Locations").push().setValue(locationData);
        Toast.makeText(BluetoothGPSService.this,
                "New location recorded.", Toast.LENGTH_SHORT).show();
    }

    public void searchBluetoothDevices(){
        bluetoothAdapter.startDiscovery();
    }


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
        unregisterReceiver(mReceiver);
    }
}
