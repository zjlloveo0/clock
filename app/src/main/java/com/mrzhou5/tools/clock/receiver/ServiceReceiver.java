package com.mrzhou5.tools.clock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.service.LocalService;
import com.mrzhou5.tools.clock.util.MsgUtil;
import com.mrzhou5.tools.clock.videoRecorder.VideoRecService;

public class ServiceReceiver extends BroadcastReceiver {
    public static final String TAG = ServiceReceiver.class.getSimpleName();
    public static Long time = 0L;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("RESTART_SERVICE")){
            if(System.currentTimeMillis()-time < 10000){
                Log.d(TAG, "onReceive: 重启间隔太短");
                return;
            }
            time = System.currentTimeMillis();
            Log.d(TAG, "onReceive: 重启录像服务");
            MsgUtil.send("重启录像服务",""+System.currentTimeMillis());
            Intent videoIntent = new Intent(MaintenceInfoActivity.getInstance(), VideoRecService.class);
            MaintenceInfoActivity.getInstance().stopService(videoIntent);
            Intent videoIntent2 = new Intent(MaintenceInfoActivity.getInstance(), VideoRecService.class);
            MaintenceInfoActivity.getInstance().startService(videoIntent2);
        }
    }
}
