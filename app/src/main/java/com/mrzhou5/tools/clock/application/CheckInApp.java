package com.mrzhou5.tools.clock.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.mrzhou5.tools.clock.common.ConfigureManager;
import com.mrzhou5.tools.clock.common.Constant;
import com.mrzhou5.tools.clock.common.CrashHandler;
import com.mrzhou5.tools.clock.service.LocalService;
import com.mrzhou5.tools.clock.util.AdsysUtil;
import com.mrzhou5.tools.clock.util.MsgUtil;
import com.mrzhou5.tools.clock.util.StringUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckInApp extends Application {
    private static final String TAG = CheckInApp.class.getSimpleName();
    private static CheckInApp instance = null;

    private int lastDay = 0;
    private int random = 0;
    private int amHH;
    private int amMM;
    private int pmHH;
    private int pmMM;
    /**
     * 是否需要保持APP前台显示，若值设置为true则保持前台显示，否则不保持前台显示
     * 默认保持前台显示
     */
    private static final AtomicBoolean keepAppFront = new AtomicBoolean(true);
    private static final AtomicBoolean isMaintence = new AtomicBoolean(false);

    private final BroadcastReceiver reciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_TIME_TICK:
                    daka();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    MsgUtil.send("锁屏", new Date().toLocaleString());
                    break;
            }
        }
    };

    public void daka() {
        ConfigureManager config = ConfigureManager.getInstance();
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String msg = month + "月" + day + "日打卡时间：上班卡-" + amHH + ":" + amMM + "，下班卡-" + pmHH + ":" + pmMM;
        if (lastDay != day) {
            random = new Random().nextInt(20) - config.getLimit();
            int startM = config.getStartM() + random;
            if (startM > 59) {
                amHH = config.getStartH() + 1;
                amMM = startM - 60;
            } else if (startM < 0) {
                amHH = config.getStartH() - 1;
                amMM = startM + 60;
            } else {
                amHH = config.getStartH();
                amMM = startM;
            }
            random = new Random().nextInt(20) - config.getLimit();
            int endM = config.getEndM() + random;
            if (endM > 59) {
                pmHH = config.getEndH() + 1;
                pmMM = endM - 60;
            } else if (endM < 0) {
                pmHH = config.getStartH() - 1;
                pmMM = endM + 60;
            } else {
                pmHH = config.getEndH();
                pmMM = endM;
            }
            if (amMM > 59 || amMM < 0) {
                amHH = config.getStartH();
                amMM = config.getStartM();
            }
            if (pmMM > 59 || pmMM < 0) {
                pmHH = config.getEndH();
                pmMM = config.getEndM();
            }
            msg = month + "月" + day + "日打卡时间：上班卡-" + amHH + ":" + amMM + "，下班卡-" + pmHH + ":" + pmMM;
            Log.d(TAG, msg);
            if (lastDay == 0) {
                MsgUtil.send(amHH + "-" + amMM + "打卡准备" + pmHH + "-" + pmMM, msg);
            }
            lastDay = day;
        }
        if ((hour == 7 && minute == 0) || (hour == 17 && minute == 30)) {
            MsgUtil.send(amHH + "-" + amMM + "打卡准备" + pmHH + "-" + pmMM, msg);
        }
        if ((hour == amHH && minute >= amMM) || (hour == pmHH && minute >= pmMM)) {
            if (CheckInApp.getKeepAppFront()) {
                MsgUtil.send("开始打卡:" + hour + "-" + minute, "");
            }
            CheckInApp.setKeepAppFront(false);
            return;
        }
        if (!CheckInApp.getIsMaintence()&&(hour <= 9 || hour >= 20)) {
            CheckInApp.setKeepAppFront(true);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "get in oncreate");

        //为了防止app异常崩溃，在Appliction里面设置我们的异常处理器为UncaughtExceptionHandler处理器
        String process = AdsysUtil.getProcessName(this.getApplicationContext(), Process.myPid());
        CrashHandler handler = CrashHandler.getInstance();
        handler.init(getApplicationContext());

        instance = this;

        // 开启守护进程，由于守护进行是单独的线程，因此开启后的代码会在两个线程里面分别执行
        // 如果需要有代码一定在下面执行，则按照进程名选择在哪个进程里面进行执行
        Intent intent = new Intent(this, LocalService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        if (null != process && process.contains("updater_servant")) {
            Log.d(CheckInApp.class.getSimpleName(), "守护线程!!!");
        } else {
            Log.d(CheckInApp.class.getSimpleName(), "主线程!!!");
            ConfigureManager config = ConfigureManager.getInstance();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            registerReceiver(reciver, filter);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(reciver, filter);
        }
    }

    /**
     * 判断app是否处于运行中
     *
     * @return
     */
    public static boolean isRunScApp() {
        ActivityManager am = (ActivityManager) CheckInApp.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        boolean isAppRunning = false;
        //100表示取的最大的任务数，info.topActivity表示当前正在运行的Activity，info.baseActivity表系统后台有此进程在运行
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(Constant.PACK_NAME) || info.baseActivity.getPackageName().equals(Constant.PACK_NAME)) {
                isAppRunning = true;
                Log.i(TAG, info.topActivity.getPackageName() + " info.baseActivity.getPackageName()=" + info.baseActivity.getPackageName());
                break;
            }
        }
        Log.i(TAG, "APP运行状态：" + isAppRunning);
        return isAppRunning;
    }

    /**
     * 判断App是否已安装
     *
     * @return
     */
    public static boolean isExistScApp() {
        boolean isExisted = false;
        try {
            //获取packagemanager
            final PackageManager packageManager = CheckInApp.getInstance().getApplicationContext().getPackageManager();
            // 获取所有已安装程序的包信息
            List<PackageInfo> info = packageManager.getInstalledPackages(0);
            if (info == null || info.isEmpty()) {
                isExisted = false;
            } else {
                for (int i = 0; i < info.size(); i++) {
                    if (Constant.PACK_NAME.equals(info.get(i).packageName)) {
                        isExisted = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            isExisted = false;
            Log.d(CheckInApp.class.getSimpleName(), StringUtil.getExceptionMessage(e));
        }
        return isExisted;
    }

    public static boolean isServiceRunning(String servicename, Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> infos = am.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo info : infos) {
            if (servicename.equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开App
     */
    public static void openScApp() {
        Intent intent = CheckInApp.getInstance().getPackageManager().getLaunchIntentForPackage(Constant.PACK_NAME);
        if (intent != null) {
            MsgUtil.send("打开APP", Constant.PACK_NAME);
            CheckInApp.getInstance().startActivity(intent);
        }
    }

    /**
     * 关闭App
     */
    public static void closeScApp() {
//        MsgUtil.send("关闭APP", Constant.PACK_NAME);
        ActivityManager am = (ActivityManager) CheckInApp.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(Constant.PACK_NAME);
    }

    public static CheckInApp getInstance() {
        return instance;
    }

    public static boolean getKeepAppFront() {
        return keepAppFront.get();
    }

    public static void setKeepAppFront(boolean isMaintence) {
        CheckInApp.keepAppFront.set(isMaintence);
    }

    public static boolean getIsMaintence() {
        return isMaintence.get();
    }

    public static void setIsMaintence(boolean isMaintence) {
        CheckInApp.isMaintence.set(isMaintence);
    }
}
