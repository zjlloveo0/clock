package com.mrzhou5.tools.clock.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @category 字符串操作方法类
 * @author tao-tengtao
 * 
 */
public class StringUtil {

    /**
     * IsNumbric方法判断使用正则预编译
     */
    private final static Pattern isNumbericPattern = Pattern.compile("[0-9]*");

    /**
     * IsSignedNumbric方法判断使用正则预编译
     */
    private final static Pattern isSignedNumbericPattern = Pattern.compile("-?[0-9]+");

    /**
     * IsNumbric方法判断使用正则预编译
     */
    private final static Pattern isFloatPattern = Pattern.compile("^[0-9]+(.[0-9]+)?$");

    /**
     * 判断字符串内是否包含了unicode编码
     */
    private final static Pattern unicodePattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");

    /**
     * 
     * @param asc
     *            追加目标字符串
     * @param length
     *            追加空格个数
     * @param appendHead
     *            是否在字符串头追加
     * @return 处理后字符串
     */
    public static String addChar2Str(final String str, final int spaceCount,
            final char chr, final boolean appendHead) {

        char[] chs = null;

        if (str == null) {
            return "";
        } else if (spaceCount < 0) {
            return str;
        }

        chs = new char[str.length() + spaceCount];

        for (int i = appendHead ? 0 : str.length(), j = 0; j < spaceCount; j++) {
            chs[i++] = chr;
        }
        System.arraycopy(str.toCharArray(), 0, chs,
                appendHead ? spaceCount : 0, str.length());
        return new String(chs);
    }

    /**
     * 取得指定字符串从指定位置开始的指定长度个字符的子串
     * @param content    源字符串
     * @param startPos    开始位置
     * @param len        指定长度
     * @return            子串
     */
    public static String getSubString(String content, int startPos, int len) {

        String strSubString = "";
        try {
            if (content == null || content.length() < startPos + len) {
                return "";
            }

            strSubString = content.substring(startPos, startPos + len);
        } catch (Exception e) {
            // Util.WriteDebugMessage( "字符串截取异常?�?", e );

            strSubString = "";
        }

        return strSubString;
    }
    
    /**
     * 将传入的字符串截取或填充至指定长度
     * 
     * @param str
     *            字符串
     * @param nLen
     *            指定长度
     * @param fillStr
     *            若字符串长度不足nLen所指定长度，则用于填充的字符
     * @param appendHead
     *            若字符串长度不足nLen所指定长度，则是否从头开始补齐
     * @return
     */
    public static String getFixedLenString(String str, int nLen, char fillchar,
            boolean appendHead) {

        String strResult = str;

        if (strResult.length() < nLen) {
            strResult = addChar2Str(strResult, nLen - strResult.length(),
                    fillchar, appendHead);
        } else if (strResult.length() > nLen) {
            strResult = strResult.substring(0, nLen);
        }

        return strResult;
    }

    /**
     * 将十进制数字字符串转换为16进制数字字符串
     * 
     * @param src
     *            十进制数字字符串
     * @return 16进制数字字符串
     */
    public static String num2HexString(String src) {

        String hex_str = "";

        if (src == null || src.trim().equals("")) {
            return hex_str;
        }

        try {
            // 将数字转换成为16进制字符串
            if (src.trim().matches("[0-9]*")) {
                BigInteger target = new BigInteger(src.trim());
                hex_str = target.toString(16);
            }
        } catch (Exception e) {

            hex_str = "";
        }

        return hex_str;
    }

    /**
     * 将字符串转换为bsc码
     * @param asc
     *            待转换字符串
     * @return 转换后的byte数组
     */
    public static byte[] str2Bcd(final String ascStr) {

        int len = 0;
        int mod = 0;
        String asc = "";

        if (ascStr == null) {
            return null;
        }

        asc = ascStr;
        len = asc.length();
        mod = len % 2;
        if (len == 0) {
            return null;
        }

        if (mod != 0) {
            asc = "0" + asc;
            len = asc.length();
        }
        byte abt[] = new byte[len];
        if (len >= 2) {
            len = len / 2;
        }
        byte bbt[] = new byte[len];
        abt = asc.getBytes();
        int j, k;
        for (int p = 0; p < asc.length() / 2; p++) {
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
                j = abt[2 * p] - '0';
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
                j = abt[2 * p] - 'a' + 0x0a;
            } else {
                j = abt[2 * p] - 'A' + 0x0a;
            }
            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
                k = abt[2 * p + 1] - '0';
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
                k = abt[2 * p + 1] - 'a' + 0x0a;
            } else {
                k = abt[2 * p + 1] - 'A' + 0x0a;
            }
            int a = (j << 4) + k;
            byte b = (byte) a;
            bbt[p] = b;
        }
        return bbt;
    }
    
    /**
     * @功能: 将一个BCD码转换为字符串
     * @参数: BCD码
     * @结果:转换后字符串
     */
    public static String bcd2Str(final byte byteVal) {

        StringBuffer temp = new StringBuffer(2);

        temp.append((byte) ((byteVal & 0xf0) >>> 4));
        temp.append((byte) (byteVal & 0x0f));

        return temp.toString();
    }

    /**
     * @功能: 10进制串转为16进制码
     * @参数: 10进制串
     * @参数: 返回字节数组的长度
     * @结果:16进制码
     */
    public static byte[] str2Hex(final String asc, final int nByte) {

        int intData = 0;
        String frmtStr = "";

        if (asc == null || asc.length() == 0 || nByte > asc.length()
                || !IsNumberic(asc)) {
            return null;
        }

        intData = Integer.parseInt(asc);
        frmtStr = String.format("%d", 2 * nByte);
        frmtStr = "%0" + frmtStr + "X";
        String hexStr = String.format(frmtStr, intData);

        int p = 0;
        byte bbt[] = new byte[nByte];
        for (p = 0; p < nByte; p++) {
            bbt[p] = (byte) Integer.parseInt(
                    hexStr.substring(p * 2, p * 2 + 2), 16);
        }

        return bbt;
    }

    /**
     * @功能: 将byte数组转换为16进制字符串
     * @参数: byte数组
     * @结果: 转换得到的字符串
     */
    public static String bytesToHexString(final byte[] src) {

        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int tmp = src[i] & 0xFF;
            String hv = Integer.toHexString(tmp);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    
    /**
     * @功能: 将byte数组转换为10进制字符串
     * @参数: byte数组
     * @结果: 转换得到的字符串
     */
    public static String bytesToDecString(final byte[] src) {

        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int tmp = src[i] & 0xFF;
            String hv = Integer.toString(tmp);
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    
    /**
     * Ascii转换为字符串
     * @param value
     * @return
     */
    public static String asciiToString(String value)
    {
        StringBuffer sbu = new StringBuffer();
        String[] chars = value.split(",");
        for (int i = 0; i < chars.length; i++) {
            sbu.append((char) Integer.parseInt(chars[i]));
        }
        return sbu.toString();
    }
    
    /**
     * 将Ascii码的byte数组转换为字符串
     * @param src
     * @return
     */
    public static String asciiByteToString(final byte[] src )
    {
         StringBuffer tStringBuf=new StringBuffer ();
         char[]tChars=new char[src.length];
         for(int i=0 ; i<src.length; i++){
             tChars[i]=(char)src[i];
         }
         tStringBuf.append(tChars);     
         return tStringBuf.toString();
    }

    /**
     * 将16进制字符串转换为对应的byte数组
     *
     * @param hexStr
     *            16进制字符串
     * @return 转换得到的byte数组
     */
    public static byte[] hexStringToBytes(final String hexStr) {

        String str = "";
        if (hexStr == null || hexStr.equals("")) {
            return null;
        }

        str = hexStr;
        if(str.length() % 2 == 1) {
            str = "0" + str;
        }
        String hexString = str.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] arrData = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            arrData[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return arrData;
    }

    /**
     * Convert char to byte
     * 
     * @param c
     *            char
     * @return byte
     */
    private static byte charToByte(final char c) {

        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 将所指定字符串进行md5加密
     * @param src
     *            待加密字符串
     * @return MD5加密后32位大写密文
     */
    @Deprecated
    public static String MD5Str(final String src) {

        final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        byte[] strTemp = null;
        MessageDigest mdTemp = null;

        try {
            if (src == null || src.equals("")) {
                return "";
            }

            strTemp = src.getBytes();
            mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str).toUpperCase();
        } catch (Exception e) {

            return null;
        }
    }

    /**
     * 向目标字符串的头或者尾添加指定个数个的空格字符
     * @param asc
     *            追加目标字符串
     * @param length
     *            追加空格个数
     * @param appendHead
     *            是否在字符串头追加
     * @return 处理后字符串
     */
    public static String AddSpace2Str(final String src, final int spaceCount,
            final boolean appendHead) {

        String str = "";
        char[] chs = null;
        if (src == null) {
            str = "";
        } else {
            str = src;
        }

        if (spaceCount < 0) {
            return str;
        }

        chs = new char[str.length() + spaceCount];
        for (int i = appendHead ? 0 : str.length(), j = 0; j < spaceCount; j++) {
            chs[i++] = ' ';
        }
        System.arraycopy(str.toCharArray(), 0, chs,
                appendHead ? spaceCount : 0, str.length());
        return new String(chs);
    }

    /**
     * 向指定字符串的头或尾添加指定个数的指定字符
     * @param asc
     *            追加目标字符串
     * @param length
     *            追加空格个数
     * @param appendHead
     *            是否在字符串头追加
     * @return 处理后字符串
     */
    public static String AddChar2Str(final String str, final int spaceCount,
            final char chr, final boolean appendHead) {

        char[] chs = null;

        if (str == null) {
            return "";
        } else if (spaceCount < 0) {
            return str;
        }

        chs = new char[str.length() + spaceCount];

        for (int i = appendHead ? 0 : str.length(), j = 0; j < spaceCount; j++) {
            chs[i++] = chr;
        }
        System.arraycopy(str.toCharArray(), 0, chs,
                appendHead ? spaceCount : 0, str.length());
        return new String(chs);
    }

    /**
     * 把全是数字的String转byte数组
     *
     * @param s 纯数字字符串
     * @return
     */
    public static byte[] getBytesFromStringNum(String s) {
        byte[] b = new byte[s.length()];
        try {
            if (!s.matches("^[0-9]\\d*$")) {
                return b;
            }
            for (int i = 0; i < s.length(); i++) {
                String temp = s.charAt(i) + "";
                int tempInt = Integer.parseInt(temp);
                b[i] = (byte) tempInt;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * 将一个偶数长度的16进制字符串高低位互换，返回互换后16进制所对应的10进制字符串
     * @param hexStr
     * @return
     */
    public static String swapAndConverHex2Str(String hexStr) {
        String strNum = "";

        if (hexStr == null || hexStr.equals("") || (hexStr.length() % 2 != 0)) {
            return strNum;
        }

        try {
            // �?16进制数值进行高低位互换
            strNum = "";
            for (int i = (hexStr.length() / 2 - 1); i >= 0; i--) {
                strNum += hexStr.substring(2 * i, 2 * i + 2);
            }

            // �?高低位互换后的16进制数字转换为10进制数字字符串
            Long lTmp = Long.parseLong(strNum, 16);
            strNum = Long.valueOf(lTmp).toString();
        } catch (Exception e) {
            strNum = "";
        }

        return strNum;
    }

    /**
     * 将一个偶数长度的16进制字符串高低位互换，返回互换后16进制所对应的10进制字符串
     * @param hexStr
     * @return
     */
    public static Long swapAndConverHex2Long(String hexStr) {
        Long lTmp = 0L;

        if (hexStr == null || hexStr.equals("") || (hexStr.length() % 2 != 0)) {
            return lTmp;
        }

        try {
            //将16进制数值进行高低位互换
            String strNum = "";
            for (int i = (hexStr.length() / 2 - 1); i >= 0; i--) {
                strNum += hexStr.substring(2 * i, 2 * i + 2);
            }

            //将高低位互换后的16进制数字转换为10进制数字字符串
            lTmp = Long.parseLong(strNum, 16);
        } catch (Exception e) {
            // Util.WriteDebugMessage(Message.MSG_449, e);

            lTmp = 0L;
        }

        return lTmp;
    }

    /**
     * 固定格式时间戳转成Date对象
     * @param str
     * @return
     */
    public static Date  timestampToDate(String str){

        Date date = null;

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        long timel = Long.parseLong(str);
        String timeStr = format.format(timel);

        try {
            date = format.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Date对象转为固定格式时间戳
     * @param date
     * @return
     */
    public static String DateTotimestamp(Date date){
        long times = date.getTime();
        String timestamp = String.valueOf(times);
        return timestamp;
    }

    /**
     * 判定字符串内所有字符是否都是数字
     *
     * @param str
     *            待判定字符串
     * @return
     */
    public static boolean IsNumberic(final String str) {

        Matcher isNum = null;

        if (str == null || str.equals("")) {
            return false;
        }

        isNum = isNumbericPattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 判定字符串内容是否是整数（包括负数）
     *
     * @param str
     *            待判定字符串
     * @return
     */
    public static boolean IsSignedNumberic(final String str) {

        Matcher isNum = null;

        if (str == null || str.equals("")) {
            return false;
        }

        isNum = isSignedNumbericPattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 判定字符串是否是float
     *
     * @param str
     *            待判定字符串
     * @return
     */
    public static boolean IsFloat(final String str) {

        Matcher isNum = null;

        if (str == null || str.equals("")) {
            return false;
        }

        isNum = isFloatPattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }

        return true;
    }


    /**
     * 去除字符串的左边从头开始的连续字符串
     * @param str 源字符串
     * @return      去除左边从头开始的连续字符串
     */
    public static String ltrim(String str) {
        char[] arrayOfChar = str.toCharArray();
        int pos = 0;

        for (pos = 0; pos < arrayOfChar.length; pos++) {
            if (arrayOfChar[pos] != ' ') {
                break;
            }
        }

        return str.substring(pos);
    }

    public static String getExceptionMessage(Exception e) {

        String ret = "";

        if (null == e) {
            return "";
        }

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            ret = sw.toString();
        } catch (Exception ee) {
            ret = "";
        }

        return ret;
    }
    
    /**
     * 字符串转换为Ascii码数组
     * 
     * @param value
     * @return
     */
    public static byte[] stringToAscii( String value ) {
    
        char[] chars = null;
        byte[] result = null;
        
        if (null == value || value.length( ) == 0) { return null; }
        
        try {
            chars = value.toCharArray( );
            result = new byte[chars.length];
            
            for ( int i = 0; i < chars.length; i++ ) {
                result[i] = (byte) chars[i];
            }
        } catch ( Exception e ) {
            result = null;
            e.printStackTrace( );
        }
        
        return result;
    }
    
    /**
     * Ascii转换为字符串
     * 
     * @param value
     * @return
     */
    public static String asciiToString( byte[] value ) {
    
        StringBuffer sbu = new StringBuffer( );
        String resultString = "";
        
        try {
            if (null == value || value.length == 0) { return ""; }
            for ( int i = 0; i < value.length; i++ ) {
                sbu.append( (char) value[i] );
            }
            resultString = sbu.toString( );
        } catch ( Exception e ) {
            e.printStackTrace( );
            resultString = "";
        }
        
        return resultString;
    }

    /**
     * Unicode转 汉字字符串
     *
     * @param str \u6728
     * @return '木' 26408
     */
    public static String unicodeToString(String str) {

        Matcher matcher = unicodePattern.matcher(str);
        char ch;
        while (matcher.find()) {
            //group 6728
            String group = matcher.group(2);
            //ch:'木' 26408
            ch = (char) Integer.parseInt(group, 16);
            //group1 \u6728
            String group1 = matcher.group(1);
            str = str.replace(group1, ch + "");
        }
        return str;
    }
}
