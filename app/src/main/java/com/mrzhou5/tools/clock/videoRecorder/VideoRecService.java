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
import com.mrzhou5.tools.clock.util.MsgUtil;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

    private volatile AtomicBoolean isRecording = new AtomicBoolean(false);
    private volatile AtomicBoolean isRegister = new AtomicBoolean(false);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("RESTART_SERVICE")) {
                Log.d(TAG, "onReceive: 停止录像服务");
                MsgUtil.send("停止录像服务", "" + System.currentTimeMillis());
                Intent videoIntent = new Intent(MaintenceInfoActivity.getInstance(), VideoRecService.class);
                MaintenceInfoActivity.getInstance().stopService(videoIntent);
            } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                Future<Boolean> future = executor.submit(startWork);
                try {
                    future.get(8, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "stop: " + e.getMessage());
                    restart();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "onCreat");
        MaintenceInfoActivity.atomicIsStartVideo.set(true);
        // 创建悬浮窗
        createFloatView();
        initView();
        // 录像
        new Thread(() -> {
            boolean isException = false;
            try {
                Thread.sleep(3000);
                startRecord();
            } catch (Exception e) {
                isException = true;
                Log.e(TAG, "onCreate: " + e.getMessage());
            } finally {
                if (!isException) {
                    // 启动分钟监听器 整分时收到广播
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_TIME_TICK);
                    intentFilter.addAction("RESTART_SERVICE");
                    registerReceiver(mReceiver, intentFilter);
                    isRegister.set(true);
                } else {
                    restart();
                }
            }
        }).start();
    }

    /**
     * 开始录制视频
     */
    public void startRecord() throws Exception {
        if (!isRecording.get()) {
            Log.d(TAG, "开始录像");
            isRecording.set(true);
            createRecordDir();
            initRecord();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (isRecording.get()) {
            isRecording.set(false);
            Log.d(TAG, "停止录像");
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
    }

    @Override
    public void onDestroy() {
        MaintenceInfoActivity.atomicIsStartVideo.set(false);
        Log.d(TAG, "onDestroy");
        try {
            if (isRegister.get()) {
                unregisterReceiver(mReceiver);
                isRegister.set(false);
            }
            try {
                stopRecord();
            } catch (Exception e) {
                Log.e(TAG, "stopRecord: " + e.getMessage());
            }
            freeCameraResource();
            releaseRecord();
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: " + e.getMessage());
        } finally {
            mWindowManager.removeView(mFloatLayout);
            super.onDestroy();
        }
    }

    private void restart() {
        Intent intent = new Intent("RESTART_SERVICE");
        sendBroadcast(intent);
    }

    private void initView() {
        mSurfaceHolder = mSurfaceView.getHolder();// 取得holder
        mSurfaceHolder.removeCallback(this);
        mSurfaceHolder.addCallback(this); // holder加入回调接口
        mSurfaceHolder.setKeepScreenOn(true);
    }

    /**
     * 录制前，初始化
     */
    private void initRecord() throws Exception {
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

        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        if (null == mProfile) {
            isRecording.set(false);
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
                Log.e(TAG, "releaseRecord:" + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "releaseRecord:" + e.getMessage());
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
        mSurfaceView = mFloatLayout.findViewById(R.id.surfaceView);
        btn_record = mFloatLayout.findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
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

    private void initCamera() {
        try {
            mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
            if (mCamera == null) {
                restart();
                return;
            }
//            mCamera.setDisplayOrientation(180);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            parameters = mCamera.getParameters();// 获得相机参数

            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, 320, 240);
            Log.d(TAG, "initCamera: " + optimalSize.width + "-" + optimalSize.height);
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
            Log.e(TAG, "resetRecorder:" + e.getMessage());
        }
    }

    /**
     * 录像控制方法
     * 整点时停止并重新开启录像（切换文件）
     * 异常状态（摄像头断连、存储设备断连）
     */
    public void recorder() throws Exception {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);

        if (0 == min % 1) {
            // 整点切换文件
            stopRecord();
            if (hour > 20 || hour < 7) {
                Log.d(TAG, "recorder: 非录像时段");
                return;
            }
            if (CheckInApp.getIsMaintence()) {
                Log.d(TAG, "维护中暂不录像");
                return;
            }
            startRecord();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                Log.d(TAG, "onClick: ");
                restart();
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        restart();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "预览创建：surfaceCreated");
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "预览改变：surfaceChanged: format:" + format + " width:" + width + " height" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "预览销毁：surfaceDestroyed");
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
                for (int i = 0; i < fileLength - 3; i++) {
                    FileUtil.delete(files[i].getAbsolutePath());
                }
            }

            // 判断存储空间，不足时删除最旧日期的文件
            files = videoDirs.listFiles();
            if (noneFree() && files != null && files.length > 0) {
                if (files.length == 1) {
                    File[] videoFiles = files[0].listFiles();
                    if (null != videoFiles && videoFiles.length > 2) {
                        Arrays.sort(videoFiles, dateDirComparator);
                        for (int i = 0; noneFree() && (i < videoFiles.length - 2); i++) {
                            FileUtil.delete(videoFiles[i].getAbsolutePath());
                        }
                    }
                } else {
                    Arrays.sort(files, dateDirComparator);
                    FileUtil.delete(files[0].getAbsolutePath());
                }
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
            Log.e(TAG, "createRecordFile:" + e.getMessage());
        }
    }

    /**
     * 空间不足
     *
     * @return
     */
    private boolean noneFree() {
        return 1073741824L > getDirFreeSpace(FileUtil.getVideoStorageRootPath());
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
            Log.e(TAG, "Invalid path:" + e.getMessage());
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    Comparator<File> dateDirComparator = (o1, o2) -> {
        long o1Name = 0;
        try {
            o1Name = Long.parseLong(o1.getName().replace(".mp4", ""));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Arrays.sort:" + e.getMessage());
            return -1;
        }
        long o2Name = 0;
        try {
            o2Name = Long.parseLong(o2.getName().replace(".mp4", ""));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Arrays.sort:" + e.getMessage());
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
}
