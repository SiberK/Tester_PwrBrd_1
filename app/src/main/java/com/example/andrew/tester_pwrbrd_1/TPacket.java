package com.example.andrew.tester_pwrbrd_1;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import java.nio.ShortBuffer;

/**
 * Created by Andrew on 19.04.18.
 */

public class TPacket{  public static final   int     SizeHdr   = 8       ;
    public static final   int     SizeOfCRC = 4       ;
    public static final	int		SizePack = SizeHdr+SizeOfCRC		;
    public static final	byte[]	SIGN = {'O','S','C'};
    public static final   byte    ptECG1   ='1'	,ptIRR     ='2'	,ptStartStop='S',
            ptOSC1   ='4'	,ptOSC2    ='5'	,ptOSC3     ='6',
            ptSetCfg ='A'	,ptSendCfg ='C'	,ptSetTS  ='D'	,ptMsg ='M'	,
            ptFileDir='F'	,ptFileRead='R'	,ptFileDel='L'	,
            ptInitFS ='G'	,ptDbg1    ='X' ,ptSetParam ='P',
            ptAck    ='Z'	,ptBad     ='B' ,ptSetFreq  ='F',
            ptLCMeter='L'           ;

    private				byte		Typ					;
    private				int	    	SizeOf=0, TS, LenPack=0, LenDat=0 ;
    private static		int	    	FTS = 0				;
    public              byte[]      bData = null    ;

    public      boolean             isValid = false     ;

    //----------------------------------------------------------------------------------
            TPacket(byte[] inBuf,int len)
            {
                if(inBuf != null){
                    long CS = TPacket.CRC32(inBuf,len)                  ;
                    isValid = CS == 0                                   ;
                    ByteBuffer bbPack = ByteBuffer.wrap(inBuf)          ;
                    bbPack.order(ByteOrder.LITTLE_ENDIAN)               ;
                    ShortBuffer sbPack = bbPack.asShortBuffer()         ;
                    LenPack = sbPack.get(2)                             ;
                    LenDat  = LenPack - TPacket.SizePack                ;
                    bData   = new byte[LenDat]                          ;
                    System.arraycopy(inBuf, SizeHdr, bData, 0, LenDat)  ;
                }
            }
    //----------------------------------------------------------------------------------
    public static ByteBuffer CreateBuf(byte _typ, ByteBuffer bbData)
    {
        int         DataLen = bbData == null ? 0 : bbData.capacity()             ;
        int			_sizeOf = DataLen + SizePack		    ;
        _sizeOf = ((_sizeOf + SizeOfCRC-1)/SizeOfCRC)*SizeOfCRC   ;
        ByteBuffer  bbPack = ByteBuffer.allocate(_sizeOf)   ;
        bbPack.order(ByteOrder.LITTLE_ENDIAN)	            ;
        bbPack.clear() ;

        bbPack.put(SIGN[0])	; bbPack.put(SIGN[1]); bbPack.put(SIGN[2])	;// SIGN
        bbPack.put(_typ)			                       	;// TYP
        bbPack.putShort( (short)_sizeOf)				    ;// SizeOf
        bbPack.putShort((short)(FTS++))    			    	;// TS
//	for(int ix=0;ix<DataLen;ix++)
        if(bbData != null) bbPack.put(bbData.array(),0,DataLen) ;
//
//    int	CRC16 = crc16(bbPack.array(),_sizeOf-2)			;
//    bbPack.putShort((short)CRC16)                       ;// CRC
        long	CS_CRC = CRC32(bbPack.array(),_sizeOf-SizeOfCRC)	;
        bbPack.putInt((int)CS_CRC)                          ;// CRC

        bbPack.flip()       ;
        return 		bbPack	;
    }
    //----------------------------------------------------------------------------------
    public static ByteBuffer	CreateBuf(byte _typ, byte bData)
    {
        int         DataLen = 1 ;
        int			_sizeOf = DataLen + SizePack		    ;
        _sizeOf = ((_sizeOf + SizeOfCRC-1)/SizeOfCRC)*SizeOfCRC   ;
        ByteBuffer  bbPack = ByteBuffer.allocate(_sizeOf)   ;
        bbPack.order(ByteOrder.LITTLE_ENDIAN)	            ;
        bbPack.clear() ;

        bbPack.put(SIGN[0])	; bbPack.put(SIGN[1]); bbPack.put(SIGN[2])	;// SIGN
        bbPack.put(_typ)			                       	;// TYP
        bbPack.putShort( (short)_sizeOf)				    ;// SizeOf
        bbPack.putShort((short)(FTS++))    			    	;// TS
//	for(int ix=0;ix<DataLen;ix++)
        bbPack.put(bData)    ;

        long	CS_CRC = CRC32(bbPack.array(),_sizeOf-SizeOfCRC)	;
//        bbPack.putShort((short)CS_CRC)                      ;// CRC
        bbPack.putInt((int)CS_CRC)                          ;// CRC

        bbPack.flip()       ;
        return 		bbPack	;
    }
    //----------------------------------------------------------------------------------
    public static ByteBuffer Request(byte ReqCode)
    { return TPacket.CreateBuf(ReqCode,null) ;}
    //------------------------------------------------------------------------

    //----------------------------------------------------------------------------------
    private static final int  POLYNOMIAL        =	0x04C11DB7  ;
    private static final int  INITIAL_REMAINDER = 	0xFFFFFFFF  ;

    //            #define WIDTH    		32
//            #define TOPBIT   		(1 << (WIDTH - 1))
    private static int[]  crcTable = new int[256]	    ;
    private static int     TblInit = 0                 ;
    //---------------------------------------------------------------------------
    private static void  crcInit()
    {int	remainder, dividend, bit		        ;

        // Compute the remainder of each possible dividend.
        for(dividend = 0; dividend < 256; ++dividend){
            remainder = (dividend << 24) & 0xFFFFFFFF  ;// Start with the dividend followed by zeros.
            for (bit = 8; bit > 0;bit--){
                remainder = ((remainder & 0x80000000) != 0 ? (remainder << 1) ^ POLYNOMIAL :
                        (remainder << 1))&0xFFFFFFFF			    ;}
            crcTable[dividend] = remainder           ;}
    }
    //---------------------------------------------------------------------------
    public static int CRC32(byte[] buf,int  len )
    {
        int             ix,ib	;
        int             data	    ;
        int             remainder = INITIAL_REMAINDER		 ;

        if(TblInit == 0){ TblInit = 1	; crcInit()		     ;}

        for(ix = 0; ix < len;ix++){
            ib   = ((ix>>2)<<2) | (3-(ix&3))			         ;
            data = (buf[ib] ^ (int)(remainder >> 24))&0xFF      ;
            remainder = (crcTable[data] ^ ((remainder << 8)))   ;}
        remainder &= 0xFFFFFFFF          ;
        return remainder     ;
    }
//---------------------------------------------------------------------------
//----------------------------------------------------------------------------------
}
