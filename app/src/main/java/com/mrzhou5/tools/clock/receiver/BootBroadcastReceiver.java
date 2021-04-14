package com.mrzhou5.tools.clock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mrzhou5.tools.clock.activity.BaseActivity;
import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;

/**
 * @author zjl
 * @date 2018/10/29.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //启动Service
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)||
                intent.getAction().equals("android.media.AUDIO_BECOMING_NOISY") ){
            Log.e("","BootBroadcastReceiver AppServic. start");
//            Intent serviceIntent = new Intent(context, AppService.class);
//            context.startService(serviceIntent);
            Intent newIntent = new Intent(context,MaintenceInfoActivity.class);
            newIntent.setAction("android.intent.action.MAIN");
            newIntent.addCategory("android.intent.category.LAUNCHER");
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //注意，必须添加这个标记，否则启动会失败
            intent.putExtra(BaseActivity.START_FROM_PAUSED_ACTIVITY_FLAG, false);
            context.startActivity(newIntent);
        }else if(intent.getAction().equals("STOPACTION")){
            Log.d(BootBroadcastReceiver.class.getSimpleName(), "Revieved stopaction broadcast and begin to disconnect the sockets");
        }
    }
}
