package com.mdm.bt_control;

import android.support.annotation.NonNull;

public interface GattProviderI {

    interface onUpdateCallback {

        void onUpdateGattState(int state);

        void onDataAvailable(String txt);
    }
    void getGattStaus( @NonNull onUpdateCallback callback);

}
