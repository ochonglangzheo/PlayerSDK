package com.jeffmony.playerdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.jeffmony.playersdk.CommonPlayer;
import com.jeffmony.playersdk.IPlayer;
import com.jeffmony.playersdk.PlayerParams;
import com.jeffmony.playersdk.PlayerType;
import com.jeffmony.playersdk.utils.ScreenUtils;
import com.jeffmony.videorender.IRenderProcess;
import com.jeffmony.videorender.ImageUtils;
import com.jeffmony.videorender.LogTag;
import com.jeffmony.videorender.MirrorType;
import com.jeffmony.videorender.RenderSdk;
import com.jeffmony.videorender.listener.OnCaptureListener;
import com.jeffmony.videorender.listener.OnSurfaceListener;

public class TextureViewActivity extends AppCompatActivity {

    private final static int MSG_CAPTURE_FRAME_SUCCESS = 100;
    private final static int MSG_CAPTURE_FRAME_FAILED = 101;
    private final static int MSG_INIT_PLAYER = 102;
    private TextureView mVideoTextureView;
    private IRenderProcess mRenderProcess;
    private Surface mSurface;
    private CommonPlayer mPlayer;
    private String mVideoUrl;
    private Button mPlayPauseBtn;
    private int mBackgroundEffectId = -1;
    private int mStickerId = -1;

    private Handler mMainHandler = new Handler(msg -> {
        int what = msg.what;
        if (what == MSG_CAPTURE_FRAME_SUCCESS) {

        } else if (what == MSG_CAPTURE_FRAME_FAILED) {

        } else if (what == MSG_INIT_PLAYER) {
            initPlayer();
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_view);
        mVideoUrl = getIntent().getStringExtra("video_uri");
        mVideoTextureView = findViewById(R.id.video_texture_view);
        mRenderProcess = RenderSdk.createRenderProcess();
        mRenderProcess.setTextureView(mVideoTextureView);
        mRenderProcess.setOnSurfaceListener(new OnSurfaceListener() {
            @Override
            public void onSurfaceCreated(Surface surface) {
                mSurface = surface;
                if (mBackgroundEffectId == -1) {
                    mBackgroundEffectId = mRenderProcess.addEffect("{\n" +
                            "    \"effect\":[\n" +
                            "        {\n" +
                            "            \"type\":\"background\",\n" +
                            "            \"backgroundType\":1,\n" +
                            "            \"blur\":10,\n" +
                            "            \"renderFrameType\":0,\n" +
                            "            \"z_order\":1\n" +
                            "        }\n" +
                            "    ]\n" +
                            "}");
                }
                mMainHandler.sendEmptyMessage(MSG_INIT_PLAYER);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {

            }

            @Override
            public void onSurfaceDestroy() {

            }
        });
        findViewById(R.id.reset_btn).setOnClickListener(v -> {
            if (mRenderProcess == null) {
                return;
            }
            if (mStickerId != -1) {
                mRenderProcess.deleteEffect(mStickerId);
                mStickerId = -1;
            }
            mRenderProcess.setMirror(MirrorType.NONE);
            if (!mPlayer.isPlaying()) {
                mRenderProcess.updateFrame();
            }
        });
        mPlayPauseBtn = findViewById(R.id.play_pause_btn);
        mPlayPauseBtn.setOnClickListener(v-> {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                mPlayPauseBtn.setText("播放");
            } else {
                mPlayer.start();
                mPlayPauseBtn.setText("暂停");
            }
        });

        findViewById(R.id.horizontal_btn).setOnClickListener(v -> {
            if (mRenderProcess == null) {
                return;
            }
            mRenderProcess.setMirror(MirrorType.HORIZONTAL);
            if (!mPlayer.isPlaying()) {
                mRenderProcess.updateFrame();
            }
        });
        findViewById(R.id.vertical_btn).setOnClickListener(v -> {
            if (mRenderProcess == null) {
                return;
            }
            mRenderProcess.setMirror(MirrorType.VERTICAL);
            if (!mPlayer.isPlaying()) {
                mRenderProcess.updateFrame();
            }
        });
        findViewById(R.id.capture_btn).setOnClickListener(v -> {
            if (mRenderProcess == null) {
                return;
            }
            mRenderProcess.captureFrame(new OnCaptureListener() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    Log.i(LogTag.TAG, "captureFrame onSuccess");
                    if (bitmap != null) {
                        ImageUtils.saveImg(bitmap, "/sdcard/DCIM/Camera/result.jpg");
                        bitmap.recycle();
                        mMainHandler.post(() -> {
                            Toast.makeText(TextureViewActivity.this, "截图成功", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(int code) {
                    Log.e(LogTag.TAG, "captureFrame onError");
                    mMainHandler.post(() -> {
                        Toast.makeText(TextureViewActivity.this, "截图失败", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        findViewById(R.id.sticker_btn).setOnClickListener(v -> {
            if (mRenderProcess == null) {
                return;
            }
            if (mStickerId == -1) {
                mStickerId = mRenderProcess.addEffect("{\n" +
                        "    \"effect\":[\n" +
                        "        {\n" +
                        "            \"type\":\"sticker\",\n" +
                        "            \"path\":\"/sdcard/Pictures/icon.png\",\n" +
                        "            \"center_x\":0.1,\n" +
                        "            \"center_y\":0.2,\n" +
                        "            \"scale\":1.0\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}");
            } else {
                mRenderProcess.deleteEffect(mStickerId);
                mStickerId = -1;
            }
            if (!mPlayer.isPlaying()) {
                mRenderProcess.updateFrame();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
            mPlayPauseBtn.setText("播放");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mRenderProcess != null) {
            mRenderProcess.destroy();
        }
    }

    private void initPlayer() {
        PlayerParams params = new PlayerParams();
        params.setUseOkHttp(false);
        mPlayer = new CommonPlayer(this, PlayerType.IJK_PLAYER, params);
        try {
            mPlayer.setDataSource(this, Uri.parse(mVideoUrl));
        } catch (Exception e) {
            Log.w(LogTag.TAG, "setDataSource failed, exception = " + e.getMessage());
            return;
        }
        mPlayer.setSurface(mSurface);
        mPlayer.setOnPreparedListener(mPrepareListener);
        mPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mPlayer.prepareAsync();
    }

    private IPlayer.OnPreparedListener mPrepareListener = mp -> {
        mPlayer.start();
    };

    private IPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IPlayer mp, int width, int height, int rotationDegree, float pixelRatio, float darRatio) {
            if (mRenderProcess != null && width != 0 && height != 0) {
                mRenderProcess.setVideoSize(width, height);

                ViewGroup.LayoutParams layoutParams = mVideoTextureView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = (int) (ScreenUtils.getScreenWidth(TextureViewActivity.this) * height * 1.0f / width);
                mVideoTextureView.setLayoutParams(layoutParams);
            }
        }
    };
}