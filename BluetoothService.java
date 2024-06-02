package com.example.forcapstone2;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.UUID;

public class BluetoothService extends Service {

    // 추가===========================================================================================================================================
    private boolean isServiceConnected = false; // 추가된 부분

    public static final String ACTION_GATT_CONNECTED = "com.example.forcapstone2.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.example.forcapstone2.ACTION_GATT_DISCONNECTED";

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private boolean isScanning = false;
    private static final long SCAN_PERIOD = 10000;
    private static final String TARGET_MAC_ADDRESS = "20:33:91:BA:82:3F"; // Your Arduino Nano BLE MAC address

    // 추가=====================================================================================================================
    private String WaterChange = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanLeDevice(true);
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(() -> {
                isScanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                Log.d("BluetoothService", "Stopped scanning");
            }, SCAN_PERIOD);

            isScanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
            Log.d("BluetoothService", "Started scanning");
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d("BluetoothService", "Stopped scanning");
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d("BluetoothService", "Found device: " + device.getAddress());
            if (TARGET_MAC_ADDRESS.equals(device.getAddress())) {
                Log.d("BluetoothService", "Target device found");
                connectToDevice(device);
                scanLeDevice(false);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        Log.d("BluetoothService", "Connecting to device: " + device.getAddress());
        if (ContextCompat.checkSelfPermission(BluetoothService.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device.connectGatt(BluetoothService.this, false, gattCallback);
        } else {
            showToast("Bluetooth Connect permission is required to connect to device");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("BluetoothService", "Connected to GATT server.");
                bluetoothGatt.discoverServices();
                showToast("Connected to " + gatt.getDevice().getName());
                broadcastUpdate(ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("BluetoothService", "Disconnected from GATT server.");
                showToast("Disconnected");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            } else {
                Log.w("BluetoothService", "Connection state changed to " + newState + " with status " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothService", "Services discovered.");
            } else {
                Log.w("BluetoothService", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Handle characteristic read
            }
        }

        // 추가=================================================================================================================
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Characteristic 값이 변경될 때 실행되는 코드
            byte[] data = characteristic.getValue();
            WaterChange = new String(data); // 바이트 데이터를 문자열로 변환하여 WaterChange에 저장
            Log.d("BluetoothService", "Received data: " + WaterChange);
        }
    };

    // 추가==============================================================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceConnected = true; // 추가된 부분
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceConnected = false; // 추가================================================================================================
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    // 추가==========================================================================================================================================
    public boolean isServiceConnected() {
        return isServiceConnected;
    }

    private void showToast(final String message) {
        handler.post(() -> Toast.makeText(BluetoothService.this, message, Toast.LENGTH_SHORT).show());
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // 추가=========================================================================================================================
    // 기기가 연결되어 있는지 확인하는 메서드
    public boolean isConnected() {
        return bluetoothGatt != null && bluetoothGatt.getConnectionState(bluetoothGatt.getDevice()) == BluetoothGatt.STATE_CONNECTED;
    }

    // 연결된 기기로 데이터를 전송하는 메서드================================================================================================
    @SuppressLint("MissingPermission")
    public void sendData(String data) {
        if (isConnected()) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // 서비스의 UUID를 사용하여 서비스를 가져옵니다.
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // characteristic의 UUID를 사용하여 characteristic을 가져옵니다.
                if (characteristic != null) {
                    characteristic.setValue(data);
                    boolean success = bluetoothGatt.writeCharacteristic(characteristic);
                    if (!success) {
                        Log.e("BluetoothService", "Failed to write characteristic");
                    }
                } else {
                    Log.e("BluetoothService", "Characteristic not found");
                }
            } else {
                Log.e("BluetoothService", "Service not found");
            }
        } else {
            Log.e("BluetoothService", "Not connected to a device");
        }
    }

    // 추가======================================================================================================================
    // WaterChange 값을 가져오는 메서드
    public String getWaterChange() {
        return WaterChange;
    }
}
