package com.mrzhou5.tools.clock.activity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mrzhou5.tools.clock.R;
import com.mrzhou5.tools.clock.application.CheckInApp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MaintenceInfoActivity extends BaseActivity {
    TextView timeStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintence_info_max);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timeStr = findViewById(R.id.timeStr);
        timeStr.setTypeface(CheckInApp.getOtfPingfangSimpleRoutine());
        setSystemUIVisible(!CheckInApp.getKeepAppFront());
        new Thread(()-> {
            try {
                while (true) {
                    Date date = new Date();
                    runOnUiThread(() -> {
                        timeStr.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date));
                    });
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onResume() {
        setSystemUIVisible(!CheckInApp.getKeepAppFront());
        super.onResume();
    }

    private void setSystemUIVisible(boolean show) {
        if (show) {
            int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiFlags |= 0x00001000;
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        } else {
            int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiFlags |= 0x00001000;
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        }
    }
}
