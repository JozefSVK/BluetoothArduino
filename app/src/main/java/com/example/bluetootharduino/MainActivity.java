package com.example.bluetootharduino;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    /*  =================================Position of X Y Z =============================*/
    // data of positions
    final static String name = "positions";
    SharedPreferences sp;
    // for seekBars
    final int defaultposition = 25;
    // Variables related to progressBar X
    int tempX = 0;
    int totalX;
    TextView Xview;
    String Xstring = "X" + totalX;

    // Variables related to progressBar Y
    int tempY = 0;
    int totalY;
    TextView Yview;
    String Ystring = "Y" + totalY;

    // Variables related to progressBar Z
    int tempZ = 0;
    int totalZ;
    TextView Zview;
    String Zstring = "Z" + totalZ;

    // Show all positions
    String allPos;
    TextView textPositions;

    SeekBar.OnSeekBarChangeListener mlistener; // for multiple seekbars

    /*  ================================= ACCELEROMETER =============================*/
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    final int accelerometerValue = 1;

    Button buttonAccelerometer;

    Toast toastObject;

    int state = 0;
    int stateX = 0;
    int stateY = 0;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];

            System.out.println("x = " + x + ";   y = " + y);

            showDir(x, y);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

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

        // Buttons
        final Button buttonHome = findViewById(R.id.buttonHome);
        buttonHome.setEnabled(false);
        buttonHome.setOnClickListener(this);
        final Button buttonPause = findViewById(R.id.buttonPause);
        buttonPause.setEnabled(false);
        buttonPause.setOnClickListener(this);
        final Button buttonCommands = findViewById(R.id.buttonCommand);
        buttonCommands.setOnClickListener(this);
        final Button buttonPosition = findViewById(R.id.buttonPosition);
        buttonPosition.setEnabled(false);
        buttonPosition.setOnClickListener(this);

        // Accelerometer
        buttonAccelerometer = findViewById(R.id.buttonAccelerometer);
        buttonAccelerometer.setOnClickListener(this);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // radio group (disable and enable switch)
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        RadioButton buttonDisable = findViewById(R.id.buttonDisable);
        buttonDisable.setEnabled(false);
        buttonDisable.setOnClickListener(this);
        RadioButton buttonEnable = findViewById(R.id.buttonEnable);
        buttonEnable.setEnabled(false);
        buttonEnable.setOnClickListener(this);


        // hide keyboard
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // data on positions
        sp = getSharedPreferences(name, Context.MODE_PRIVATE);

        // X position
        totalX = sp.getInt("Xpos", 0);
        Xstring = "X = " + totalX;
        Xview = findViewById(R.id.Xview);
        Xview.setText(Xstring);
        SeekBar seekBarX = (SeekBar)findViewById(R.id.seekBarX);

        // Y position
        totalY = sp.getInt("Ypos", 0);
        Ystring = "Y = " + totalY;
        Yview = findViewById(R.id.Yview);
        Yview.setText(Ystring);
        SeekBar seekBarY = (SeekBar)findViewById(R.id.seekBarY);

        // Z position
        totalZ = sp.getInt("Zpos", 0);
        Zstring = "Z = " + totalZ;
        Zview = findViewById(R.id.Zview);
        Zview.setText(Zstring);
        SeekBar seekBarZ = (SeekBar)findViewById(R.id.seekBarZ);

        // text to send to arduino and write to app
        allPos = "X" + totalX + "Y" + totalY + "Z" + totalZ + "#";
        textPositions = (TextView)findViewById(R.id.textPositions);
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
                                buttonPosition.setEnabled(true);
                                buttonPause.setEnabled(true);
                                buttonHome.setEnabled(true);
                                buttonDisable.setEnabled(true);
                                buttonEnable.setEnabled(true);
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
                        textViewInfo.setText("Arduino Message : " + arduinoMsg);
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

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // seekbars
                        if (tempX != 0)
                        {
                            totalX += tempX;
                            changeText('X');
                            System.out.println("Timer = " + totalX);
                        }
                        if (tempY != 0)
                        {
                            totalY += tempY;
                            changeText('Y');
                            System.out.println("Timer = " + totalY);
                        }
                        if (tempZ != 0)
                        {
                            totalZ += tempZ;
                            changeText('Z');
                            System.out.println("Timer = " + totalZ);
                        }

                        // Accelerometer
                        // right(-1) and left(1) direction
                        if (stateX == 1){
                            totalX -= accelerometerValue;
                            changeText('X');
                            //textToSend();
                        } else if (stateX == -1){
                            totalX += accelerometerValue;
                            changeText('X');
                            //textToSend();
                        }
                        // Forward(-1) and backward(1) direction
                        if (stateY == 1){
                            totalY -= accelerometerValue;
                            changeText('Y');
                            //textToSend();
                        } else if (stateY == -1){
                            totalY += accelerometerValue;
                            changeText('Y');
                            //textToSend();
                        }
                    }
                });
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, 50L,100L);

        mlistener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (seekBar.getId()) {
                    case R.id.seekBarX:
                        tempX = progress - defaultposition;
                        break;
                    case R.id.seekBarY:
                        tempY = progress - defaultposition;
                        break;

                    case R.id.seekBarZ:
                        tempZ = progress - defaultposition;
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
                        tempX = 0;
                        Xstring = "X" + totalX;
                        seekBarX.setProgress(defaultposition);
                        break;
                    case R.id.seekBarY:
                        tempY = 0;
                        seekBarY.setProgress(defaultposition);
                        break;

                    case R.id.seekBarZ:
                        tempZ = 0;
                        seekBarZ.setProgress(defaultposition);
                        break;
                }
            }
        };

        seekBarX.setOnSeekBarChangeListener(mlistener);
        seekBarY.setOnSeekBarChangeListener(mlistener);
        seekBarZ.setOnSeekBarChangeListener(mlistener);
    }

    // For buttons
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonHome:
                // X value reset
                totalX = 0;
                // Y value reset
                totalY = 0;
                // Z value reset
                totalZ = 0;

                changeText('A');

                // store value of positions for startup.
                UpdateData();

                // Text which to send
                textToSend("h");
                showPopUp("Homed");
                break;

            case R.id.buttonPause:
                // Text which to sent to arduino
                textToSend("p");
                showPopUp("Paused");
                break;

            case R.id.buttonDisable:
                textToSend("]");
                showPopUp("Steppers Disabled");
                break;

            case R.id.buttonEnable:
                textToSend("[");
                showPopUp("Steppers Enabled");
                break;

            case R.id.buttonCommand:
                // opens activity with list of commands
                Intent intent = new Intent(this, ListOfCommands.class);
                startActivity(intent);
                break;

            case R.id.buttonPosition:
                // store value of positions for startup.
                UpdateData();

                // Text which to send
                textToSend(allPos);
                showPopUp("Sent");
                break;

            case R.id.buttonAccelerometer:
                if (state == 0){
                    buttonAccelerometer.setTextColor(Color.GREEN);
                    state = 1;
                    mSensorManager.registerListener(sensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }
                else if (state == 1){
                    buttonAccelerometer.setTextColor(Color.WHITE);
                    stateToZero('a');
                    state = 0;
                    mSensorManager.unregisterListener(sensorEventListener);
                }
                break;

            default:
                break;

        }

    }

    //
    public void showDir(float x, float y){
        // left and right
        if (Math.abs(x) > 3){
            // if LEFT
            if(x > 3){
                System.out.println("x = " + x);
                if (stateX <= 0){
                    stateToZero('x');
                    stateX ++;
                    showPopUp("Left");
                }
            }
            // if RIGHT
            else if (x < -3){
                System.out.println("x = " + x);
                if (stateX >= 0){
                    stateToZero('x');
                    stateX --;
                    showPopUp("Right");
                }
            }
        }
        // forward and backward
        else if (Math.abs(y) > 3) {
            // if BACKWARD
            if (y > 3) {
                System.out.println("y = " + y);
                if (stateY <= 0) {
                    stateToZero('y');
                    stateY++;
                    showPopUp("Backward");
                }
            }
            // If FORWARD
            else if (y < -3) {
                System.out.println("y = " + y);
                if (stateY >= 0) {
                    stateToZero('y');
                    stateY--;
                    showPopUp("Forward");
                }
            }
        }

        // if no tilt
        else if ((stateX != 0) || (stateY != 0)){
            stateToZero('a');
            showPopUp("Stop");
        }
    }

    public void stateToZero(char c){
        if (c == 'x'){
            stateY = 0;
        }
        else if (c == 'y'){
            stateX = 0;
        }
        if (c == 'a'){
            stateX = 0;
            stateY = 0;
        }
    }

    // Save position of robot
    public void UpdateData(){
        SharedPreferences sp = getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("Xpos", totalX);
        editor.putInt("Ypos", totalY);
        editor.putInt("Zpos", totalZ);
        editor.apply();
    }

    public void changeText(char c){
        // X text view
        if (c == 'X'){
            Xstring = "X = " + totalX;
            Xview.setText(Xstring);
        }
        // Y text view
        else if (c == 'Y'){
            Ystring = "Y = " + totalY;
            Yview.setText(Ystring);
        }
        // Z text view
        else if (c =='Z'){
            Zstring = "Z = " + totalZ;
            Zview.setText(Zstring);
        }
        else if (c == 'A'){
            // X
            Xstring = "X = " + totalX;
            Xview.setText(Xstring);
            // Y
            Ystring = "Y = " + totalY;
            Yview.setText(Ystring);
            // Z
            Zstring = "Z = " + totalZ;
            Zview.setText(Zstring);
        }
        // refresh bottom text which shows positions of axes
        allPos = "X" + totalX + "Y" + totalY + "Z" + totalZ + "#";
        textPositions.setText(allPos);
    }

    public void showPopUp(String text){
        if (toastObject != null){
            toastObject.cancel();
        }
        toastObject = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toastObject.show();
    }

    // text to arduino
    public void textToSend(String s){
        String cmdText;
        cmdText = s;
        connectedThread.write(cmdText);
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