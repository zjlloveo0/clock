package com.mrzhou5.tools.clock.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends Activity {

    public static final String START_FROM_PAUSED_ACTIVITY_FLAG = "START_FROM_PAUSED_ACTIVITY_FLAG";

    protected boolean paused = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 防止在维护的时候，用户触碰返回键导致退出当前系统流程
            // 设置画面按下返回键的时候不关闭当前画面，跳转至系统主画面
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bitmap bp = null;

        try {
            // 画面退出时，获取全部的子控件，并且清除上面的图片
            List<View> list = getAllChildViews(getWindow().getDecorView());

            for (View view : list) {
                if (null != view) {
                    try {
                        view.setDrawingCacheEnabled(true);
                        bp = view.getDrawingCache();
                        if (null != bp && !bp.isRecycled()) {
                            bp.recycle();
                        }
                        view.setBackgroundResource(0);
                        view.setDrawingCacheEnabled(false);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<View> getAllChildViews(View view) {
        List<View> allchildren = new ArrayList<View>();
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View viewchild = vp.getChildAt(i);
                allchildren.add(viewchild);
                //再次 调用本身（递归）
                allchildren.addAll(getAllChildViews(viewchild));
            }
        }
        return allchildren;
    }
}
