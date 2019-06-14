/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mdm.bt_control;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.mdm.bt_control.Util.AppDebugLog;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import static com.mdm.bt_control.SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED = "com.mdm.bt_control.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.mdm.bt_control.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.mdm.bt_control.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.mdm.bt_control.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.mdm.bt_control.EXTRA_DATA";
    public final static UUID UUID_RING = UUID.fromString(SampleGattAttributes.RING);
    public final static UUID UUID_CREDIT = UUID.fromString(SampleGattAttributes.CREDIT);
    public final static UUID UUID_WRITE_TEXT = UUID.fromString(SampleGattAttributes.WRITE_TEXT);
    public final static UUID UUID_READ_TEXT = UUID.fromString(SampleGattAttributes.READ_TEXT);
    private String resultStr = "";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdateString(intentAction, "");
                AppDebugLog.d("myLogs", "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                AppDebugLog.d("myLogs", "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                AppDebugLog.d("myLogs", "Disconnected from GATT server.");
                broadcastUpdateString(intentAction, "");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdateString(ACTION_GATT_SERVICES_DISCOVERED, "");
                AppDebugLog.d("myLogs", "onServicesDiscovered !!! " );
            } else {
                AppDebugLog.d("myLogs", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            AppDebugLog.d("myLogs", "onCharacteristicRead !!! " );
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdateCharacteristic(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            AppDebugLog.d("myLogs", "onCharacteristicWrite !!! " );
            broadcastUpdateCharacteristic(ACTION_DATA_AVAILABLE, characteristic);
        };


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdateCharacteristic(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            AppDebugLog.d("myLogs", "onDescriptorRead !!! descriptor "+descriptor.getUuid().toString() );
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            AppDebugLog.d("myLogs", String.format("onDescriptorWrite !!! UUID %s, status=%d ", descriptor.getUuid().toString(), status ));
            broadcastUpdateString(ACTION_DATA_AVAILABLE, descriptor.getUuid().toString());
        }
    };

    private void broadcastUpdateString(final String action, final String str) {
        final Intent intent = new Intent(action);
        if (CLIENT_CHARACTERISTIC_CONFIG.equals(str)){
            intent.putExtra(EXTRA_DATA, "#read");
        } else {
            intent.putExtra(EXTRA_DATA, action);
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdateCharacteristic(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        String str = "";
        boolean strEnd = false;
        if (UUID_RING.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, "#ring");
            sendBroadcast(intent);
        } else
        if (UUID_CREDIT.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, "#credit");
            sendBroadcast(intent);
        } else
        if (UUID_WRITE_TEXT.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, "#write");
            sendBroadcast(intent);
        } else{
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    if (byteChar == 0x0D) {strEnd = true; break;}
                }
                str = new String(data);
            }
        }
        resultStr = resultStr.concat(str);
        if(strEnd) {
            intent.putExtra(EXTRA_DATA, resultStr);
            sendBroadcast(intent);
            resultStr = "";
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        AppDebugLog.d("myLogs","BluetoothLeService initialize");
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                AppDebugLog.d("myLogs", "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            AppDebugLog.d("myLogs", "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            AppDebugLog.d("myLogs", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            AppDebugLog.d("myLogs", "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            AppDebugLog.d("myLogs", "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        AppDebugLog.d("myLogs", "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            AppDebugLog.d("myLogs", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) { return; }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            AppDebugLog.d("myLogs", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            AppDebugLog.d("myLogs", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (!enabled) return;;
        if (UUID_RING.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        if (UUID_READ_TEXT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }
}
