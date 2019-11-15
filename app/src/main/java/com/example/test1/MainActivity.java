package com.example.test1;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.BatteryManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.telephony.TelephonyManager;
import android.os.Environment;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    //Variable for data and status display
    public float data1 = 0;
    public float data2 = 0;
    public float data3 = 0;
    public TextView dataDisplay1;
    public TextView dataDisplay2;
    public TextView dataDisplay3;
    public TextView bluetoothStatus;

    //Accelerometer Sensor
    public Sensor accelerometer;
    public SensorManager accelerometerManager;

    //Bluetooth and list of connected device
    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> devices;

    //Use SharedPreference to store data
    SharedPreferences data;
    private SharedPreferences.Editor editor;
    File storageData;
    FileWriter fw;

    //Monitor battery status
    Intent batteryStatus;
    int status;
    boolean isCharging;

    String SerialNumber = android.os.Build.SERIAL;
    UUID u = UUID.fromString("FFC43281-53C2-11CB-A6B4-8FEF5A6DDAB8");

    //monitor is used for updating battery charging status
    private BroadcastReceiver monitor = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        }
    };

    //testTimer run data collection in 20Hz
    public class testTimer {
        Timer timer;
        int time = 0;
        public testTimer() {
            timer = new Timer();
            timer.schedule(new RemindTask(), 20, 50);
        }
        class RemindTask extends TimerTask {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        devices = bluetoothAdapter.getBondedDevices();
                        bluetoothStatus.setText("Currently connect to " +  devices.size() +" bluetooth devices");
                        if(!isCharging){
                            time ++;
                            if(fw != null)
                                try{
                                    fw.append("X: " + data1 + " Y: " + data2 + " Z: " + data3 + '\n');
                                }
                                catch(IOException e){
                                    System.out.print("no exist");
                                }
                            dataDisplay1.setText("" + data1);
                            dataDisplay2.setText("" + data2);
                            dataDisplay3.setText("" + data3);
                            editor.putFloat("x " + time, data1);
                            editor.putFloat("y " + time, data2);
                            editor.putFloat("z " + time, data3);
                            editor.commit();
                        }
                        else if(isCharging){
                            // This part suppose to be connecting to bluetooth and transfer data, which is not fully accomplished yet.
//                            BluetoothSocket socket = null;
//                            int count = 1;
//                            for(BluetoothDevice d : devices) {
//                                try {
//                                    ParcelUuid[] x = d.getUuids();
//                                    String address = d.getAddress();
//                                    dataDisplay1.setText(address);
//                                    //u = UUID.fromString(address);
//                                    socket = d.createInsecureRfcommSocketToServiceRecord(address);
//                                    bluetoothStatus.setText("Connection Rfcomm created with device No." + count);
//                                    try{
//                                        socket.connect();
//                                        bluetoothStatus.setText("Connection complete with device No," + count);
//                                        break;
//                                    }
//                                    catch(IOException e){
//                                        try {
//                                            socket.close();
//                                            bluetoothStatus.setText("Connection failed, closed success with device No."+ count);
//                                        } catch (IOException closeException) {
//                                            bluetoothStatus.setText("Connection failed, closed fail ");
//                                            System.out.print(e);
//                                        }
//                                    }
//                                } catch (IOException e) {
//                                    System.out.print(e);
//                                }
//                                count++;
//                            }
//
//                                File root = Environment.getExternalStorageDirectory();
//                                File file = new File(root, "shumei.txt");
//                                FileOutputStream output;
//                                try{
//                                    output = new FileOutputStream(file);
//                                }
//                                catch(FileNotFoundException e){
//                                    output = null;
//                                    System.out.print(e);
//                                }
//                                byte[] buffer = new byte[1024];
//                                try {
//                                    output.write(buffer);
//                                } catch (IOException e) {
//                                    System.out.print(e);
//                                }
                            }
                        }
                });
            }
        }
    }

    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, 43);
    }
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Setting file on SD card and on internal storage
        data = getPreferences(Context.MODE_PRIVATE);
        editor = data.edit();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        storageData = new File(path + "/data.txt");
        try{
            storageData.createNewFile();
        }
        catch(IOException e){
            System.out.print("no exist");
        }
        try{
            fw = new FileWriter(storageData, true) ;
        }
        catch(IOException e){
            System.out.print("no exist");
        }

        //Setting Battery monitor
        IntentFilter filtering = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, filtering);
        status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

        //Setting bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothStatus = findViewById(R.id.visualize4);
        devices = bluetoothAdapter.getBondedDevices();
        bluetoothStatus.setText("Currently connect to " +  devices.size() +" bluetooth devices");

        //Setting accelerometer
        accelerometerManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = accelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.registerReceiver(monitor, filtering);
        dataDisplay1 = findViewById(R.id.visualize1);
        dataDisplay2 = findViewById(R.id.visualize2);
        dataDisplay3 = findViewById(R.id.visualize3);
        new testTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        data1 = event.values[0];
        data2 = event.values[1];
        data3 = event.values[2];
    }
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        accelerometerManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometerManager.unregisterListener(this);
    }
}

