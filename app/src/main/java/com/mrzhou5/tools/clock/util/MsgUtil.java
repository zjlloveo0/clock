package com.mrzhou5.tools.clock.util;

import android.util.Log;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;

public class MsgUtil {
    private final static String TAG = MsgUtil.class.getSimpleName();
    private final static String url = "https://sc.ftqq.com/SCU90163T0f9b49c3c9bdcdf8e1e9dd242c9067875e72ca1fb7e4a.send";

    public static void send(final String title, final String msg) {
        Runnable runnable = () -> {
            String body = HttpRequest.post(url)
                    .form("text", title)
                    .form("desp", msg)
                    .execute()
                    .body();
            if (null != body && !"".equals(body)) {
                JSONObject json = new JSONObject(body);
                if ("0".equals(json.getStr("errno"))) {
                    return;
                }
            }
            Log.e(TAG, "消息发送失败：" + title);
        };
        new Thread(runnable).start();
    }
}
