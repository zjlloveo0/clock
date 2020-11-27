package com.mrzhou5.tools.clock.service;

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

import clock.service.IMyAidlInterface;

/**
 * 远程保活服务，用于和LocalService相互监视保活
 *
 * @author tao-tengtao
 * @date 2018/10/31.
 */
public class RemoteServantService extends Service {

    private MyBinder mBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IMyAidlInterface iMyAidlInterface = IMyAidlInterface.Stub.asInterface(service);
            try {
                Log.i(RemoteServantService.class.getSimpleName(), "connected with " + iMyAidlInterface.getServiceName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(RemoteServantService.class.getSimpleName(), "链接断开，重新启动 LocalService");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(RemoteServantService.this, LocalService.class));
            } else {
                startService(new Intent(RemoteServantService.this, LocalService.class));
            }
            bindService(new Intent(RemoteServantService.this, LocalService.class), connection, Context.BIND_IMPORTANT);
        }
    };

    public RemoteServantService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(RemoteServantService.class.getSimpleName(), "RemoteServantService 启动");

        bindService(new Intent(this, LocalService.class), connection, Context.BIND_IMPORTANT);
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
        Toast.makeText(this, "远程保活服务关闭", Toast.LENGTH_SHORT).show();
        stopForeground(false);
        unbindService(connection);
    }

    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = "my_channel_02";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = getString(R.string.channel_name2);
//         用户可以看到的通知渠道的描述
        String description = getString(R.string.channel_description2);
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
        int notifyID = 2;
        // 通知渠道的id
        // Create a notification and set the notification channel.
        Notification notification = new Notification.Builder(this)
                .setContentTitle("时钟服务").setContentText("Remote服务正在后台运行")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setChannelId(id)
                .build();
        startForeground(notifyID, notification);
    }

    private class MyBinder extends IMyAidlInterface.Stub {

        @Override
        public String getServiceName() throws RemoteException {
            return RemoteServantService.class.getName();
        }
    }
}
