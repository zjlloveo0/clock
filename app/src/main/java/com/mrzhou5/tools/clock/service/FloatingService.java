package com.mrzhou5.tools.clock.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mrzhou5.tools.clock.R;
import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.common.ConfigureManager;
import com.mrzhou5.tools.clock.util.MsgUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class FloatingService extends Service {
    public static final String TAG = FloatingService.class.getSimpleName();
    public final static AtomicInteger maintenceTimes = new AtomicInteger(1);
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private TextView timeStr;
    private View view;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("InflateParams")
    private void showFloatingWindow() {
        MaintenceInfoActivity.atomicIsStart.set(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            // 获取WindowManager服务
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            // 设置LayoutParam
            layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            layoutParams.format = PixelFormat.RGBA_8888;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            //宽高自适应
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            //显示的位置
            layoutParams.x = 0;
            layoutParams.y = 0;
            layoutParams.flags = layoutParams.flags | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

            // 新建悬浮窗控件
            view = LayoutInflater.from(this).inflate(R.layout.test_layout, null);
            timeStr = view.findViewById(R.id.timeStr2);
            timeStr.setOnClickListener(v -> {
                Log.d(TAG, "maintenceTimes: " + maintenceTimes);
                if (maintenceTimes.getAndAdd(1) >= 5) {
                    maintenceTimes.set(1);
                    CheckInApp.setIsMaintence(!CheckInApp.getIsMaintence());
                    if (CheckInApp.getIsMaintence()) {
                        MsgUtil.send("打卡器维护中", "打卡器维护中");
                        timeStr.setTextColor(Color.BLUE);
                        timeStr.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        windowManager.removeViewImmediate(view);
                        windowManager.addView(view, layoutParams);
                    } else {
                        MsgUtil.send("打卡器已取消维护", "打卡器已取消维护");
                        ConfigureManager.getInstance().reloadProperties();
                        timeStr.setTextColor(Color.WHITE);
                        timeStr.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
                        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                        windowManager.removeViewImmediate(view);
                        windowManager.addView(view, layoutParams);
                    }
                }
            });
            view.setOnTouchListener(new FloatingOnTouchListener());
            // 将悬浮窗控件添加到WindowManager
            windowManager.addView(view, layoutParams);
            new Thread(() -> {
                try {
                    while (true) {
                        Date date = new Date();
                        timeStr.post(() -> {
                            timeStr.setText(simpleDateFormat.format(date));
                            if (!CheckInApp.getIsMaintence()) {
                                if (CheckInApp.getKeepAppFront()) {
                                    timeStr.setTextColor(Color.WHITE);
                                } else {
                                    timeStr.setTextColor(Color.YELLOW);
                                }
                            }
                        });
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        windowManager.removeViewImmediate(view);
        MaintenceInfoActivity.atomicIsStart.set(false);
    }
}