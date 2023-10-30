package com.example.inclui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public String TAG = "-----------";
    private String macAddress = "";
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button BtnConnect;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    int REQUEST_ENABLE_BT;
    Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BtnConnect = (Button) findViewById(R.id.BtnConnect);

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        REQUEST_ENABLE_BT = 100;

        requestBluetoothPermission();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Dispositivo não suporta bluetooth!!", Toast.LENGTH_LONG).show();
        } else {
            tryToConnect();
            connectMethods();
        }
    }

    private void requestPermission(String permission){
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, REQUEST_ENABLE_BT);
        }
    }

    private void requestBluetoothPermission(){
        requestPermission(android.Manifest.permission.BLUETOOTH);
        requestPermission(Manifest.permission.BLUETOOTH_CONNECT);
    }

    ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Bluetooth ativado com sucesso
                }
            }
    );

    private void turnOnBluetooth(){
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtLauncher.launch(enableBtIntent);
            }
        }
    }

    private void tryToConnect(){
        if(bluetoothSocket != null && bluetoothSocket.isConnected()){
            Toast.makeText(getApplicationContext(),"O dispostivo ja esta conectado",Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission();
            return;
        }

        if(!bluetoothAdapter.isEnabled()){
            turnOnBluetooth();
            return;

        }

        pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("HC-06")) {
                macAddress = device.getAddress();
                Toast.makeText(getApplicationContext(), "O dispositivo foi encontrado no historico", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        if(macAddress == ""){
            Toast.makeText(getApplicationContext(),"O dispositivo do inclui++ não foi encontrado no historico de conexões.",Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);

        Intent nextPageIntent = new Intent(MainActivity.this, InfoActivity.class);
        nextPageIntent.putExtra("macAddress", macAddress);
        startActivity(nextPageIntent);

    }

    private void connectMethods(){
        BtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToConnect();
            }
        });
    }
}