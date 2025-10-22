package com.hk1089.mettax.utils;

import android.content.Context;


import com.hk1089.mettax.R;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 日期转换工具类
 * @ClassName: DateUtil
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author:
 * @date: Nov 10, 2011 10:00:14 AM
 * @version: V1.0
 */
public class DateUtil {
  public static String dateSwitchString(Date date){
    SimpleDateFormat formatDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String str="";
    if(date!=null)
      str=formatDate.format(date);
    return str;
  }

  public static String dateSwitchStringEx(Date date){
    SimpleDateFormat formatDate=new SimpleDateFormat("yyyyMMddHHmmss");
    String str="";
    if(date!=null)
      str=formatDate.format(date);
    return str;
  }

  public static String dateSwitchDateString(Date date){
    SimpleDateFormat formatDate=new SimpleDateFormat("yyyy-MM-dd");
    String str="";
    if(date!=null)
      str=formatDate.format(date);
    return str;
  }

  public static String dateSwitchTimeString(Date date){
    SimpleDateFormat formatDate=new SimpleDateFormat("HH:mm:ss");
    String str="";
    if(date!=null)
      str=formatDate.format(date);
    return str;
  }

  /*
   * 将秒转移成小时分秒的方式
   */
  public static String secondSwitchHourString(int totalSecond, String hour, String minute, String second){
    StringBuilder ret = new StringBuilder();
    if (totalSecond >= 3600) {
      ret.append(totalSecond/3600 + hour);
    }
    totalSecond = totalSecond%3600;
    if (totalSecond/60 > 0) {
      ret.append(totalSecond/60 + minute);
    }
    totalSecond = totalSecond%60;
    if (totalSecond > 0) {
      ret.append(totalSecond + second);
    }
    return ret.toString();
  }

  /*
   * 将秒转移成小时分秒的方式
   */
  public static String secondSwitchHourString(int totalSecond) {
    return String.format("%02d:%02d:%02d", totalSecond/3600, totalSecond % 3600 / 60, totalSecond % 60);
  }

  /*
   * 将秒转移成天小时分秒的方式
   */
  public static String secondSwitchDateHourString(int totalSecond, String date, String hour, String minute, String second) {
    StringBuilder ret = new StringBuilder();

    if (totalSecond >= (3600 * 24)) {
      ret.append(totalSecond/(3600 * 24) + date);
    }

    if (totalSecond >= 3600) {
      ret.append(totalSecond/3600 + hour);
    }
    totalSecond = totalSecond%3600;
    if (totalSecond/60 > 0) {
      ret.append(totalSecond/60 + minute);
    }
    totalSecond = totalSecond%60;
    if (totalSecond > 0) {
      ret.append(totalSecond + second);
    }
    return ret.toString();
  }

  /**
   * 时间戳转换为字符串
   * @param time
   * @return
   */
  public static String timestampSwitchString(Timestamp time) {
    Date date = null;
    if (null != time) {
      date = new Date(time.getTime());
    }
    return dateSwitchString(date);
  }


  /**
   * 比较两个时间大小
   * @Title: compareDate
   * @Description: TODO
   * @param: date1
   * @param: date2
   * @return: boolean
   * @throws:
   */
  public static boolean compareDate(Date date1,Date date2){
    if(date1.after(date2))
      return true;
    else
      return false;
  }

  /**
   * 判断长时间（2012-04-05 11:00:23)是否合法
   * @Title: isLongTimeValid
   * @Description: TODO
   * @param: time
   * @return: boolean
   * @throws:
   */
  public static boolean isLongTimeValid(String time) {
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      dfs.parse(time);
      return true;
    } catch (ParseException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 判断长时间（2012-04-05 11:00:23)是否合法
   * @Title: isLongTimeValid
   * @Description: TODO
   * @param: time
   * @return: boolean
   * @throws:
   */
  public static boolean isDateValid(String time) {
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd");
    try {
      dfs.parse(time);
      return true;
    } catch (ParseException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 比较字符行长时间格式，时间大小
   * @Title: compareStrLongTime
   * @Description: TODO
   * @param: date1
   * @param: date2
   * @return: boolean
   * @throws:
   */
  public static int compareStrLongTime(String time1, String time2){
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      Date date1 = dfs.parse(time1);
      Date date2 = dfs.parse(time2);
      return date1.compareTo(date2);
    } catch (ParseException e) {
      e.printStackTrace();
      return 2;
    }
  }

  /**
   * 字符串转换成时间
   * @Title: StrDate2Date
   * @Description: TODO
   * @throws:
   */
  static public Date StrDate2Date(String time) {
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd");
    try {
      return dfs.parse(time);
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * 字符串转换成时间
   * @Title: StrLongTime2Date
   * @Description: TODO
   * @param: date1
   * @param: date2
   * @return: boolean
   * @throws:
   */
  static public Date StrLongTime2Date(String time) {
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      return dfs.parse(time);
    } catch (ParseException e) {
      return null;
    }
  }

  /*
   * 添加秒数
   */
  static public Date AddSecond(Date date, int second) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.SECOND, second);
    return calendar.getTime();
  }



  /**
   * 秒数转化为日期
   * */
  public static String getDateFromSeconds(String seconds){
    if(seconds==null){

      return " ";
    }else{
      Date date=new Date();
      try{
        date.setTime(Long.parseLong(seconds)*1000);
      }catch(NumberFormatException nfe){
      }
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(date);
    }
  }

  /**
   * 时间戳转化为日期
   * */
  public static String getDateFromTimestampEx(String timestamp){
    if(timestamp==null){

      return " ";
    }else{
      Date date=new Date();
      try{
        date.setTime(Long.parseLong(timestamp));
      }catch(NumberFormatException nfe){
      }
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(date);
    }
  }

  /**
   * 秒数转化为年月日时分秒
   * */
  public static String getDateFromTimestamp(long timestamp){
    if(timestamp==0){
      return "";
    }else{
      Date date=new Date();
      try{
        date.setTime(timestamp);
      }catch(NumberFormatException nfe){
      }
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
  }
  /**
   * 秒数转化为年月日
   * */
  public static String getDateFromTimestampEx(long timestamp){
    if(timestamp==0){
      return "";
    }else{
      Date date=new Date();
      try{
        date.setTime(timestamp);
      }catch(NumberFormatException nfe){
      }
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(date);
    }
  }

  /**
   * 比较两个日期的大小，日期格式为yyyy-MM-dd HH:mm:ss
   *
   * @param str1 the first date
   * @param str2 the second date
   * @return true <br/>false
   */
  public static boolean isTimeOneBigger(String str1, String str2) {
    boolean isBigger = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date dt1 = null;
    Date dt2 = null;
    try {
      dt1 = sdf.parse(str1);
      dt2 = sdf.parse(str2);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    if (dt1.getTime() > dt2.getTime()) {
      isBigger = true;
    } else if(dt1.getTime() == dt2.getTime()){
      isBigger = true;
    }else if (dt1.getTime() < dt2.getTime()) {
      isBigger = false;
    }
    return isBigger;
  }

  /**
   * 比较两个日期的大小，日期格式为yyyy-MM-dd
   *
   * @param str1 the first date
   * @param str2 the second date
   * @return true <br/>false
   */
  public static boolean isDateOneBiggerEx(String str1, String str2) {
    boolean isBigger = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date dt1 = null;
    Date dt2 = null;
    try {
      dt1 = sdf.parse(str1);
      dt2 = sdf.parse(str2);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    if (dt1.getTime() > dt2.getTime()) {
      isBigger = true;
    } else if(dt1.getTime() == dt2.getTime()){
      isBigger = false;
    }else if (dt1.getTime() < dt2.getTime()) {
      isBigger = false;
    }
    return isBigger;
  }

  /**
   * 比较两个日期的大小，日期格式为yyyy-MM-dd HH:mm:ss
   *
   * @param str1 the first date
   * @param str2 the second date
   * @return true <br/>false
   */
  public static boolean isDateOneBigger(String str1, String str2) {
    boolean isBigger = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date dt1 = null;
    Date dt2 = null;
    try {
      dt1 = sdf.parse(str1);
      dt2 = sdf.parse(str2);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    if (dt1.getTime() > dt2.getTime()) {
      isBigger = true;
    } else if (dt1.getTime() < dt2.getTime()) {
      isBigger = false;
    }
    return isBigger;
  }

  /**
   * 获取当前日期
   * */
  public static String getCurrentDate(){
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    Date curDate =  new Date(System.currentTimeMillis());
    return sdf.format(curDate);
  }

  /**
   * 获取月日
   * */
  public static String getFormatDateEx(String str) {
    SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat sf2 = new SimpleDateFormat("MM-dd");
    String formatStr = "";
    try {
      formatStr = sf2.format(sf1.parse(str));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return formatStr;
  }

  /**
   * 获取年月日
   * */
  public static String getFormatDate(String str) {
    SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sf2 = new SimpleDateFormat("yyyy-MM-dd");
    String formatStr = "";
    try {
      formatStr = sf2.format(sf1.parse(str));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return formatStr;
  }

  /**
   * 获取时分
   * */
  public static String getFormatHourMin(String str) {
    SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sf2 = new SimpleDateFormat("HH:mm");
    String formatStr = "";
    try {
      formatStr = sf2.format(sf1.parse(str));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return formatStr;
  }

  /**
   * 获取时分秒
   * */
  public static String getFormatHourMinSec(String str) {
    SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sf2 = new SimpleDateFormat("HH:mm:ss");
    String formatStr = "";
    try {
      formatStr = sf2.format(sf1.parse(str));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return formatStr;
  }

  /**
   * 获取月日时分秒
   * */
  public static String getFormatDateTime(String str) {
    SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sf2 = new SimpleDateFormat("MM-dd HH:mm:ss");
    String formatStr = "";
    try {
      formatStr = sf2.format(sf1.parse(str));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return formatStr;
  }


  /**
   * 由过去的某一时间,计算距离当前的时间
   */
  public static String CalculateTime(Context context, String time) {
    long nowTime = System.currentTimeMillis(); // 获取当前时间的毫秒数
    String msg = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 指定时间格式
    Date setTime = null; // 指定时间
    try {
      setTime = sdf.parse(time); // 将字符串转换为指定的时间格式
    } catch (ParseException e) {

      e.printStackTrace();
    }
    long reset = setTime.getTime(); // 获取指定时间的毫秒数
    long dateDiff = nowTime - reset;
    if (dateDiff < 0) {
      //msg += context.getString(R.string.date_util_time_wrong);
      msg  = context.getString(R.string.date_util_time_just);
    } else {
      long dateTemp1 = dateDiff / 1000; // 秒
      long dateTemp2 = dateTemp1 / 60; // 分钟
      long dateTemp3 = dateTemp2 / 60; // 小时
      long dateTemp4 = dateTemp3 / 24; // 天数
      long dateTemp5 = dateTemp4 / 30; // 月数
      long dateTemp6 = dateTemp5 / 12; // 年数

      if (dateTemp6 > 0) {

        msg = dateTemp6 + context.getString(R.string.date_util_time_year);
      } else if (dateTemp5 > 0) {
        msg = dateTemp5 + context.getString(R.string.date_util_time_month);
      } else if (dateTemp4 > 0) {
        msg = dateTemp4 + context.getString(R.string.date_util_time_day);
      } else if (dateTemp3 > 0) {
        msg = dateTemp3 + context.getString(R.string.date_util_time_hour);
      } else if (dateTemp2 > 0) {
        msg = dateTemp2 + context.getString(R.string.date_util_time_min);
      } else if (dateTemp1 > 0) {

        msg = context.getString(R.string.date_util_time_just);
      }
    }
    return msg;
  }

  /**
   * 由过去的某一时间,计算距离当前的时间 秒
   */
  public static int CalculateTimeWithSecond(String time) {
    if(time == null){
      return 0;
    }
    if(time.isEmpty()){
      return 0;
    }
    long nowTime = System.currentTimeMillis(); // 获取当前时间的毫秒数
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 指定时间格式
    Date setTime = null; // 指定时间
    try {
      setTime = sdf.parse(time); // 将字符串转换为指定的时间格式
    } catch (ParseException e) {

      e.printStackTrace();
    }
    long reset = setTime.getTime(); // 获取指定时间的毫秒数
    long dateDiff = nowTime - reset;
    if (dateDiff < 0) {
      return 0;
    }
    int tempSec = (int) (dateDiff / 1000);
    return tempSec;
  }

  /**
   * 由过去的某一时间,计算距离当前的时间
   */
  public static String CalculateTimeNoSecond(Context context, String time) {
    if(time == null){
      return"";
    }
    if(time.isEmpty()){
      return"";
    }
    long nowTime = System.currentTimeMillis(); // 获取当前时间的毫秒数
    String msg = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 指定时间格式
    Date setTime = null; // 指定时间
    try {
      setTime = sdf.parse(time); // 将字符串转换为指定的时间格式
    } catch (ParseException e) {

      e.printStackTrace();
    }
    long reset = setTime.getTime(); // 获取指定时间的毫秒数
    long dateDiff = nowTime - reset;
    if (dateDiff < 0) {
      //msg += context.getString(R.string.date_util_time_wrong);
      msg  = "";
    } else {
      long nd = 1000 * 24 * 60 * 60;
      long nh = 1000 * 60 * 60;
      long nm = 1000 * 60;
      long ns = 1000;
      // long ns = 1000;
      // 计算差多少天
      long day = dateDiff / nd;
      // 计算差多少小时
      long hour = dateDiff % nd / nh;
      // 计算差多少分钟
      long min = dateDiff % nd % nh / nm;
      // 计算差多少秒//输出结果
      long sec = dateDiff % nd % nh % nm / ns;

      if(day != 0){
        msg += day + context.getString(R.string.date_util_time_day_ex);
      }
      if(hour != 0){
        msg += hour + context.getString(R.string.date_util_time_hour_ex);
        if(day != 0){
          return msg;
        }
      }
      if(min != 0){
        msg += min + context.getString(R.string.date_util_time_min_ex);
      }
    }
    return msg;
  }


  /**
   * 由过去的某一时间,计算距离当前的时间
   */
  public static String CalculateTimeEx(Context context, String time) {
    if(time == null){
      return"";
    }
    if(time.isEmpty()){
      return"";
    }
    long nowTime = System.currentTimeMillis(); // 获取当前时间的毫秒数
    String msg = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 指定时间格式
    Date setTime = null; // 指定时间
    try {
      setTime = sdf.parse(time); // 将字符串转换为指定的时间格式
    } catch (ParseException e) {

      e.printStackTrace();
    }
    long reset = setTime.getTime(); // 获取指定时间的毫秒数
    long dateDiff = nowTime - reset;
    if (dateDiff < 0) {
      //msg += context.getString(R.string.date_util_time_wrong);
      msg  = "";
    } else {
      long nd = 1000 * 24 * 60 * 60;
      long nh = 1000 * 60 * 60;
      long nm = 1000 * 60;
      long ns = 1000;
      // long ns = 1000;
      // 计算差多少天
      long day = dateDiff / nd;
      // 计算差多少小时
      long hour = dateDiff % nd / nh;
      // 计算差多少分钟
      long min = dateDiff % nd % nh / nm;
      // 计算差多少秒//输出结果
      long sec = dateDiff % nd % nh % nm / ns;

      if(day != 0){
        msg += day + context.getString(R.string.date_util_time_day_ex);
      }
      if(hour != 0){
        msg += hour + context.getString(R.string.date_util_time_hour_ex)
                +min + context.getString(R.string.date_util_time_min_ex)
                +sec + context.getString(R.string.date_util_time_sec_ex);
      }
    }
    return msg;
  }

  /**
   * 由过去的某一时间,计算距离当前的时间 秒数
   */
  public static long CalculateSecond(Context context, String time) {
    if(time == null){
      return 0;
    }
    if(time.isEmpty()){
      return 0;
    }
    long nowTime = System.currentTimeMillis(); // 获取当前时间的毫秒数
    long second = 0;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 指定时间格式
    Date setTime = null; // 指定时间
    try {
      setTime = sdf.parse(time); // 将字符串转换为指定的时间格式
    } catch (ParseException e) {

      e.printStackTrace();
    }
    long reset = setTime.getTime(); // 获取指定时间的毫秒数
    long dateDiff = nowTime - reset;
    if (dateDiff < 0) {
      second = 0 ;
    } else {
      second = dateDiff / 1000;
    }
    return second;
  }

  /*
   * 将时间转换为时间戳 long
   */
  public static long dateToStamp(String time){
//		String res;
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = simpleDateFormat.parse(time);
      long ts = date.getTime();
      return  ts;
    }catch (ParseException e) {
      e.printStackTrace();
      return 0;
    }


//		res = String.valueOf(ts);
//		return res;
  }

  /*
   * 将时间转换为时间戳 long
   */
  public static long dateToStampEx(String time) throws ParseException{
//		String res;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    Date date = simpleDateFormat.parse(time);
    long ts = date.getTime();
    return  ts;
//		res = String.valueOf(ts);
//		return res;
  }

  /*
   * 规范时间
   */

  public static String getCanonicalTime() {
    SimpleDateFormat utcDayFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat utcHourFormat = new SimpleDateFormat("hh:mm:ss");
    utcDayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    utcHourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date now = new Date();
    return String.format("%sT%sZ", utcDayFormat.format(now), utcHourFormat.format(now));
  }

  /**
   * 两个时间差值秒
   * @Title: compareStrLongTime
   * @Description: TODO
   * @param: date1
   * @param: date2
   * @return: boolean
   * @throws:
   * long diff = date1.getTime() - date2.getTime();
   * long days = diff / (1000 * 60 * 60 * 24);
   * long hours = (diff-days*(1000 * 60 * 60 * 24))/(1000* 60 * 60);
   * long minutes = (diff-days*(1000 * 60 * 60 * 24)-hours*(1000* 60 * 60))/(1000* 60);
   * System.out.println(""+days+"天"+hours+"小时bai"+minutes+"分");
   */
  public static long diffStrLongTimeSec(String time1, String time2){
    if(time1 == null && time2 == null){
      return 0;
    }
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      Date date1 = dfs.parse(time1);
      Date date2 = dfs.parse(time2);
      long diff = date1.getTime() - date2.getTime();//微秒级别
      return diff / 1000;
    } catch (ParseException e) {
      e.printStackTrace();
      return 0;
    }
  }

  public static long diffStrLongTimeDay(String time1, String time2){
    SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date date1 = dfs.parse(time1);
      Date date2 = dfs.parse(time2);
      long diff = date1.getTime() - date2.getTime();//微秒级别
      if(diff > 0){
        return diff /  (24 * 60 * 60 * 1000);
      }
      return 0;
    } catch (ParseException e) {
      e.printStackTrace();
      return 0;
    }
  }


  /* time 毫秒
   *	格式化时间hh:mm:ss
   */
  public static String formatMileTime(int time){
    // hh:mm:ss
    int hour = 60*60*1000;
    int minute = 60*1000;
    int second = 1000;
    int h = time/hour;
    int m = time%hour/minute;
    int s = time%minute/second;
    return String.format("%02d:%02d:%02d",h,m,s);
  }


  /**
   * 日期格式转换yyyy-MM-dd'T'HH:mm:ss.SSSXXX  (yyyy-MM-dd'T'HH:mm:ss.SSSZ) TO  yyyy-MM-dd HH:mm:ss
   * @throws ParseException
   */
  public static String dealDateFormat(String oldDateStr) throws ParseException{
    //此格式只有  jdk 1.7才支持  yyyy-MM-dd'T'HH:mm:ss.SSSXXX

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    Date date = formatter.parse(oldDateStr);
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String sf =sdf.format(date);
    return sf;
//		System.out.println(sDate);

//		String[] strs = oldDateStr.split("T");
//		String bb = strs[0];
//		String aa = strs[1];
//		String[] strs1 = aa.split("+");
//		return "";
//		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm z");  //yyyy-MM-dd'T'HH:mm:ss.SSSZ
//		Date  date = df.parse(oldDateStr);
//		SimpleDateFormat df1 = new SimpleDateFormat ("EEE MMM dd HH:mm:ss Z yyyy", Locale.UK);
//		Date date1 =  df1.parse(date.toString());
//		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		return df2.format(date1);
  }


}
