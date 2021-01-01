package com.jnu.smartbandtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    static private String TAG = "SmartBandTest";
    private int REQUEST_ENABLE_BT;
    private ArrayAdapter<String> DeviceListViewList;
    private Set<BluetoothDevice> pairedDeviceList = new ArraySet<BluetoothDevice>();
    private Set<BluetoothDevice> newDeviceList = new ArraySet<BluetoothDevice>();
    private ListView deviceListView;

    private boolean isDiscovery =  false;
    private final int PERMISSION_REQUEST_COARSE_LOCATION = 0xb01;


    private Button scanButton;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = (ListView) findViewById(R.id.DeviceList);
        scanButton = (Button) findViewById(R.id.Scan);
        Init();
    }

    private void Init()
    {
        // set the item click callback function
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // judge which one is clicked
                if ( position < pairedDeviceList.size() )
                {
                    String deviceName = parent.getItemAtPosition(position).toString();
                    Log.i(TAG, "selected device belongs the paired devices, {position}: "+ position + ", name: " + deviceName);
                    BluetoothDevice device = findDevice(pairedDeviceList, deviceName);
                    if ( device != null )
                    {
                        Log.i(TAG, "Name: " + device.getName() + ", MAC: " + device.getAddress());
                    }
                }
                else
                {
                    
                    Log.i(TAG, "selected device belongs the unpaired devices");
                }
            }
        });

        // request the permission when you need to discovery new devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED  ) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            if ( this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBluetoothAdapter == null )
        {
            Log.e(TAG, "Could not get the default bluetooth adapter!");
            // TODO
            // disable the scan button or other functions
            return;
        }

        if ( !mBluetoothAdapter.isEnabled() )
        {
            REQUEST_ENABLE_BT = 1;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            if ( REQUEST_ENABLE_BT == RESULT_OK )
            {
                Log.i(TAG, "enabled bluetooth!");
            }
        }

        List<String> pairedDevicesName = new ArrayList<String>();
        // check if the paired devices have the one we need
        pairedDeviceList = mBluetoothAdapter.getBondedDevices();
        if ( pairedDeviceList.size() > 0 )
        {
            for ( BluetoothDevice device : pairedDeviceList ) {
                pairedDevicesName.add(device.getName());
            }
            DeviceListViewList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pairedDevicesName);
            deviceListView.setAdapter(DeviceListViewList);
            Log.i(TAG, "The size of paired devices is: " + pairedDeviceList.size());
        }

        // discover new devices
        scanButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                if ( isDiscovery == false )
                {
                    boolean status = mBluetoothAdapter.startDiscovery();
                    if ( status == false )
                    {
                        Log.v(TAG, "startDiscovery failed!");
                    }
                    isDiscovery = true;
                    scanButton.setText("Scanning");
                }
                else
                {
                    mBluetoothAdapter.cancelDiscovery();
                    isDiscovery = false;
                    scanButton.setText("Scan");
                }

            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    // create a intent to discovery new device
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if ( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                Log.v(TAG, "start discovery!");
            }

            if ( BluetoothDevice.ACTION_FOUND.equals(action) )
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ( device.getName() != null ) {
                    DeviceListViewList.add(device.getName());
                    DeviceListViewList.notifyDataSetChanged();
                    newDeviceList.add(device);
                    Log.v(TAG, "found device: " + device.getName());
                }
            }
        }
    };


    @Override
    public void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }


    private final BluetoothDevice findDevice(Set<BluetoothDevice> set, String name)
    {
        for ( BluetoothDevice device : set )
        {
            if (device.getName().equals(name) )
            {
                return device;
            }
        }
        return null;
    }
}