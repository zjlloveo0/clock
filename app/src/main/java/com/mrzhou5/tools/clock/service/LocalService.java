package com.mrzhou5.tools.clock.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.mrzhou5.tools.clock.R;
import com.mrzhou5.tools.clock.activity.BaseActivity;
import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.videoRecorder.VideoRecService;

import java.util.List;

import clock.service.IMyAidlInterface;


/**
 * 本地保活服务，用于和远程服务相互保活，并保证APP始终在前端
 *
 * @author tao-tengtao
 * @date 2018/10/31.
 */
public class LocalService extends Service implements Runnable {
    private static final String TAG = LocalService.class.getSimpleName();

    private MyBinder mBinder;

    private static final int THREAD_SHOW_TIME = 10000;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IMyAidlInterface iMyAidlInterface = IMyAidlInterface.Stub.asInterface(service);
            try {
                Log.i(TAG, "服务连接：" + iMyAidlInterface.getServiceName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "连接断开，重新启动 RemoteServantService");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(LocalService.this, RemoteServantService.class));
            } else {
                startService(new Intent(LocalService.this, RemoteServantService.class));
            }
            bindService(new Intent(LocalService.this, RemoteServantService.class), connection, Context.BIND_IMPORTANT);
        }
    };

    public LocalService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "LocalService onCreate");
        new Thread(this).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LocalService.class.getSimpleName(), "LocalService 启动");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(LocalService.this, RemoteServantService.class));
        } else {
            startService(new Intent(LocalService.this, RemoteServantService.class));
        }
//        // 关闭服务
//        stopSelf(startId);
        try {
            bindService(new Intent(this, RemoteServantService.class), connection, Context.BIND_IMPORTANT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBinder = new MyBinder();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "本地保活服务关闭", Toast.LENGTH_SHORT).show();
        stopForeground(false);
        unbindService(connection);
    }

    private class MyBinder extends IMyAidlInterface.Stub {

        @Override
        public String getServiceName() throws RemoteException {
            return LocalService.class.getName();
        }

    }

    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // 通知渠道的id
        String id = "my_channel_03";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = getString(R.string.channel_name);
//         用户可以看到的通知渠道的描述
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
//         配置通知渠道的属性
        mChannel.setDescription(description);
//         设置通知出现时的闪灯（如果 android 设备支持的话）
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
//         设置通知出现时的震动（如果 android 设备支持的话）
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
//         最后在notificationmanager中创建该通知渠道 //
        mNotificationManager.createNotificationChannel(mChannel);

        // 为该通知设置一个id
        int notifyID = 1;
        // 通知渠道的id
        // Create a notification and set the notification channel.
        Notification notification = new Notification.Builder(this, id)
                .setContentTitle("时钟服务").setContentText("Local服务正在后台运行")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .build();
        startForeground(notifyID, notification);
    }


    @Override
    public void run() {
        while (true) {
            FloatingService.maintenceTimes.set(1);
            if (CheckInApp.getIsMaintence()) {
                Log.d(TAG, "维护中...");
            } else {
                if (CheckInApp.isExistScApp()) {
                    if (CheckInApp.getKeepAppFront()) {
                        CheckInApp.closeScApp();
                    } else {
                        CheckInApp.openScApp();
                    }
                }
                if (CheckInApp.getKeepAppFront()) {
                    toFront();
                }
                if(!CheckInApp.isServiceRunning(VideoRecService.class.getName(), CheckInApp.getInstance())){
                    Log.d(TAG, "run: 录像服务未启动");
                    MaintenceInfoActivity.getInstance().startVideoRecorder();
                }
            }
            try {
                Thread.sleep(THREAD_SHOW_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void toFront() {
        //获取ActivityManager
        ActivityManager mAm = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        //获得当前运行的task
        List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
        if (null != taskList) {
            for (ActivityManager.RunningTaskInfo rti : taskList) {
                //找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
                if (rti.topActivity.getPackageName().equals(getPackageName())) {
                    mAm.moveTaskToFront(rti.id, 0);
                    return;
                }
            }
            //若没有找到运行的task，用户结束了task或被系统释放，则重新启动mainactivity
            Intent resultIntent = new Intent(this, MaintenceInfoActivity.class);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");
            resultIntent.putExtra(BaseActivity.START_FROM_PAUSED_ACTIVITY_FLAG, false);
            startActivity(resultIntent);
        }
    }
}
