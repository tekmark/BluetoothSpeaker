package com.example.chao.bluetoothspeaker;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static public String LOG_TAG="MainActivity";
    //final static public String MY_UUID = "00001108-0000-1000-8000-00805F9B34FB";
    final static public String MY_UUID = "0000110A-0000-1000-8000-00805F9B34FB";
    final static public int REQUEST_ENABLE_BT = 1;
    final static public int MESSAGE_READ = 2;
    final static public String NAME = "Bluetooth Speaker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //setup bluetooth device.
        if (mBluetoothAdapter == null) {    // Device does not support Bluetooth
            Log.w(LOG_TAG, "no Bluetooth device");
        }
        if (!mBluetoothAdapter.isEnabled()) {       //enable bluetooth device
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);        // Don't forget to unregister during onDestroy
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(mReceiver, filter1);


        mBluetoothAdapter.startDiscovery();


        enableBTDiscoverability();

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();


        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(getApplicationContext(), mProfileListener, BluetoothProfile.A2DP);




        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void enableBTDiscoverability() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //setup discoverable duration
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        Log.d(LOG_TAG, "enable discoverability");
        startActivity(discoverableIntent);
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();                     // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d(LOG_TAG, device.getName() + ":" + device.getAddress());
                ParcelUuid uuids[] = device.getUuids();
                device.fetchUuidsWithSdp();
                if (uuids == null) {
                    Log.d(LOG_TAG, "uuids is null");
                    return;
                }
                Log.d(LOG_TAG, "# of supported uuids : " + uuids.length);
                for (ParcelUuid uuid : uuids) {
                    Log.d(LOG_TAG, "uuid : " + uuid.toString());
                }
            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                if(uuidExtra ==  null) {
                    Log.e(LOG_TAG, "UUID = null" + d.getName());
                }

                if(d != null && uuidExtra != null)
                    Log.d(LOG_TAG, d.getName() + ": " + uuidExtra.toString());
            } else {
                Log.d(LOG_TAG, "action is : " + action);
            }
        }
    };

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(LOG_TAG, "HEADSET profile");
                mBluetoothHeadset = (BluetoothHeadset) proxy;
                List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                for(BluetoothDevice device : devices) {
                    Log.d(LOG_TAG, "connected device: " + device.getAddress());
                    if (mBluetoothHeadset.isAudioConnected(device)) {
                        Log.d(LOG_TAG, "audio is connected");
                    } else {
                        Log.d(LOG_TAG, "audio is disconnected");
                    }
                }
            } else if (profile == BluetoothProfile.A2DP) {
                Log.d(LOG_TAG, "A2DP profile");
                mBluetoothA2dp = (BluetoothA2dp) proxy;
                List<BluetoothDevice> devices = mBluetoothA2dp.getConnectedDevices();
                for(BluetoothDevice device : devices) {
                    Log.d(LOG_TAG, "connected device: " + device.getAddress());
                    if (mBluetoothA2dp.isA2dpPlaying(device)) {
                        Log.d(LOG_TAG, "audio is connected");
                    } else {
                        Log.d(LOG_TAG, "audio is disconnected");
                    }
                }
            } else {
                Log.d(LOG_TAG, "Unrecognized profile");
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }
    };
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSock;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            BluetoothServerSocket tmp = null;
            try {
                //MY_UUID is the app's UUID string, also used by the client code           
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                        UUID.fromString(MY_UUID));
            } catch (IOException e) {}
            mmServerSock = tmp;
        }

        public void run() {
            Log.d(LOG_TAG, "start a thread for accepting");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned       
            while (true) {
                try {
                    socket = mmServerSock.accept();
                    Log.d(LOG_TAG, "socket accepted");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "failed to accept a connection");
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        Log.d(LOG_TAG, "socket closed");
                        mmServerSock.close();
                    } catch (IOException e) {
                        Log.d(LOG_TAG, "failed to close a socket");
                    };
                    break;
                }
            }
        }
        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSock.close();
            } catch (IOException e) {

            }
        }
        public void manageConnectedSocket(BluetoothSocket socket) {
            //start a new thread for connection management.
            ConnectedThread connectedThread = new ConnectedThread(socket);
            connectedThread.start();

//            List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
//            for(BluetoothDevice device : devices) {
//                Log.d(LOG_TAG, "connected device: " + device.getAddress());
//                if (mBluetoothHeadset.isAudioConnected(device)) {
//                    Log.d(LOG_TAG, "audio is connected");
//                } else {
//                    Log.d(LOG_TAG, "audio is disconnected");
//                }
//            }
        }
    }
    final private Handler mHandler = new Handler();
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSock;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSock = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.d(LOG_TAG, "start a thread for connection management");
            byte[] buffer = new byte[1024];         // buffer store for the stream
            int bytes;                              // bytes returned from read()       
            // Keep listening to the InputStream until an exception occurs       
            while (true) {
                try {
                // Read from the InputStream               
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSock.close();
            } catch (IOException e) { }
        }
    }
}
