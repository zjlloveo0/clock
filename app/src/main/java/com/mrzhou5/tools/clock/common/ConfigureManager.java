package com.mrzhou5.tools.clock.common;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.Properties;

/**
 * sd卡上配置文件读取类
 *
 * @author zjl
 * @date 2018/10/16.
 */
public class ConfigureManager {
    private final static String TAG = ConfigureManager.class.getSimpleName();
    private final static String PROPERTY_FILE_NAME = "clock.property";
    private final static String START_H = "START_H";
    private final static String START_M = "START_M";
    private final static String END_H = "END_H";
    private final static String END_M = "END_M";
    private final static String LIMIT = "LIMIT";

    private Integer startH = 8;
    private Integer startM = 0;
    private Integer endH = 19;
    private Integer endM = 0;
    private Integer limit = 10;
    private Application application;

    private static volatile ConfigureManager instance = null;
    private Properties properties = null;

    private ConfigureManager(Application app) {
        try {
            application = app;
            properties = new Properties();
            try {
                File file = new File(getCongfigureFilePath());
                if (!file.exists()) {
                    file.createNewFile();
                    updateProperties(START_H, startH + "");
                    updateProperties(START_M, startM + "");
                    updateProperties(END_H, endH + "");
                    updateProperties(END_M, endM + "");
                    updateProperties(LIMIT, limit + "");
                    return;
                }
                FileInputStream in = new FileInputStream(file);
                properties.load(new InputStreamReader(in, "UTF-8"));
            } catch (IOException e) {
                Log.d(TAG, StringUtil.getExceptionMessage(e));
            }
            if (StringUtil.IsNumberic(properties.getProperty(START_H))) {
                startH = Integer.parseInt(properties.getProperty(START_H));
            }
            if (StringUtil.IsNumberic(properties.getProperty(START_M))) {
                startM = Integer.parseInt(properties.getProperty(START_M));
            }
            if (StringUtil.IsNumberic(properties.getProperty(END_H))) {
                endH = Integer.parseInt(properties.getProperty(END_H));
            }
            if (StringUtil.IsNumberic(properties.getProperty(END_M))) {
                endM = Integer.parseInt(properties.getProperty(END_M));
            }
            if (StringUtil.IsNumberic(properties.getProperty(LIMIT))) {
                limit = Integer.parseInt(properties.getProperty(LIMIT));
            }
        } catch (Exception e) {
            Log.d(TAG, StringUtil.getExceptionMessage(e));
        }
    }

    /**
     * 更新properties文件的键值对
     * 如果该主键已经存在，更新该主键的值；
     * 如果该主键不存在，则插件一对键值。
     *
     * @param keyname  键名
     * @param keyvalue 键值
     */
    private boolean updateProperties(String keyname, String keyvalue) {
        boolean isSucceed = true;
        OutputStream fos = null;
        OutputStreamWriter fosw = null;
        try {
            // 调用 Hashtable 的方法 put，使用 getProperty 方法提供并行性。
            // 强制要求为属性的键和值使用字符串。返回值是 Hashtable 调用 put 的结果。
            fos = new FileOutputStream(getCongfigureFilePath());
            properties.setProperty(keyname, keyvalue);
            // 以适合使用 load 方法加载到 Properties 表中的格式，
            // 将此 Properties 表中的属性列表（键和元素对）写入输出流
            fosw = new OutputStreamWriter(fos, "utf-8");
            properties.store(fosw, "Property update");

        } catch (Exception e) {
            Log.e(TAG, "Fail to update property file");
            Log.e(TAG, StringUtil.getExceptionMessage(e));
            isSucceed = false;
        } finally {
            try {
                fos.close();
                fosw.close();
            } catch (IOException e) {
                Log.d("流关闭异常", e.toString());
            }
        }

        return isSucceed;
    }

    /**
     * 重载配置文件
     */
    public ConfigureManager reloadProperties() {
        synchronized (ConfigureManager.class) {
            instance = new ConfigureManager(CheckInApp.getInstance());
        }
        return instance;
    }

    public static ConfigureManager getInstance() {
        if (null == instance) {
            synchronized (ConfigureManager.class) {
                if (null == instance) {
                    instance = new ConfigureManager(CheckInApp.getInstance());
                }
            }
        }
        return instance;
    }

    public Integer getStartH() {
        return startH;
    }

    public Integer getStartM() {
        return startM;
    }

    public Integer getEndH() {
        return endH;
    }

    public Integer getEndM() {
        return endM;
    }

    public Integer getLimit() {
        return limit;
    }

    public boolean setLimit(Integer limit) {
        boolean isSucceed = true;
        if (!Objects.equals(this.limit, limit)) {
            this.limit = limit;
            isSucceed = updateProperties(LIMIT, limit + "");
        }
        return isSucceed;
    }

    public boolean setStartH(Integer startH) {
        boolean isSucceed = true;
        if (!Objects.equals(this.startH, startH)) {
            this.startH = startH;
            isSucceed = updateProperties(START_H, startH + "");
        }
        return isSucceed;
    }

    public boolean setStartM(Integer startM) {
        boolean isSucceed = true;
        if (!Objects.equals(this.startM, startM)) {
            this.startM = startM;
            isSucceed = updateProperties(START_M, startM + "");
        }
        return isSucceed;
    }

    public boolean setEndH(Integer endH) {
        boolean isSucceed = true;
        if (!Objects.equals(this.endH, endH)) {
            this.endH = endH;
            isSucceed = updateProperties(END_H, endH + "");
        }
        return isSucceed;
    }

    public boolean setEndM(Integer endM) {
        boolean isSucceed = true;
        if (!Objects.equals(this.endM, endM)) {
            this.endM = endM;
            isSucceed = updateProperties(END_M, endM + "");
        }
        return isSucceed;
    }

    /**
     * 获取当前APP的配置文件路径（SD卡上固定死的一个目录）
     *
     * @return
     */
    private static String getCongfigureFilePath() {
        String pathString = "";
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {// 外部存储卡
                pathString = Environment.getExternalStorageDirectory().getPath();
                pathString = pathString + File.separator + Constant.DIR_NAME + File.separator + PROPERTY_FILE_NAME;
            }
        } catch (Exception e) {
            Log.d(ConfigureManager.class.getSimpleName(), StringUtil.getExceptionMessage(e));
            pathString = "";
        }

        return pathString;
    }
}
