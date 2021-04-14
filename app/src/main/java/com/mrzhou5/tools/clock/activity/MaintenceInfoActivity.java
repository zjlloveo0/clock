package com.mrzhou5.tools.clock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mrzhou5.tools.clock.R;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.service.FloatingService;
import com.mrzhou5.tools.clock.util.PermissionsUtils;
import com.mrzhou5.tools.clock.videoRecorder.VideoRecService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MaintenceInfoActivity extends BaseActivity {
    private final static String TAG = MaintenceInfoActivity.class.getSimpleName();
    private TextView timeStr;
    public static AtomicBoolean atomicIsStart = new AtomicBoolean(false);
    public static AtomicBoolean atomicIsStartVideo = new AtomicBoolean(false);
    private static MaintenceInfoActivity instance = null;
    private Intent floatService;
    //创建监听权限的接口对象
    PermissionsUtils.IPermissionsResult permissionsResult = new PermissionsUtils.IPermissionsResult() {
        @Override
        public void passPermissons() {
            Toast.makeText(MaintenceInfoActivity.this, "权限通过!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void forbitPermissons() {
            Toast.makeText(MaintenceInfoActivity.this, "权限不通过!", Toast.LENGTH_SHORT).show();
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //就多一个参数this
        PermissionsUtils.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintence_info_max);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        floatService = new Intent(MaintenceInfoActivity.this, FloatingService.class);
        timeStr = findViewById(R.id.timeStr);
        timeStr.setOnClickListener(v -> {
            if (!atomicIsStart.get()) {
                startService(floatService);
            }
            if (!atomicIsStartVideo.get()) {
                startVideoRecorder();
            }
        });

        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        PermissionsUtils.getInstance().chekPermissions(this, permissions, permissionsResult);
        setSystemUIVisible(!CheckInApp.getKeepAppFront());
        startFloatingService();
        instance = this;
    }
    @SuppressLint("ShowToast")
    public void startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
            return;
        }
        if (!atomicIsStart.get()) {
            startService(floatService);
        }
        if (!atomicIsStartVideo.get()) {
            startVideoRecorder();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                if (!atomicIsStart.get()) {
                    startService(floatService);
                }
                if (!atomicIsStartVideo.get()) {
                    startVideoRecorder();
                }
            }
        }
    }
    public void startVideoRecorder(){
        Intent videoIntent = new Intent(this, VideoRecService.class);
        startService(videoIntent);
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

    public static MaintenceInfoActivity getInstance() {
        return instance;
    }
}
