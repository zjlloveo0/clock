package com.mrzhou5.tools.clock.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author tao-tengtao
 * @date 2018/03/13
 */

public class DateTimeUtil {

    public static final String	DATE_FORMAT_1	= "yyyy-MM-dd HH:mm:ss";
    public static final String	DATE_FORMAT_2	= "yyyyMMddHHmmss";
    public static final String	DATE_FORMAT_3	= "yyyyMMddHHmmssSSS";
    public static final String	DATE_FORMAT_4	= "yyyy-MM-dd";
    public static final String	DATE_FORMAT_5	= "HHmmss";
    public static final String	DATE_FORMAT_6	= "HH:mm";
    public static final String	DATE_FORMAT_7	= "yyyy/MM/dd";
    public static final String	DATE_FORMAT_8	= "yyyyMMdd";

    /**
     * 格式化给定的Date类型的日期为字符串类型
     *
     * @param format
     *            描述日期和时间格式的模式
     * @param date
     *            要格式化的日期（Date类型）
     * @return
     */
    public static String getFormattedDateString(String format, Date date ) {

        String strDate = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat( format );
        if(null != date){
            strDate = dateFormat.format( date );
        }
        return strDate;
    }

    /**
     * 解析字符串的文本，生成 Date
     *
     * @param format
     *            描述日期和时间格式的模式
     * @param strDate
     *            要解析的日期（String类型）
     * @return
     */
    public static Date getDateFromFormattedString(String format, String strDate ) throws ParseException {

        Date retDate = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat( format );

        if(!strDate.equals( "" )){
            retDate = dateFormat.parse( strDate );
        }

        return retDate;
    }
    
    /**
     * 对传入的日期进行按日别的计算
     * @param date 起始日期
     * @param days 便宜日数，若为正数则向后添加，如果负数则向前倒退
     * @return 计算后的日期
     */
    public static Date calDatesByDay(Date date, int days) {
    	
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.add(Calendar.DAY_OF_YEAR, days);
         return rightNow.getTime();
    }
    
    /**
	 * 计算两个时间的差值。传入时间单位为纳秒，返回时间单位为毫秒
	 * 
	 * @param preTime
	 * @param laterTime
	 * @return
	 */
	public static double GetTimeDifferece(final long preTime,
			final long laterTime) {

		return ((laterTime - preTime) / 10e5);
	}
}
