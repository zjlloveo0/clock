package com.mrzhou5.tools.clock.util;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private final static String TAG = FileUtil.class.getSimpleName();
    private final static String pathname = "/sdcard";
    /**
     * 获取当前工控机上插入的U盘路径
     * @return U盘在系统内的绝对路径。若U盘未插入，或执行异常，则返回空字符串
     */
    public static String getUsbRootPath() {
        return pathname;
    }
    /**
     * 获取录像存储地址
     * @return
     */
    public static String getVideoStorageRootPath() {
        File file = new File(pathname);
        if (file.exists()) {
            File fileSc = new File(pathname + File.separator + "video");
            if (fileSc.exists()) {
                return pathname+ File.separator + "video";
            } else {
                if(fileSc.mkdir()){
                    return pathname+ File.separator + "video";
                }
            }
        }
        return "";
    }

    public static String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if(sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    /**
     * 比较文件路径
     * @param fileName
     * @return
     */
    public static boolean  getFilePath( String fileName) {

        try {
            if(null!=fileName){
                //获取根路径
                String absolutePath = Environment.getDataDirectory().getAbsolutePath();
                //文件路径
                String Path =absolutePath+fileName;
                //获取文件
                File f =new File(Path);
                if(f.exists()){
                       return true;
                }else{
                    return false;
                }
            }else{
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static List<String> getUsbExtention(Application app){
        List<String> storageList = new ArrayList<>();
        StorageManager storageManager = (StorageManager) app.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", paramClasses);
            getVolumePathsMethod.setAccessible(true);
            Object[] params = {};
            Object invoke = getVolumePathsMethod.invoke(storageManager, params);
            for (int i = 0; i < ((String[])invoke).length; i++) {

                Log.i("TAG", "!!!!!!!!!!!!!!!!!path----> " + ((String[])invoke)[i].toString());
                storageList.add(((String[])invoke)[i].toString());
            }
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
            storageList.clear();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            storageList.clear();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            storageList.clear();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            storageList.clear();
        }
        return storageList;
    }

    /**
     * 删除文件，可以是文件或文件夹
     *
     * @param fileName
     *            要删除的文件名
     * @return 删除成功返回true，否则返回false
     */
    public static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            Log.d(TAG,"删除文件失败:" + fileName + "不存在！");
            return false;
        } else {
            if (file.isFile())
                return deleteFile(fileName);
            else
                return deleteDirectory(fileName);
        }
    }

    /**
     * 删除单个文件
     *
     * @param fileName
     *            要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.d(TAG,"删除单个文件" + fileName + "成功！");
                return true;
            } else {
                Log.d(TAG,"删除单个文件" + fileName + "失败！");
                return false;
            }
        } else {
            Log.d(TAG,"删除单个文件失败：" + fileName + "不存在！");
            return false;
        }
    }

    /**
     * 删除目录及目录下的文件
     *
     * @param dir
     *            要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String dir) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!dir.endsWith(File.separator))
            dir = dir + File.separator;
        File dirFile = new File(dir);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            Log.d(TAG,"删除目录失败：" + dir + "不存在！");
            return false;
        }
        boolean flag = true;
        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
            // 删除子目录
            else if (files[i].isDirectory()) {
                flag = deleteDirectory(files[i]
                        .getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag) {
            Log.d(TAG,"删除目录失败！");
            return false;
        }
        // 删除当前目录
        if (dirFile.delete()) {
            Log.d(TAG,"删除目录" + dir + "成功！");
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查目录内是否有某文件
     * 支持模糊查询(contains)
     *
     * @param dir 目录地址
     * @param fileName 需要查询的文件名
     * @param isLike 是否模糊查询
     * @return
     */
    public static boolean hasFile(String dir, final String fileName, final boolean isLike){
        if(null == dir || null == fileName){
            return false;
        }

        File fileDir = new File(dir);
        if(fileDir != null && fileDir.exists() && fileDir.isDirectory()){
            String[] videoList = fileDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(isLike){
                        return name.contains(fileName);
                    } else {
                        return name.equals(fileName);
                    }
                }
            });
            if (null != videoList && videoList.length > 0) {
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
}
