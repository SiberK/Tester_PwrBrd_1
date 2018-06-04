package com.example.andrew.tester_pwrbrd_1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;

import android.view.WindowManager;
import android.widget.TextView;
import com.example.andrew.tester_pwrbrd_1.usbserial.driver.UsbSerialDriver;
import com.example.andrew.tester_pwrbrd_1.usbserial.driver.UsbSerialProber;
import com.example.andrew.tester_pwrbrd_1.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity{
    private static final String                 TAG = "TESTER_MAIN"     ;
    private static final boolean                D = true                ;
    private static MainActivity	                Instance = null			;

    private         UsbManager                  mUsbManager             ;
    private static  UsbSerialDriver             sDriver = null          ;
    private         SerialInputOutputManager    mSerialIoManager        ;
    private final   ExecutorService             mExecutor = Executors.newSingleThreadExecutor();

    private         SharedPreferences           sPref					;
    private         int                         CntBadCRC = 0           ;

    //----------------------------------------------------------------
//    private Handler mHandler = new Handler() {
//        public void handleMessage(android.os.Message msg) { HandleMessage(msg)	;}
//    };
    //----------------------------------------------------------------------
    public static MainActivity	GetInstance() { return Instance	; }
    //------------------------------------------------------------------------
    //===================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)          ;
        Instance = this                                 ;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)    ;

//        sPref = getPreferences(MODE_PRIVATE)            ;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE)    ;
        OnStartDriverUSB()              ;
        if(sDriver != null)  l(sDriver.getClass().getSimpleName()) ;
    }
    //===================================================================
    //===================================================================
    public void onBtnClick(View v){
        Intent intent  ;
        switch(v.getId()){
        }
    }
    //===================================================================
    protected void ParsePack(byte[] BuffPack,int LenPack) {
//        if(LenPack > 5000){
//            SendRequest(com.example.andrew.tester_pwrbrd_1.TPacket.ptDbg1,null)                     ;
//        }
//
        TPacket     Packet = new TPacket(BuffPack,LenPack)  ;
        TTstData    TstData = new TTstData(Packet.bData)    ;

        TextView    Lbl ;
        Lbl = (TextView)findViewById(R.id.lblPCH1_S1)       ; if(Lbl != null) Lbl.setBackgroundColor(TstData.S[0] == 0 ? Color.GREEN : Color.GRAY);
        Lbl = (TextView)findViewById(R.id.lblPCH1_S2)       ; if(Lbl != null) Lbl.setBackgroundColor(TstData.S[1] == 0 ? Color.RED : Color.GRAY);
        Lbl = (TextView)findViewById(R.id.lblPCH2_S1)       ; if(Lbl != null) Lbl.setBackgroundColor(TstData.S[2] == 0 ? Color.GREEN : Color.GRAY);
        Lbl = (TextView)findViewById(R.id.lblPCH2_S2)       ; if(Lbl != null) Lbl.setBackgroundColor(TstData.S[3] == 0 ? Color.RED : Color.GRAY);
        Lbl = (TextView)findViewById(R.id.lblPCH3_S1)       ; if(Lbl != null) Lbl.setBackgroundColor(TstData.S[4] == 0 ? Color.GREEN : Color.GRAY);
        Lbl = (TextView)findViewById(R.id.lblPCH3_AIN)      ; if(Lbl != null) Lbl.setText(TstData.V[0] + " " + TstData.V[3]+" " +TstData.iS)           ;

    }
    //===================================================================
    //===================================================================
//TODO  USB
    //===================================================================
    private final SerialInputOutputManager.Listener mListenerUSB =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) { l("Runner stopped."); }

                //TODO
                @Override
                public void onPackData(final byte[] data,final int Len) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            l("Receive " + data.length + " bytes")   ;
                            MainActivity.this.ParsePack(data,Len)  ;
                        }
                    });
                }

                @Override
                public void onRawData(final byte[] data) {
                }

                @Override
                public void onNewString(final String Str) {
//                    OSC_MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            if(Str.length()>2)
////                                OSC_MainActivity.this.ParseStr(Str);
//                        }
//                    });
                }
            };
    //===================================================================
    protected UsbSerialDriver GetUsbDriver(){
        UsbSerialDriver	 rDriver = null	;

        for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
            final List<UsbSerialDriver> drivers =
                    UsbSerialProber.probeSingleDevice(mUsbManager, device);
            l("Found usb device: " + device);
            if (drivers.isEmpty()) {
                l("  - No UsbSerialDriver available.");
            } else {
                for(UsbSerialDriver driver : drivers) {
                    l("  + " + driver);
                    rDriver = driver	;
                }
            }
        }
        return rDriver	;}
    //===================================================================
    private void OnStartDriverUSB(){
        if (sDriver != null) return     ;
        else  sDriver = GetUsbDriver()	;
        l("Resumed, sDriver=" + sDriver);

        if (sDriver == null) {
            l("No serial device.");
        } else {
            try {
                sDriver.open();
//                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
                sDriver.setParameters(9600, 8, UsbSerialDriver.STOPBITS_2, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                l("Error opening device: " + e.getMessage());
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            l("Serial device: " + sDriver.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }
    //===================================================================
    private void OnStopDriverUSB(){
        stopIoManager();

        if (sDriver != null) {
            try { sDriver.close()       ;}
            catch (IOException e) {// Ignore.
            }
            sDriver = null              ;}
    }
    //===================================================================
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            i("Stopping io manager");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }
    //===================================================================
    private void startIoManager() {
        if (sDriver != null) {
            i("Starting io manager");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListenerUSB);
            mSerialIoManager.TypeData = SerialInputOutputManager.PACK_DATA     ;
            mExecutor.submit(mSerialIoManager);
        }
    }
    //===================================================================
    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
    //===================================================================
    //===================================================================
    //===================================================================
//TODO  onStart, onStop etc
    //===================================================================
    @Override
    public void onStart() {
        super.onStart();
        e("++ ON START ++");
//        OnStartDriverBT();
    }
    //===================================================================
    @Override
    public synchronized void onResume() {
        super.onResume();
        e("+ ON RESUME +")  ;
        OnStartDriverUSB()   ;
//        OnResumeDriverBT()  ;
    }
    //===================================================================
    @Override
    public synchronized void onPause() {
        super.onPause();
        e("-* ON PAUSE *-");
//        OnStopUSBdriver()  ;
    }
    //===================================================================
    @Override
    public void onStop() {
        super.onStop();
        e("-- ON STOP --");
    }
    //===================================================================
    @Override
    public void onDestroy() {
//        sPref = getPreferences(MODE_PRIVATE)		    ;
//        SharedPreferences.Editor ed = sPref.edit()	    ;
//
//        OSC_Settings.PutSettings(sPref);
//        ed.putInt    ("CURSOR"     ,flCursorChange)             ; ed.apply()   ;
//        ed.putInt    ("IX_ATT"     ,IxAtt)                      ; ed.apply()   ;
//        ed.putInt    ("IX_FREQ"    ,IxFreqSmpl)                 ; ed.apply()   ;

        super.onDestroy();

//        OnStopDriverBT()    ;
        OnStopDriverUSB()  ;
//	mScopeView.releaseResources()			;
        e("-- ON DESTROY --");
    }
    //===================================================================
    //===================================================================
    //===================================================================
    public static String IntToHex(long val,int cnt){
        String result = ""  ;
        return result    ;}
    //===================================================================
    public static int StrToInt(String str) {
        int val 	;
        try{val = Integer.parseInt(str)	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    public static float StrToFloat(Editable str) {
        float val 	;
        try{val = Float.parseFloat(String.valueOf(str))	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    public int StrToInt(CharSequence text){ return StrToInt("" + text);}
    //===================================================================
    public int StrToHex(String str) {
        int val = 0	;
        try{val = Integer.parseInt(str,16)	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    private void e(String msg){Log.e(TAG,">=< " + msg + " >=<"); }
    //----------------------------------------------------------------------
    private void l(String msg){Log.d(TAG, ">==< "+msg+" >==<"); }
    //----------------------------------------------------------------------
    private void i(String msg){Log.i(TAG, ">===< "+msg+" >===<"); }
}
