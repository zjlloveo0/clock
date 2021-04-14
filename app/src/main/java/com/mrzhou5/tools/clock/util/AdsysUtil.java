package com.mrzhou5.tools.clock.util;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.PowerManager;

import java.util.List;

/**
 * @author zjl
 * @date 2018/11/12.
 */
public class AdsysUtil {
    public static boolean isRunningOnForeground(Context context) {

        ActivityManager acm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (acm != null) {
            List<ActivityManager.RunningAppProcessInfo> runApps = acm.getRunningAppProcesses();
            if (runApps != null && !runApps.isEmpty()) {
                for (ActivityManager.RunningAppProcessInfo app : runApps) {
                    if (app.processName.equals(context.getPackageName())) {
                        if (app.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    /**
     * 重启当前的安卓系统
     * 需要android.uid.system权限
     * @return
     */
    public static void rebootSystem(Application app) {
        PowerManager powerManager = (PowerManager)app.getSystemService(Context.POWER_SERVICE);
        powerManager.reboot("手动重启");
    }
}
