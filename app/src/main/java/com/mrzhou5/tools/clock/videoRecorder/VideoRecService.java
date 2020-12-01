package com.mrzhou5.tools.clock.videoRecorder;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.mrzhou5.tools.clock.R;
import com.mrzhou5.tools.clock.activity.MaintenceInfoActivity;
import com.mrzhou5.tools.clock.application.CheckInApp;
import com.mrzhou5.tools.clock.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.hutool.json.JSONUtil;

/**
 * 静默录像
 */
public class VideoRecService extends Service implements View.OnClickListener
        , SurfaceHolder.Callback, MediaRecorder.OnErrorListener {
    private static final String TAG = VideoRecService.class.getSimpleName();
    //定义浮动窗口布局
    RelativeLayout mFloatLayout;
    LayoutParams wmParams;

    //创建浮动窗口设置布局参数的对象
    WindowManager mWindowManager;
    private static VideoRecService instance = null;
    private Button btn_record;              // 录像摄像头

    private SurfaceView mSurfaceView;
    private MediaRecorder mMediaRecorder;   // 录制视频的类
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private File mVecordFile = null;        // 录像文件

    private volatile boolean isRecording = false;
    private volatile boolean isRegister = false;
    List<int[]> mFpsRange;
    private Camera.Size optimalSize;
    private Camera.Parameters parameters;
    //视频存储的目录
    private String dirname;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static VideoRecService getInstance() {
        return instance;
    }

    Callable<Boolean> startWork = () -> {
        recorder();
        return true;
    };

    Comparator<File> dateDirComparator = (o1, o2) -> {
        int o1Name = 0;
        try {
            o1Name = Integer.valueOf(o1.getName());
        } catch (NumberFormatException e) {
            Log.d(TAG, "Arrays.sort:" + e.getMessage());
            return -1;
        }
        int o2Name = 0;
        try {
            o2Name = Integer.valueOf(o2.getName());
        } catch (NumberFormatException e) {
            Log.d(TAG, "Arrays.sort:" + e.getMessage());
            return 1;
        }
        if (o1Name < o2Name) {
            return -1;
        } else if (o1Name == o2Name) {
            return 0;
        } else {
            return 1;
        }
    };
    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Future<Boolean> future = executor.submit(startWork);
            try {
                future.get(8, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Log.d(TAG, "onReceive: 执行超时");
                future.cancel(true);
//                executor.shutdownNow();
                freeCameraResource();
                restartVideoRecoder(true);
            } catch (Exception e) {
                Log.d(TAG, "stop: " + e.getMessage());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "onCreat");
        MaintenceInfoActivity.atomicIsStartVideo = true;
        // 创建悬浮窗
        createFloatView();
        initView();
        // 录像
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                startRecord();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 启动分钟监听器 整分时收到广播
                IntentFilter timeFilter = new IntentFilter();
                timeFilter.addAction(Intent.ACTION_TIME_TICK);
                registerReceiver(mTimeReceiver, timeFilter);
                isRegister = true;
            }
        }).start();
    }

    /**
     * 录像控制方法
     * 整点时停止并重新开启录像（切换文件）
     * 异常状态（摄像头断连、存储设备断连）
     */
    public void recorder() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        try {
            if (0 == min % 1) {
                // 整点切换文件
                stopRecord();
                if (hour > 20 || hour < 7) {
                    Log.d(TAG, "recorder: 非录像时段");
                    return;
                }
                startRecord();
            } else if (!isRecording) {
                // 未启动需要启动
                Log.d(TAG, "recorder: 检测代码");
                startRecord();
            }
        } catch (Exception e) {
            Log.d(TAG, "BroadcastReceiver:" + e.getMessage());
        }
    }

    /**
     * 开始录制视频
     */
    public void startRecord() {
        if (!isRecording) {
            try {
                Log.d(TAG, "开始录像");
                isRecording = true;
                createRecordDir();
                initRecord();
            } catch (Exception e) {
                Log.d(TAG, "startRecord:" + e.getMessage());
            }
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (isRecording) {
            isRecording = false;
            Log.d(TAG, "停止录像");
            if (mMediaRecorder != null) {
                try {
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                } catch (Exception e) {
                    Log.d(TAG, "stopRecord:" + e.getMessage());
                }
            }
        }
    }

    private void restartVideoRecoder(boolean isRestartService) {
        if (isRestartService) {
            Log.d(TAG, "restartVideoRecoderService");
            CheckInApp.getInstance().restartVideoRecorder();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        MaintenceInfoActivity.atomicIsStartVideo = false;
        stop();
        mWindowManager.removeView(mFloatLayout);
        mMediaRecorder = null;
        mWindowManager = null;
        mSurfaceView = null;
        mSurfaceHolder = null;
        mCamera = null;
        isRecording = false;
        if (isRegister) {
            unregisterReceiver(mTimeReceiver);
            isRegister = false;
        }
        super.onDestroy();
    }

    Callable<Boolean> workStop = () -> {
        freeCameraResource();
        stopRecord();
        releaseRecord();
        return true;
    };

    /**
     * 停止拍摄
     * 如果后续还要录像，就不要调用此方法
     */
    public void stop() {
        Log.d(TAG, "start stop");
        try {
            Future<Boolean> future = executor.submit(workStop);
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Log.d(TAG, "stop: 执行超时");
        } catch (Exception e) {
            Log.d(TAG, "stop: " + e.getMessage());
        }
        Log.d(TAG, "end stop");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                Log.d(TAG, "onClick: ");
                try {
                    //获得外接USB输入设备的信息
                    Process p = Runtime.getRuntime().exec("cat /proc/bus/input/devices");
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        String deviceInfo = line.trim();
                        Log.d(TAG, "onClick: " + deviceInfo);
                        //对获取的每行的设备信息进行过滤，获得自己想要的。
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: format:" + format + " width:" + width + " height" + height);
        resetRecorder();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        freeCameraResource();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            Log.d(TAG, "onError");
            if (isRecording) {
                stopRecord();
            }
            if (mr != null) {
                mr.reset();
                resetRecorder();
                releaseRecord();
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, "onError:" + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "onError:" + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    private void resetRecorder() {
        Log.d(TAG, "resetRecorder");
        if (mCamera != null) {
            freeCameraResource();
        }

        try {
            mCamera = Camera.open(1);
            if (mCamera == null)
                return;
//            mCamera.setDisplayOrientation(180);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            parameters = mCamera.getParameters();// 获得相机参数

            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, 1280, 720);

            parameters.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览图像大小
            parameters.set("orientation", "portrait");
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mFpsRange = parameters.getSupportedPreviewFpsRange();

            mCamera.setParameters(parameters);// 设置相机参数
            mCamera.startPreview();// 开始预览
        } catch (Exception e) {
            Log.d(TAG, "resetRecorder:" + e.getMessage());
        }
    }

    private void initView() {
        mSurfaceView = (SurfaceView) mFloatLayout.findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();// 取得holder
        mSurfaceHolder.removeCallback(this);
        mSurfaceHolder.addCallback(this); // holder加入回调接口
        mSurfaceHolder.setKeepScreenOn(true);
        btn_record = (Button) mFloatLayout.findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
    }

    /**
     * 录制前，初始化
     */
    private void initRecord() {
        try {
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
            }
            if (mCamera != null) {
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
            }

            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);//音频源
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);// 视频源

            CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            if (null == mProfile) {
                isRecording = false;
                return;
            }

            if (null != optimalSize) {
                mProfile.videoFrameWidth = optimalSize.width;
                mProfile.videoFrameHeight = optimalSize.height;
            } else {
                mProfile.videoFrameWidth = 640;
                mProfile.videoFrameHeight = 360;
            }

            // 单独从setProfile中抽出的设置视频的参数
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            mMediaRecorder.setOutputFormat(mProfile.fileFormat);
            mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
            mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
//            mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
            // 设置音视频参数统一配置，此方法已经设置了上面的视频参数
//            mMediaRecorder.setProfile(mProfile);
            // （延时录像的采样率）该设置是为了抽取视频的某些帧，参数值在40左右接近正常速度，0-40值越小视频越快
//            mMediaRecorder.setCaptureRate(mFpsRange.get(0)[0]);//获取最小的每一秒录制的帧数

            mMediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());
            // 录像画面旋转
//            mMediaRecorder.setOrientationHint(180);

            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            Log.d(TAG, "initRecord:" + e.getMessage());
            releaseRecord();
        }
    }

    /**
     * 创建目录与文件,并对存储空间进行清理
     */
    private void createRecordDir() {
        // 获取当天的文件夹路径
        dirname = getDateNumber(0);
        // 对存储空间进行清理
        File videoDirs = new File(FileUtil.getVideoStorageRootPath());
        if (!videoDirs.exists()) {
            videoDirs.mkdirs();
        }
        if (null != videoDirs && videoDirs.isDirectory()) {
            // 只保留Constant.RETENTION_TIME天的录像
            File[] files = videoDirs.listFiles();
            int fileLength = files.length;
            if (null != files && fileLength > 1) {
                Arrays.sort(files, dateDirComparator);
            }
            for (int i = 0; i < fileLength - 3; i++) {
                FileUtil.delete(files[i].getAbsolutePath());
            }
            // 判断存储空间，不足时删除最旧日期的文件
            files = videoDirs.listFiles();
            if (null != files && fileLength > 1) {
                Arrays.sort(files, dateDirComparator);
            }
            if (null != files && 1073741824L > getDirFreeSpace(FileUtil.getVideoStorageRootPath())) {
                FileUtil.delete(files[0].getAbsolutePath());
            }
        }
        // 新视频的目录
        File fileDir = new File(FileUtil.getVideoStorageRootPath() + File.separator + dirname);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        // 创建文件
        try {
            mVecordFile = new File(fileDir.getAbsolutePath() + "/" + getDateNumber(2) + ".mp4");
            Log.d(TAG, "FilePath:" + mVecordFile.getAbsolutePath());
        } catch (Exception e) {
            Log.d(TAG, "createRecordFile:" + e.getMessage());
        }
    }

    /**
     * 取当前路径剩余可用空间大小 单位Bytes
     *
     * @param path 文件路径
     * @return
     */
    private long getDirFreeSpace(String path) {
        StatFs stat = null;
        try {
            stat = new StatFs(path);
        } catch (Exception e) {
            Log.d(TAG, "Invalid path:" + e.getMessage());
            return 0;
        }
        long blockSize = 0;
        long availableBlocks = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
        }

        return availableBlocks * blockSize;
    }

    /**
     * 释放资源
     */
    private void releaseRecord() {
        Log.d(TAG, "releaseRecord");
        if (mMediaRecorder != null) {
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.setOnErrorListener(null);
            try {
                mMediaRecorder.release();
            } catch (IllegalStateException e) {
                Log.d(TAG, "releaseRecord:" + e.getMessage());
            } catch (Exception e) {
                Log.d(TAG, "releaseRecord:" + e.getMessage());
            }
        }
        mMediaRecorder = null;
    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        Log.d(TAG, "freeCameraResource");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * 数字时间串
     *
     * @param type 类型 0 年月日型；1 时分秒型；2 年月日时分秒型
     * @return
     */
    private String getDateNumber(int type) {
        Date currentTime = new Date();
        SimpleDateFormat formatter;
        String pattern = "yyyyMMddHHmmss";
        switch (type) {
            case 0:
                pattern = "yyyyMMdd";
                break;
            case 1:
                pattern = "HHmmss";
                break;
            case 2:
                pattern = "yyyyMMddHHmmss";
                break;
        }
        formatter = new SimpleDateFormat(pattern);
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 创建悬浮窗
     */
    @SuppressLint("ClickableViewAccessibility")
    private void createFloatView() {
        Log.d(TAG, "createFloatView");
        wmParams = new LayoutParams();
        //获取WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        //设置window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
//        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags =
//          LayoutParams.FLAG_NOT_TOUCH_MODAL |
                LayoutParams.FLAG_NOT_FOCUSABLE;
//          LayoutParams.FLAG_NOT_TOUCHABLE;

        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;

        // 以屏幕左上角为原点，设置x、y初始值
        wmParams.x = 0;
        wmParams.y = 0;

        // 设置悬浮窗口长宽数据
        wmParams.width = 1;
        wmParams.height = 1;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.video_recorder_window, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //设置监听浮动窗口的触摸移动
        mFloatLayout.setOnTouchListener(new OnTouchListener() {
            private int x;
            private int y;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = (int) motionEvent.getRawX();
                        y = (int) motionEvent.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) motionEvent.getRawX();
                        int nowY = (int) motionEvent.getRawY();
                        int movedX = nowX - x;
                        int movedY = nowY - y;
                        x = nowX;
                        y = nowY;
                        wmParams.x = wmParams.x + movedX;
                        wmParams.y = wmParams.y + movedY;

                        // 更新悬浮窗控件布局
                        mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }
}
