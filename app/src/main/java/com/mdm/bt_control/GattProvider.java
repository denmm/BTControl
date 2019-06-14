package com.mdm.bt_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.mdm.bt_control.Util.AppDebugLog;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.mdm.bt_control.SampleGattAttributes.CREDIT;
import static com.mdm.bt_control.SampleGattAttributes.READ_TEXT;
import static com.mdm.bt_control.SampleGattAttributes.RING;
import static com.mdm.bt_control.SampleGattAttributes.WRITE_TEXT;

public class GattProvider implements GattProviderI{

    private static volatile GattProvider INSTANCE;

    public final static String UDATE_EVENT = "com.mdm.bt_control.UDATE_EVENT";
    public final static String RECIVE_STRING = "com.mdm.bt_control.RECIVE_STRING";
    public final static String PROVIDER_EXTRA_DATA = "com.mdm.bt_control.PROVIDER_EXTRA_DATA";
    public final static int    GATT_STATE_CONNECT    = 1;
    public final static int    GATT_STATE_DISCONNECT = 1;

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private BluetoothDevice mBtDevice;
    private BluetoothLeService mBluetoothLeService;
    ArrayList<BluetoothGattCharacteristic> mGattCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean uartConnect = false;
    private static final int CREDIT_LIMIT = 5;//100;//127;
    private int input_size_data = 0;

    public boolean mConnected = false;
    public boolean mScanning = false;

    private Handler mHandler;
    Runnable runnable_restart;
    private static final long RESTART_PERIOD = 10000;
    private  onUpdateCallback mCallback;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            AppDebugLog.d("myLogs","GATT Provider onServiceConnected Bluetooth");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                AppDebugLog.d("myLogs","GATT Provider  Unable to initialize Bluetooth");
                //finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private GattProvider(Context context){
        mContext = context;
        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mHandler = new Handler();
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public static GattProvider getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (GattProvider.class) {
                if ((INSTANCE == null)&&(context!=null)) {
                    INSTANCE = new GattProvider(context);
                }
            }
        }
        return INSTANCE;
    }

    public void GattPpoviderConnected(BluetoothDevice bt_device){
        mBtDevice = bt_device;
        mDeviceAddress = bt_device.getAddress();
        uartConnect = false;
        ConnectToDevice(bt_device);
    }

    private void ConnectToDevice(BluetoothDevice device){
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(device.getAddress());
            AppDebugLog.d("myLogs", "GATT Provider Connect request result=" + result);
        } else
            AppDebugLog.d("myLogs","GATT Provider ConnectToDevice mBluetoothLeService = null");
    }

    public void GattPpoviderDisconnected(){
        if(mBluetoothLeService == null) {
            return;
        }
        mHandler.removeCallbacks(runnable_restart);
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();

    }


    //T=9.99C IN=99.9V 3V3=99.9V 4V2=99.9V 5V=99.9V
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mCallback.onUpdateGattState(GATT_STATE_CONNECT);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mScanning = false;
                mCallback.onUpdateGattState(GATT_STATE_DISCONNECT);
                mGattCharacteristics.clear();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                AppDebugLog.d("myLogs","GATT Provider ACTION_GATT_SERVICES_DISCOVERED");
                List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
                for (BluetoothGattService gattService : gattServices) {
                    AppDebugLog.d("myLogs","GATT Provider UIID Servises: "+ gattService.getUuid().toString());
                    if ( "0000fefb-0000-1000-8000-00805f9b34fb".equals(gattService.getUuid().toString()) ) {
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            AppDebugLog.d("myLogs","GATT Provider UIID Characteristic: "+ gattCharacteristic.getUuid().toString());
                            mGattCharacteristics.add(gattCharacteristic);
                        }
                    }
                }
                if(gattServices.size()>0){
                    ConnectingToUART();
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String input_val = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if ("#ring".equals(input_val)){
                    AppDebugLog.d("myLogs", String.format("GATT Provider #ring ACTION_DATA_AVAILABLE %s", input_val));
                    ReadFromUART();
                } else
                if("#read".equals(input_val)){
                    AppDebugLog.d("myLogs", String.format("GATT Provider #read ACTION_DATA_AVAILABLE %s", input_val));
                    if (!uartConnect) {
                        SetCreditForReadUART();
                        uartConnect = true;
                        mConnected = true;
                        mScanning = false;
                        input_size_data = 0;
                        restartCredit(true);
                    }

                } else
                if("#credit".equals(input_val)){
                    AppDebugLog.d("myLogs", String.format("GATT Provider #credit ACTION_DATA_AVAILABLE %s", input_val));
                    ReadFromUART();
                }
                else
                if("#write".equals(input_val)){
                    AppDebugLog.d("myLogs", String.format("GATT Provider #write ACTION_DATA_AVAILABLE %s", input_val));
                }
                else{
                    input_size_data = input_size_data + input_val.length();
                    AppDebugLog.d("myLogs", String.format("GATT Provider xxx ACTION_DATA_AVAILABLE %s input_size_data %d", input_val, input_size_data));
                    mCallback.onDataAvailable(input_val);
                    SetCreditForReadUART();
                    restartCredit(true);
                }
            }
        }
    };

    private void ConnectingToUART(){
        for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
            if (RING.equals(gattCharacteristic.getUuid().toString())){
                mNotifyCharacteristic = gattCharacteristic;
                AppDebugLog.d("myLogs","GATT Provider ConnectingToUART "+ mNotifyCharacteristic.getUuid().toString());
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, true);

                break;
            }
        }
    }

    private void ReadFromUART(){
        mBluetoothLeService.setCharacteristicNotification(
                mNotifyCharacteristic, false);
        for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
            if (READ_TEXT.equals(gattCharacteristic.getUuid().toString())){
                mNotifyCharacteristic = gattCharacteristic;
                AppDebugLog.d("myLogs","GATT Provider ConnectingToUART "+ mNotifyCharacteristic.getUuid().toString());
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, true);

                break;
            }
        }
    }

    private void SetCreditForReadUART(){
        for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
            if (CREDIT.equals(gattCharacteristic.getUuid().toString())){
                //mNotifyCharacteristic = gattCharacteristic;
                AppDebugLog.d("myLogs","GATT Provider ConnectingToUART "+ gattCharacteristic.getUuid().toString());
                byte[] data_to_write = {CREDIT_LIMIT};//{127};//{0x7f};
                gattCharacteristic.setValue(data_to_write);

                mBluetoothLeService.setCharacteristic(gattCharacteristic);

                break;
            }
        }
    }

    public void WriteCharacteristic(String text){
        //if(!mConnected) return;
        for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
            if (WRITE_TEXT.equals(gattCharacteristic.getUuid().toString())){
                //mNotifyCharacteristic = gattCharacteristic;
                AppDebugLog.d("myLogs","GATT Provider WriteCharacteristic "+ gattCharacteristic.getUuid().toString());

                byte[] data_to_write;
                try {
                    data_to_write = text.getBytes("UTF-8");
                    gattCharacteristic.setValue(data_to_write);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.setCharacteristic(gattCharacteristic);

                break;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void restartCredit(final boolean enable) {
        AppDebugLog.d("myLogs", "GATT Provider reset restartCredit");
        if (enable) {
            mHandler.removeCallbacks(runnable_restart);
            runnable_restart = new Runnable() {
                @Override
                public void run() {
                    AppDebugLog.d("myLogs", "GATT Provider  restartCredit");
                    SetCreditForReadUART();
                }
            };
            mHandler = null;
            mHandler = new Handler();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(runnable_restart, RESTART_PERIOD);

        } else {

            AppDebugLog.d("myLogs", "stopLeScan");
        }
    }

    @Override
    public void getGattStaus(@NonNull onUpdateCallback callback) {
        mCallback = callback;
    }
}
