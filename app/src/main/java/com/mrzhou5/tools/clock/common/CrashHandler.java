package com.mrzhou5.tools.clock.common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.util.StringUtil;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    //重新启动应用程序指定Activity间隔时间(毫秒)
    private final static long SLEEPTIME_RESTART_ACTIVITY = 10000;

    private static CrashHandler instance;
    private final static Class<MaintenceInfoActivity> RESTART_ACTIVITY = MaintenceInfoActivity.class;

    private CrashHandler(){}

    public static CrashHandler getInstance( ){
        if (instance == null){
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    public void init(Context ctx){  //初始化，把当前对象设置成UncaughtExceptionHandler处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {  //当有未处理的异常发生时执行此函数

        restartActivity(RESTART_ACTIVITY , ex);

        Log.d(CrashHandler.class.getSimpleName(), StringUtil.getExceptionMessage(new Exception(ex)));

        //应用已经崩溃, 需要先终止当前的应用线程. 否则会ANR
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    /**
     * 重新启动应用程序
     * @param activityClass 要启动的Activity
     */
    public static void restartActivity(Class<?> activityClass , Throwable throwable){

        //创建用于启动的 Intent , 与对应的数据
        Intent intent = new Intent(CheckInApp.getInstance().getApplicationContext(),activityClass);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                CheckInApp.getInstance().getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );

        //获取闹钟管理器 , 用于定时执行我们的启动任务
        AlarmManager mgr = (AlarmManager) CheckInApp.getInstance().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        //设置执行PendingIntent的时间是当前时间+SLEEPTIME_RESTART_ACTIVITY 参数的值
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + SLEEPTIME_RESTART_ACTIVITY , pendingIntent);
    }
}
