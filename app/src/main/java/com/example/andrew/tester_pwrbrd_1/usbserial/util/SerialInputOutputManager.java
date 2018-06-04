/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.example.andrew.tester_pwrbrd_1.usbserial.util;

import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.example.andrew.tester_pwrbrd_1.usbserial.driver.UsbSerialDriver;

import java.io.IOException;
import java.nio.ByteBuffer;


import java.nio.ByteOrder;

/**
 * Utility class which services a {@link UsbSerialDriver} in its {@link #run()}
 * method.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialInputOutputManager implements Runnable {

    private static final String TAG = SerialInputOutputManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int READ_WAIT_MILLIS = 200;
    private static final int BUFSIZ = 4096;
    private static final int STRSIZ = 80;
    public static final int  RAW_DATA  = 0           ;
    public static final int  STR_DATA  = 1           ;
    public static final int  PACK_DATA = 2           ;

    private final UsbSerialDriver mDriver;

    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);
//    private final ByteBuffer mStrBuffer = ByteBuffer.allocate(BUFSIZ);

    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);
    private final byte[] StrData = new byte[STRSIZ]     ;
    private         int  IxStrData = 0                  ;
    public int              TypeData  = RAW_DATA    ;
    public int              SignPack  = 0           ;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    // Synchronized by 'this'
    private State mState = State.STOPPED;

    // Synchronized by 'this'
    private Listener mListener;

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        public void onPackData(byte[] data, int Len);
        public void onRawData(byte[] data);
        public void onNewString(String Str);

        /**
         * Called when {@link SerialInputOutputManager#run()} aborts due to an
         * error.
         */
        public void onRunError(Exception e);
    }

    /**
     * Creates a new instance with no listener.
     */
    public SerialInputOutputManager(UsbSerialDriver driver) {
        this(driver, null);
    }

    /**
     * Creates a new instance with the provided listener.
     */
    public SerialInputOutputManager(UsbSerialDriver driver, Listener listener) {
        mDriver = driver;
        mListener = listener;
    }

    public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

    public void writeAsync(byte[] data) {
        synchronized (mWriteBuffer) {
            mWriteBuffer.put(data);
        }
    }

    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    private synchronized State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     *
     * NOTE(mikey): Uses inefficient read/write-with-timeout.
     * TODO(mikey): Read asynchronously with {@link UsbRequest#queue(ByteBuffer, int)}
     */
    @Override
    public void run() {
        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running.");
            }
            mState = State.RUNNING;
        }

        Log.i(TAG, "Running ..");
        try {
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step();
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final Listener listener = getListener();
            if (listener != null) {
              listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = State.STOPPED;
                Log.i(TAG, "Stopped.");
            }
        }
    }
    //------------------------------------------------------------------------
    private final static int  LenBFpack = 60000             ;
    private byte[]			BuffPack  = new byte[LenBFpack]	;
    private	byte[]			BuffPack2 = new byte[LenBFpack]	;
    private int				IxPack=0,LenPack=0	            ;
    private	long			mTime0, mTime1					;
    //------------------------------------------------------------------------
    protected void ParsePack(byte[] readBuf,int Count,Listener listener) {
        int		ix			;

        for(ix=0;ix<Count;ix++){
            if(IxPack==0){
                BuffPack[0] = BuffPack[1] ; BuffPack[1] = BuffPack[2] ; BuffPack[2] = readBuf[ix] ;
                // Ищем сигнатуру пакета "OSC"
                if((BuffPack[0] == 'O') && (BuffPack[1] == 'S') && (BuffPack[2] == 'C')){
                    IxPack = 3; LenPack = 0   ;
                }
            }
            else if(IxPack < 6){
                BuffPack[IxPack++] = readBuf[ix]	;
                if(IxPack == 6){
                    ByteBuffer    bbBuff = ByteBuffer.wrap(BuffPack)      ;
                    bbBuff.order(ByteOrder.LITTLE_ENDIAN)                 ;
                    LenPack = bbBuff.getShort(4)  ;
                    if(LenPack >= LenBFpack || LenPack <= 0){
                        SignPack = 0	; IxPack = 0	; LenPack = 0	;
                    }
                }
            }
            else if(IxPack < LenPack){
                BuffPack[IxPack++] = readBuf[ix]	;
                if(LenPack > 0 && IxPack >= LenPack){
                    BuffPack2 = BuffPack.clone()	;
                    listener.onPackData(BuffPack2,LenPack)          ;//!!!!!!!!!!!!!!!!!!!!!!!!
                    SignPack = 0	; IxPack = 0	; LenPack = 0	;
                    BuffPack[0] = BuffPack[1] = BuffPack[2] = 0     ;
                }
            }
        }
    }
    //===================================================================
    private void ParseStr(byte[] data,int len,Listener listener){
        String  Str ;
        for(int ix = 0; ix < len; ix++){
            if(IxStrData >= STRSIZ)  IxStrData = 0;
            if(data[ix] == '\r'){
                StrData[IxStrData++] = data[ix];
                Str = new String(StrData, 0, IxStrData);
                listener.onNewString(Str);
//                      if (DEBUG) Log.d(TAG, "Read string len=" + IxStrData);
                IxStrData = 0;
            } else if(data[ix] != '\n')
                StrData[IxStrData++] = data[ix];
        }
    }
    //===================================================================
    private void step() throws IOException {
        // Handle incoming data.
        int len = mDriver.read(mReadBuffer.array(), READ_WAIT_MILLIS);
//TODO
        if (len > 0) {
//            if (DEBUG) Log.d(TAG, "Read data len=" + len);
            final Listener listener = getListener();
            if (listener != null) {
                final byte[] data = new byte[len];
                mReadBuffer.get(data, 0, len);

                if     (TypeData == STR_DATA) { ParseStr(data,len,listener)     ;}
                else if(TypeData == PACK_DATA){ ParsePack(data,len,listener)    ;}
                else if(TypeData == RAW_DATA) { listener.onRawData(data)        ;}
            }
            mReadBuffer.clear();
        }

        // Handle outgoing data.
        byte[] outBuff = null;
        synchronized (mWriteBuffer) {
            if (mWriteBuffer.position() > 0) {
                len = mWriteBuffer.position();
                outBuff = new byte[len];
                mWriteBuffer.rewind();
                mWriteBuffer.get(outBuff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (outBuff != null) {
//            if (DEBUG) {Log.d(TAG, "Writing data len=" + len);}
            mDriver.write(outBuff, READ_WAIT_MILLIS);
        }
    }
//============================================================================
}
