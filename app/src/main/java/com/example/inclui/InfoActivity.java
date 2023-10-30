package com.example.inclui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class InfoActivity extends AppCompatActivity {


    public String TAG = "-----------";
    private String macAddress = "";
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    TextView TrackedObstacleText, DistanceText;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    InputStream inputStream;
    ToneGenerator toneGenerator;
    Gson gson;

    boolean timerWasAssigned = false;

    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 40);
        handler = new Handler();

        gson = new Gson();

        TrackedObstacleText = (TextView) findViewById(R.id.trackedObstacleText);
        DistanceText = (TextView) findViewById(R.id.distanceText);
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        Intent intent = getIntent();
        if(intent != null){
            macAddress = intent.getStringExtra("macAddress");
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
            connectToDevice();
        }
    }

    private void connectToDevice(){
        new Thread(new Runnable() {
            @Override
            public void run() {


                if (ActivityCompat.checkSelfPermission(InfoActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT > 31) {
                        ActivityCompat.requestPermissions(InfoActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    }
                }

                int counter = 0;
                do{
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                    }catch(IOException err){
                        err.printStackTrace();
                    }
                    counter++;
                }while (!bluetoothSocket.isConnected() && counter < 0);
                if(!bluetoothSocket.isConnected()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InfoActivity.this, "Não foi possivel se conectar ao dispositivo, tente novamente :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent comeBackToMain = new Intent(InfoActivity.this, MainActivity.class);
                    startActivity(comeBackToMain);
                }
                try {
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    Log.d("Message", "Connected to HC-06");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InfoActivity.this, "Bluetooth successfully connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                    readBluetoothDeviceInfo();

                } catch (IOException e) {
                    Log.d("Message", "Turn on bluetooth and restart the app");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InfoActivity.this, "Turn on bluetooth and restart the app", Toast.LENGTH_SHORT).show();
                        }
                    });
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void readBluetoothDeviceInfo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line = reader.readLine();
                    Log.e(TAG,line);
                    while (line != null) {
                        if (!line.isEmpty()) {
                            try {
                                DadosClass dadosClass = gson.fromJson(line, DadosClass.class);
                                int potenciomerterValue = dadosClass.getValue();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TrackedObstacleText.setText("VERDADEIRO");
                                        DistanceText.setText(potenciomerterValue + " cm");
                                    }
                                });
                                int interval = potenciomerterValue * 2 + 30;
                                if(!timerWasAssigned) {
                                    timerWasAssigned = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(interval);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            simulateObjectDetection();
                                            timerWasAssigned = false;
                                        }
                                    }).start();
                                }

                            }catch (JsonSyntaxException err){
                                Log.e(TAG,"Recebeu uma informação que não é JSON",err);
                            }
                            catch(Exception err){
                                Log.e(TAG,"Erro ",err);
                            }

                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TrackedObstacleText.setText("FALSO");
                                }
                            });

                        }
                        line = reader.readLine();
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    bluetoothSocket = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InfoActivity.this, "Dispositivo foi desconectado :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent backToMainIntent = new Intent(InfoActivity.this,MainActivity.class);
                    startActivity(backToMainIntent);
                } catch (Exception err){
                    Log.d(TAG, "Input stream was disconnected");
                }
            }
        }).start();
    }


    public class DadosClass{
        @SerializedName("sensor")
        private String sensor;
        @SerializedName("value")
        private int value;

        public DadosClass(String sensor, int value){
            this.setValue(value);
            this.setSensor(sensor);
        }

        public DadosClass(){}

        @NonNull
        @Override
        public String toString() {
            return "Sensor : " + this.getSensor() + "; Valor : " + this.getValue() ;
        }

        public void setSensor(String sensor){
            this.sensor = sensor;
        }
        public String getSensor(){
            return this.sensor;
        }
        public void setValue(int value){
            this.value = value;
        }
        public int getValue(){
            return this.value;
        }
    }

    private void simulateObjectDetection() {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 30);

    }
}

