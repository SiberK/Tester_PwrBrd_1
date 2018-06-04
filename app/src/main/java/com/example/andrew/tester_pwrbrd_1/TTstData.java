package com.example.andrew.tester_pwrbrd_1;


import java.nio.ByteBuffer;



import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by Andrew on 19.04.18.
 */

public class TTstData{
    public  byte[]      S = new byte[16]    ;// состояние выходов цифровых
    public  int[]       V = new int[4]      ;// состояние выходов аналоговых
    public  int[]       D = new int[4]      ;// задержки
    public  boolean     isValid = false     ;
    public  int         iS                  ;


    TTstData(byte[] buf){
        if(buf != null){
            ByteBuffer bbPack    = ByteBuffer.wrap(buf)	; bbPack.order(ByteOrder.LITTLE_ENDIAN)	;
            ShortBuffer sbPack  = bbPack.asShortBuffer();
            iS = sbPack.get(0)         ;
            for(int ix=0;ix<16 && ix   < sbPack.limit();ix++){
                S[ix] = (byte) ((iS >> ix) & 1)         ;}
            for(int ix=0;ix< 4 && ix+5 < sbPack.limit();ix++){
                V[ix] = sbPack.get(ix+1)                ;
                D[ix] = sbPack.get(ix+5)                ;}
            isValid = true  ;
        }
    }
}
