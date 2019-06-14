package com.mdm.bt_control;

import android.Manifest;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mdm.bt_control.Util.AppDebugLog;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.mdm.bt_control.GattProvider.GATT_STATE_CONNECT;
import static com.mdm.bt_control.GattProvider.GATT_STATE_DISCONNECT;
import static com.mdm.bt_control.GattProvider.RECIVE_STRING;
import static com.mdm.bt_control.GattProvider.UDATE_EVENT;
import static com.mdm.bt_control.SampleGattAttributes.CREDIT;
import static com.mdm.bt_control.SampleGattAttributes.READ_TEXT;
import static com.mdm.bt_control.SampleGattAttributes.RING;
import static com.mdm.bt_control.SampleGattAttributes.WRITE_TEXT;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34; // ?
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_TIME = 10000;
    private BluetoothDevice mLeDevice;
    private ProgressBar mProgressBar;
    private ImageView mImageView;
    private TextView mField1TextView, mField2TextView, mDeviceName, mDeviceAddres;
    private Button  connectButton;
    private SeekBar motorSeekBar;
    private SeekBar headSeekBar;
    private SeekBar theadSeekBar;
    private Button  motorButton;
    private Button  headButton;
    private Button  theadButton;
    private Button  chargeButton;
    private boolean mConnected = false;
    private boolean mScanning = false;
    private boolean mFindDevice = false;
    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_MOTOR_BS = "MOTOR_BS";
    public static final String APP_PREFERENCES_HEAD_BS = "HEAD_BS";
    public static final String APP_PREFERENCES_THEAD_BS = "THEAD_BS";
    public static final String OUTPUT_STR_INIT = "T=0C IN=0V 3V3=0V 4V2=0V 5V=0V";

    private GattProvider gattProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        gattProvider = GattProvider.getInstance(getApplicationContext());
        gattProvider.getGattStaus(new GattProvider.onUpdateCallback(){
            @Override
            public void onUpdateGattState(int state) {
                if(GATT_STATE_DISCONNECT == state){
                    scanLeDevice(false);
                } else
                if(GATT_STATE_CONNECT == state){
                    mFindDevice = false;
                    scanLeDevice(true);
                }
                mConnected = gattProvider.mConnected;
                mScanning =  gattProvider.mScanning;
                UpdateControls();
            }
            @Override
            public void onDataAvailable(String txt) {
                SetFieldText(txt);
            }
        });

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.result_fail);

        motorButton = (Button)findViewById(R.id.motorBtn);
        motorButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (v==motorButton){
                    if (gattProvider != null) {
                        gattProvider.WriteCharacteristic(v.getTag().toString());
                    }
                }
            }
        });

        headButton = (Button)findViewById(R.id.headBtn);
        headButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (v==headButton){
                    if (gattProvider != null) {
                        gattProvider.WriteCharacteristic(v.getTag().toString());
                    }
                }
            }
        });

        theadButton = (Button)findViewById(R.id.thedBtn);
        theadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (v==theadButton){
                    if (gattProvider != null) {
                        gattProvider.WriteCharacteristic(v.getTag().toString());
                    }
                }

            }
        });

        chargeButton = (Button)findViewById(R.id.chargeBtn);
        chargeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (v==chargeButton){
                    if (gattProvider != null) {
                        gattProvider.WriteCharacteristic("CHARGE\n");
                    }
                }
            }
        });

        motorSeekBar = (SeekBar)findViewById(R.id.motorSB);
        motorButton.setTag("MOTOR=±0%\n");
        motorButton.setText(String.format("%d%%",0));
        motorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress -= 100;
                motorButton.setText(String.format("%d%%",progress));
                motorButton.setTag(String.format("MOTOR=%d%%\n",progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        headSeekBar = (SeekBar)findViewById(R.id.headSB);
        headButton.setTag("THEAD=0%\n");
        headButton.setText(String.format("%d%%",0));
        headSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                headButton.setText(String.format("%d%%",progress));
                headButton.setTag(String.format("HEAD=%d%%\n",progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        theadSeekBar = (SeekBar)findViewById(R.id.theadSB);
        theadButton.setTag("THEAD=0.00C\n");
        theadButton.setText(String.format("%.1fC",0.0));
        theadSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val_progress = ((double)progress-999)/10;
                theadButton.setText(String.format("%.1fC",val_progress));
                theadButton.setTag(String.format("THEAD=%.1fC\n",val_progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        connectButton = (Button)findViewById(R.id.connectBtn);
        connectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                AppDebugLog.d("myLogs", String.format("onClick %s %s %s",mScanning?"scan=true":"scan=false", mConnected?"connect=true":"connect=false", mFindDevice?"finddev=true":"finddev=false"));
                if (v==connectButton){
                    if((!mConnected)&&(!mScanning)&&(!mFindDevice)) {
                        Connect();
                    } else
                    if((!mConnected)&&(mScanning)&&(!mFindDevice)) {
                        CancelConnect();
                    } else
                    if((!mConnected)&&(mScanning)&&(mFindDevice)) {
                        CancelConnect();
                    }
                    if((mConnected)&&(!mScanning)&&(mFindDevice)) {
                        mConnected = false;
                        mFindDevice = false;
                        mScanning = false;
                        Disconnect();
                    }
                    UpdateControls();
                }
            }
        });
        mDeviceName =    (TextView) findViewById(R.id.nameText);
        mDeviceAddres =  (TextView) findViewById(R.id.addresText);
        mField1TextView = (TextView) findViewById(R.id.fieldText1);
        mField2TextView = (TextView) findViewById(R.id.fieldText2);
        EnabledControls(false);
        SetFieldText(OUTPUT_STR_INIT);
        if (!checkPermissions())requestPermissions();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.BLE_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void Connect(){
        mScanning = true;
        scanLeDevice(true);
    }

    private void Disconnect(){
        if(gattProvider == null) return;
        gattProvider.GattPpoviderDisconnected();
    }

    private void CancelConnect(){
        mScanning = false;
        mFindDevice = false;
        mConnected = false;
        Disconnect();
        scanLeDevice(false);

    }

    private void EnabledControls(boolean enable){
        motorSeekBar.setEnabled(enable);
        motorButton.setEnabled(enable);
        headButton.setEnabled(enable);
        headSeekBar.setEnabled(enable);
        theadButton.setEnabled(enable);
        theadSeekBar.setEnabled(enable);
        chargeButton.setEnabled(enable);
    }

    private void UpdateControls(){
        if ((!mConnected)&&(!mScanning)&&(!mFindDevice)) {
            mDeviceName.setText(R.string.no_bt_device);
            mDeviceAddres.setText("");
            SetFieldText(OUTPUT_STR_INIT);
            EnabledControls(false);
            connectButton.setText(R.string.connect);
        }
        if((!mConnected)&&(mScanning)&&(mFindDevice)){
            connectButton.setText(R.string.read_param);
            EnabledControls(false);
        }
        if((mConnected)&&(!mScanning)&&(mFindDevice)){
            connectButton.setText(R.string.disconnect);
            EnabledControls(true);
        }
        if((!mConnected)&&(mScanning)&&(!mFindDevice)){
            connectButton.setText(R.string.cancel);
            EnabledControls(false);
        }
        mImageView.setImageResource(mConnected?R.drawable.result_ok:R.drawable.result_fail);
        mProgressBar.setVisibility(mScanning?View.VISIBLE:View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        InitSeekBars();
        UpdateControls();
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void  InitSeekBars(){
        SharedPreferences mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        motorSeekBar.setProgress(mSettings.getInt(APP_PREFERENCES_MOTOR_BS,0));
        headSeekBar.setProgress(mSettings.getInt(APP_PREFERENCES_HEAD_BS,0));
        theadSeekBar.setProgress(mSettings.getInt(APP_PREFERENCES_THEAD_BS,0));
    }

    private void saveSetting(){
        SharedPreferences mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(APP_PREFERENCES_MOTOR_BS, motorSeekBar.getProgress())
        .putInt(APP_PREFERENCES_HEAD_BS, headSeekBar.getProgress())
        .putInt(APP_PREFERENCES_THEAD_BS, theadSeekBar.getProgress());
        editor.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Disconnect();
    }

    @Override
    public void onStop() {
        super.onStop();
        saveSetting();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void scanLeDevice(final boolean enable) {
        mImageView.setImageResource(R.drawable.result_fail);
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if((mScanning)&&(!mConnected)&&(!mFindDevice)){
                        Disconnect();
                        Connect();
                    }
                    UpdateControls();
                }
            }, SCAN_TIME);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        UpdateControls();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(("00:80:25:54:F5:94".equals(device.getAddress()) || // my
                               ("00:80:25:D1:55:72".equals(device.getAddress()))) ){
                                mLeDevice = device;
                                mFindDevice = true;
                                scanLeDevice(false);
                                mDeviceName.setText(device.getName());
                                mDeviceAddres.setText(device.getAddress().toString());
                                gattProvider.GattPpoviderConnected(mLeDevice);
                            }
                        }
                    });
                }
            };

    // "T=9.99C IN=99.9V 3V3=99.9V 4V2=99.9V 5V=99.9V"
    private void SetFieldText(String text){
        String txt1 = "";
        String txt2 = "";
        if (text.length() < 5) return;
        String[] separated = text.split(" ");
        if(separated.length == 5){
            txt1 = String.format("%s\n\n%s\n\n%s",separated[0], separated[1], separated[2]);
            txt2 = String.format("%s\n\n%s\n\n",separated[3], separated[4]);
        } else {
            for (int i=0; i<separated.length; i++){
                txt1 = txt1.concat(String.format("%s\n\n",separated[i]));
            }
        }
        mField1TextView.setText(txt1);
        mField2TextView.setText(txt2);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
             int permissionState1 = ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION);
            int permissionState2 = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            int permissionState3 = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE );
            int permissionState4 = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE );
            return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED
                    && permissionState3 == PackageManager.PERMISSION_GRANTED && permissionState4 == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);
        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean shouldProvideRationale4 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_PHONE_STATE);
        if (true/*shouldProvideRationale || shouldProvideRationale2*/ /*|| shouldProvideRationale3 || shouldProvideRationale4 || shouldProvideRationale5*/) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE/*,
							Manifest.permission.CALL_PHONE*/},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

}
