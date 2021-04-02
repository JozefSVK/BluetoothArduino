package com.example.bluetootharduino;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    // data of positions
    final static String name = "positions";
    SharedPreferences sp;
    // for seekBars
    final int defaultposition = 25;
    int previousPosition = - defaultposition;
    // Variables related to progressBar X
    int tempX;
    int totalX;
    String Xstring = "X" + totalX;

    // Variables related to progressBar Y
    int tempY;
    int totalY;
    String Ystring = "Y" + totalY;

    // Variables related to progressBar Z
    int tempZ;
    int totalZ;
    String Zstring = "Z" + totalZ;

    // Show all positions
    String allPos;

    SeekBar.OnSeekBarChangeListener mlistener; // for multiple seekbars

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);

        final Button buttonSend = findViewById(R.id.buttonSend);
        buttonSend.setEnabled(false);
        buttonSend.setOnClickListener(this);
        final Button buttonCommands = findViewById(R.id.buttonCommand);
        buttonCommands.setOnClickListener(this);
        final Button buttonPosition = findViewById(R.id.buttonPosition);
        buttonPosition.setEnabled(false);
        buttonPosition.setOnClickListener(this);
        final TextView textPositions = (TextView)findViewById(R.id.textPositions);

        // hide keyboard
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // data on positions
        sp = getSharedPreferences(name, Context.MODE_PRIVATE);

        // X position
        totalX = sp.getInt("Xpos", 0);
        Xstring = "X" + totalX;
        TextView Xview = findViewById(R.id.Xview);
        Xview.setText(Xstring);
        SeekBar seekBarX = (SeekBar)findViewById(R.id.seekBarX);

        // Y position
        totalY = sp.getInt("Ypos", 0);
        Ystring = "Y" + totalY;
        TextView Yview = findViewById(R.id.Yview);
        Yview.setText(Ystring);
        SeekBar seekBarY = (SeekBar)findViewById(R.id.seekBarY);

        // Z position
        totalZ = sp.getInt("Zpos", 0);
        Zstring = "Z" + totalZ;
        TextView Zview = findViewById(R.id.Zview);
        Zview.setText(Zstring);
        SeekBar seekBarZ = (SeekBar)findViewById(R.id.seekBarZ);

        allPos = "X" + String.valueOf(totalX) + "Y" + String.valueOf(totalY) + "Z" +
                String.valueOf(totalZ) + "#";
        textPositions.setText(allPos);

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonSend.setEnabled(true);
                                buttonPosition.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });
/* ============================================== MY CODE ============================================*/
        mlistener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (seekBar.getId()) {
                    case R.id.seekBarX:
                        tempX += progress - previousPosition;
                        previousPosition = progress;
                        if(progress == 50){
                            tempX += 1;
                            Xstring = "X = " + totalX + " + " + tempX;
                        }
                        else if(progress == 0){
                            tempX -= 1;
                        }
                        Xstring = "X = " + totalX + " + " + tempX;
                        Xview.setText(Xstring);
                        break;
                    case R.id.seekBarY:
                        tempY = progress - defaultposition;
                        Ystring = "Y = " + totalY + " + " + tempY;
                        Yview.setText(Ystring);
                        break;

                    case R.id.seekBarZ:
                        tempZ = progress - defaultposition;
                        Zstring = "Z = " + totalZ + " + " + tempZ;
                        Zview.setText(Zstring);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                switch (seekBar.getId()) {
                    case R.id.seekBarX:
                        totalX += tempX;
                        tempX = 0;
                        Xstring = "X" + totalX;
                        previousPosition = - defaultposition;
                        seekBarX.setProgress(defaultposition);
                        break;
                    case R.id.seekBarY:
                        totalY += tempY;
                        seekBarY.setProgress(defaultposition);
                        break;

                    case R.id.seekBarZ:
                        totalZ += tempZ;
                        seekBarZ.setProgress(defaultposition);
                        break;
                }
                allPos = "X" + String.valueOf(totalX) + "Y" + String.valueOf(totalY) + "Z" + String.valueOf(totalZ) + "#";
                textPositions.setText(allPos);
            }
        };

        seekBarX.setOnSeekBarChangeListener(mlistener);
        seekBarY.setOnSeekBarChangeListener(mlistener);
        seekBarZ.setOnSeekBarChangeListener(mlistener);
    }

    @Override
    public void onClick(View v) {
        String cmdText = null;
        switch (v.getId()){

            case R.id.buttonSend:
                EditText text = ((EditText)findViewById(R.id.TextCommand)); // find text by id
                cmdText = text.getText().toString().toUpperCase(); // turn to uppercase
                connectedThread.write(cmdText); // send text
                text.setText(""); // remove text from the textView
                break;

            case R.id.buttonCommand:
                // opens activity with list of commands
                Intent intent = new Intent(this, ListOfCommands.class);
                startActivity(intent);
                break;

            case R.id.buttonPosition:
                SharedPreferences sp = getSharedPreferences(name, Context.MODE_PRIVATE);

                // store value of positions for startup.
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("Xpos", totalX);
                editor.putInt("Ypos", totalY);
                editor.putInt("Zpos", totalZ);
                editor.apply();

                // Text which to send
                cmdText = allPos;
                connectedThread.write(cmdText);
                break;
            default:
                break;

        }

    }
/* ============================================ END OF MY CODE ===================================================== */

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}