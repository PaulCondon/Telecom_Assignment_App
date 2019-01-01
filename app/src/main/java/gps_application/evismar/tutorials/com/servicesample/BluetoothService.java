package gps_application.evismar.tutorials.com.servicesample;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BluetoothService extends Service {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private static int i = 0;
    private int fifteenMinutes = 1000; //900000;
    private ArrayList<String> deviceList;
    private Handler handler;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(BluetoothService.this,"Bluetooth Service Started", Toast.LENGTH_SHORT).show();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceList = new ArrayList<>();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();
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
                Toast.makeText(BluetoothService.this,"Found a Device", Toast.LENGTH_SHORT).show();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceNumber = "device" + i++;
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
                    dbRef.child("Devices").child(deviceNumber).setValue(deviceName);
                }
            }
        }
    };

    public void searchBluetoothDevices(){
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
}
