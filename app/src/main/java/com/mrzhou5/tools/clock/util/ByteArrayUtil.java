package com.mrzhou5.tools.clock.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteArrayUtil {
    
    public static final int    SHORT_LENGTH    = 2;
    public static final int    INT_LENGTH        = 4;
    
    @Deprecated
    public static byte[] bytesToASCIIbytes( int size, byte[] content ) {
    
        int i = 0;
        int j = 0;
        byte[] retArray = new byte[size * 2];
        for ( i = 0; i < size; i++ ) {
            byte a = (byte) ( ( content[i] >> 4 ) & 0x0F );
            if (a < 0x0A) {
                a = (byte) ( 0x30 + a );
            } else {
                a = (byte) ( 0x37 + a );
            }
            // ret.add( a );
            retArray[j] = a;
            j++;
            a = (byte) ( content[i] & 0x0f );
            if (a < 0x0A) {
                a = (byte) ( 0x30 + a );
            } else {
                a = (byte) ( 0x37 + a );
            }
            // ret.add( a );
            retArray[j] = a;
            j++;
        }
        /*
         * final int retSize = ret.size( ); byte[] retArray = new byte[retSize];
         * for(i=0;i<retSize;i++){ retArray[i] = ret.get( i ); }
         */
        return retArray;
    }
    
    /**
     * 将short类型转换为byte数组
     * 
     * @param shortValue
     *            待转换short数值
     * @return 转换后byte数组
     */
    public static byte[] shortToBytes( short shortValue ) {
    
        byte[] b = new byte[SHORT_LENGTH];
        for ( int i = 0; i < SHORT_LENGTH; i++ ) {
            b[i] = (byte) ( shortValue >> ( i * 8 ) );
        }
        return b;
    }
    
    /**
     * 将byte数组的前4位转换为对应的int值
     * 
     * @param byteArray
     *            待转换byte数组
     * @return 转换后int的值
     */
    public static int bytesToInt( byte[] byteArray ) {
    
        int intValue = 0;
        if (byteArray == null) { throw new IllegalArgumentException( "byte array is null!" ); }
        if (byteArray.length > 4) { throw new IllegalArgumentException( "byte array size > 4 !" ); }
        for ( int i = 0; i < byteArray.length; i++ ) {
            intValue += ( byteArray[i] & 0xFF ) << ( 8 * ( 3 - i ) );
        }
        return intValue;
    }
    
    /**
     * 将int类型的值转换为对应的byte数组
     * 
     * @param intValue
     *            int值
     * @return 转换后byte数组
     */
    public static byte[] intToBytes( int intValue ) {
    
        byte[] b = new byte[INT_LENGTH];
        for ( int i = 0; i < INT_LENGTH; i++ ) {
            b[i] = (byte) ( intValue >> 8 * ( 3 - i ) & 0xFF );
        }
        return b;
    }
    
    /**
     * 将byte数组的最前两位转换为short类型的值
     * 
     * @param byteArray
     *            待转换byte数组
     * @return
     */
    public static short byteToShort( byte[] byteArray ) {
    
        short s = 0;
        if (byteArray == null) { throw new IllegalArgumentException( "byte array is null!" ); }
        short s0 = (short) ( byteArray[0] & 0xff );
        short s1 = (short) ( byteArray[1] & 0xff );
        s1 <<= 8;
        s = (short) ( s0 | s1 );
        return s;
    }
    
    /**
     * ASCII码数组转换为二进制数组
     * 
     * @param size
     *            ASCII码数组长度
     * @param content
     *            　ASCII码数组
     * @return　null 无法转换 　　　byte[] 变换后的二进制数组
     */
    @Deprecated
    public static byte[] ASCIIbytesToBinBytes( int size, byte[] content ) {
    
        int i = 0;
        int j = 0;
        byte[] retArray = new byte[size / 2];
        byte b1;
        byte b2;
        for ( i = 0; i < size; i++ ) {
            b1 = content[i];
            if (( b1 >= 0x30 ) && ( b1 <= 0x39 )) {
                b1 -= 0x30;
            } else if (( b1 >= 0x41 ) && ( b1 <= 0x46 )) {
                b1 -= 0x37;
            } else {
                return null;
            }
            b2 = content[++i];
            if (( b2 >= 0x30 ) && ( b2 <= 0x39 )) {
                b2 -= 0x30;
            } else if (( b2 >= 0x41 ) && ( b2 <= 0x46 )) {
                b2 -= 0x37;
            } else {
                return null;
            }
            byte b = (byte) ( ( ( b1 & 0x0F ) << 4 ) | ( b2 & 0x0F ) );
            retArray[j] = b;
            j++;
        }
        
        return retArray;
    }
    
    /**
     * @功能: 将byte数组转换为16进制字符串
     * @参数: byte数组
     * @结果: 转换得到的字符串
     */
    public static String bytesToHexString( final byte[] src ) {
    
        StringBuilder stringBuilder = new StringBuilder( "" );
        if (src == null || src.length <= 0) { return null; }
        for ( int i = 0; i < src.length; i++ ) {
            int tmp = src[i] & 0xFF;
            String hv = Integer.toHexString( tmp );
            if (hv.length( ) < 2) {
                stringBuilder.append( 0 );
            }
            stringBuilder.append( hv );
        }
        return stringBuilder.toString( );
    }
    
    /**
     * 将指定的int值转换为指定长度的byte数组
     * 
     * @param src
     *            待转换的值
     * @param len
     *            长度
     * @return 转换后的byte数组
     */
    public static byte[] getByteArray( int src, int len ) {
    
        byte[] data = ByteArrayUtil.intToBytes( src );
        byte[] ret = new byte[len];
        
        if (len >= data.length) {
            System.arraycopy( data, 0, ret, len - data.length, data.length );
        } else {
            System.arraycopy( data, data.length - len, ret, 0, len );
        }
        
        return ret;
    }

    /**
     * 将指定的int值转换为指定长度的byte数组
     *
     * @param src
     *            待转换的值
     * @param len
     *            长度
     * @return 转换后的byte数组
     */
    public static byte[] getFixedLenByteArray( byte[] src, int len ) {

        byte[] data = src;
        byte[] ret = new byte[len];

        if (len >= data.length) {
            System.arraycopy( data, 0, ret, len - data.length, data.length );
        } else {
            System.arraycopy( data, data.length - len, ret, 0, len );
        }

        return ret;
    }
    
    /**
     * 将指定的int值转换为byte数组（按照数据长度，将转换为长度为4的byte数组）
     * 
     * @param src
     *            待转换的值
     * @return 转换后的byte数组
     */
    public static byte[] getByteArray( int src ) {
    
        return getByteArray( src, 4 );
    }
    
    /**
     * 检查传入的byte数组内指定区间内容是否符合BCD码的规则
     * 
     * @param data
     *            待检查byte数组
     * @param startIndex
     *            数组内容检查开始下标
     * @param length
     *            检查区间的长度
     * @return 检查结果（true:符合， false:不符合/错误）
     */
    public static boolean checkIsBCDCode( byte[] data, int startIndex, int length ) {
    
        boolean result = true;
        
        if (null == data || 0 == data.length || startIndex >= data.length || length > data.length || ( startIndex + length ) > data.length) { return false; }
        
        for ( int i = startIndex; i < length; i++ ) {
            result = checkIsBCDCode( data[i] );
            if (result == false) {
                break;
            }
        }
        
        return result;
    }
    
    /**
     * 检查传入的byte数组全内容是否符合BCD码的规则
     * 
     * @param data
     *            待检查byte数组
     * @return 检查结果（true:符合， false:不符合/错误）
     */
    public static boolean checkIsBCDCode( byte[] data ) {
    
        if (null == data || 0 == data.length) { return false; }
        
        return checkIsBCDCode( data, 0, data.length );
    }
    
    /**
     * 检查单个byte的数值是否符合BCD码的规则
     * 
     * @param data
     *            待检查byte
     * @return 检查结果（true:符合， false:不符合/错误）
     */
    public static boolean checkIsBCDCode( byte data ) {
    
        boolean result = true;
        int charHigh, charLow;
        
        charLow = data & 0x0f;
        charHigh = ( data >> 4 ) & 0x0f;
        if (charLow > 9 && charHigh > 9) {
            result = false;
        }
        
        return result;
    }
    
    /**
     * 将byte数组转换为int数
     * 
     * @param byteData
     *            待转换的byte数组，长度为4
     * @return 转换后得到的数字
     */
    public static int bytes2Int( final byte[] byteData ) {
    
        int mask = 0xff;
        int temp = 0;
        int intData = 0;
        byte[] data = null;
        
        if (byteData == null) {
            return 0;
        } else if (byteData.length < 4) {
            data = new byte[4];
            Arrays.fill( data, (byte) 0x00 );
            System.arraycopy( byteData, 0, data, 4 - byteData.length, byteData.length );
        } else {
            data = byteData;
        }
        
        for ( int i = 0; i < 4; i++ ) {
            intData <<= 8;
            temp = data[i] & mask;
            intData |= temp;
        }
        return intData;
    }
    
    /**
     * 将传入的数组倒置
     * 
     * @param arr
     *            待处理数组
     * @return 处理后数组
     */
    public static byte[] swapArray( byte[] arr ) {
    
        if (arr == null || arr.length == 0) { return arr; }
        
        byte[] tmp = new byte[arr.length];
        
        for ( int i = 0; i < tmp.length; i++ ) {
            tmp[i] = arr[arr.length - i - 1];
        }
        System.arraycopy( tmp, 0, arr, 0, tmp.length );
        
        return arr;
    }
    
    /**
     * byte[] to short
     * 
     * @param buf
     *            byte数组
     * @param asc
     *            正反
     * @return short值
     */
    public final static short byteArray2Short( byte[] buf, boolean asc ) {
    
        if (buf == null) { throw new IllegalArgumentException( "byte array is null!" ); }
        if (buf.length > 2) { throw new IllegalArgumentException( "byte array size > 2 !" ); }
        short r = 0;
        if (asc) {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }
        return r;
    }

    /**
     * 将bits数组(每个boolean值代表一个bit)转为byte数组
     *
     * @param bits bits数组
     * @return byte数组
     */
    public static byte[] bits2Bytes(boolean[] bits) {
        byte[] bytes = new byte[0];
        if (bits == null || bits.length == 0) {
            return bytes;
        }
        if (bits.length % 8 == 0) {
            bytes = new byte[bits.length / 8];
        } else {
            bytes = new byte[bits.length / 8 + 1];
        }
        for (int i = 0; i < bytes.length; i++) {
            byte b = 0;
            for (int j = 0; j <= 7; j++) {
                int index = (i * 8) + j;
                if (index < bits.length && bits[index]) {
                    int n = 1 << j;
                    b += n;
                }
            }
            bytes[i] = b;
        }
        return bytes;
    }

    public  static boolean[] byteToBits(byte by) {
        boolean[] bits = new boolean[8];

        for (int i=0; i<8; i++) {
            if (((by >> i) & 0x1) == 1) {
                bits[i] = true;
            } else {
                bits[i] = false;
            }
        }

        return bits;
    }

    public static List<boolean[]> bytesToBits(byte[] datas) {

        List<boolean[]> bitsList = new ArrayList<>();

        try {
            if (null == datas || datas.length == 0) {
                return bitsList;
            }

            for(int i=0; i<datas.length; i++) {
                boolean[] bits = byteToBits(datas[i]);
                bitsList.add(bits);
            }
        } catch (Exception e) {
            e.printStackTrace();
            bitsList = null;
        }

        return bitsList;
    }

    /**
     * int转byte数组
     * @param value
     * @return
     */
    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 8),
                (byte) value};
    }

    /**
     * byte数组转ArrayList
     * @param bytes
     * @return
     */
    public static ArrayList<Byte> byteArrayToByteArrayList(byte[] bytes) {
        ArrayList<Byte> returnValue = new ArrayList<>();

        for (int i = 0; i < bytes.length; i++) {
            returnValue.add(bytes[i]);
        }

        return returnValue;
    }

    /**
     * ArrayList转byte数组
     * @param byteArrayList
     * @return
     */
    public static byte[] byteArrayListToByteArray(ArrayList<Byte> byteArrayList) {
        byte[] values = new byte[byteArrayList.size()];

        for (int i = 0; i < byteArrayList.size(); i++) {
            values[i] = byteArrayList.get(i);
        }
        return values;
    }
}
