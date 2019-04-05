package com.wubeibei.front;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.alibaba.fastjson.JSONObject;
import com.wubeibei.front.command.LeftDoorCommand;
import com.wubeibei.front.util.CrashHandler;
import com.wubeibei.front.util.LogUtil;
import com.wubeibei.front.util.MyTimer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private List<Uri> Playlist;
    private List<Uri> list;
    private final static int alreadyintostation = 0;
    private final static int alreadystart = 1;
    private final static int auto = 2;
    private final static int highspeed = 3;
    private final static int letwork = 4;
    private final static int startandend = 5;
    private final static int t1 = 6;
    private final static int t2 = 7;
    private final static int thanks = 8;
    private final static int upanddowncustmer = 9;
    private final static int welcome = 10;

    private volatile boolean AutoState = false;

    private volatile int msec = 0;
    private volatile int videoindex = 0;
    private VideoView videoView;
    private circulPlay circulPlay = new circulPlay();
    private sequencePlay sequencePlay = new sequencePlay();

    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashHandler.getInstance().init(this);
        init();
        hideBottomUIMenu();
        videoView = findViewById(R.id.videoview);
        videoView.setVideoURI(list.get(welcome));
        videoView.setOnCompletionListener(new circulPlay());
        new Thread(new Runnable() {
            @Override
            public void run() {
                UDP_receive();
            }
        }).start();
        myTimer = new MyTimer(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        videoindex = 0;
                        msec = 0;
                        setAuto();
                    }
                });
            }
        });
        myTimer.start();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (videoView != null)
            videoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null)
            videoView.pause();
    }

    private void init() {
        list = new ArrayList<>(Arrays.asList(
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.alreadyintostation),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.alreadystart),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.auto),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.highspeed),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.letwork),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.startandend),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.t1),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.t2),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.thanks),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.upanddowncustmer),
                Uri.parse("android.resource://" + getPackageName() + "/raw/" + R.raw.welcome)
        ));
        Playlist = new ArrayList<>(Arrays.asList(
                list.get(welcome),
                list.get(startandend),
                list.get(welcome),
                list.get(startandend),
                list.get(alreadystart),
                list.get(auto),
                list.get(startandend),
                list.get(highspeed),
                list.get(auto),
                list.get(alreadyintostation),
                list.get(t2),
                list.get(upanddowncustmer),
                list.get(alreadystart),
                list.get(auto),
                list.get(letwork),
                list.get(alreadystart),
                list.get(startandend),
                list.get(auto),
                list.get(alreadyintostation),
                list.get(t1),
                list.get(thanks),
                list.get(thanks),
                list.get(t1)
        ));

    }

    // 接收CAN总线
    private void UDP_receive() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(null);
            datagramSocket.setReuseAddress(true);
            datagramSocket.bind(new InetSocketAddress(5556));

            DatagramPacket datagramPacket;
            while (true) {
                byte[] receMsgs = new byte[1024];
                datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
                // 读取到命令
                try {
                    datagramSocket.receive(datagramPacket);
                    JSONObject jsonObject = JSONObject.parseObject(new String(receMsgs));
                    LogUtil.d(TAG, jsonObject.toJSONString());
                    int id = jsonObject.getIntValue("id");
                    final int data;
                    switch (id) {
                        case LeftDoorCommand.Driver_model:
                            data = jsonObject.getIntValue("data");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switch (data) {
                                        case LeftDoorCommand.Auto:
                                            setAuto();
                                            myTimer.startTimer();
                                            break;
                                        case LeftDoorCommand.Remote:
                                            setRemote();
                                            myTimer.cancelTimer();
                                            break;
                                    }
                                }
                            });
                            break;
                        case LeftDoorCommand.Left_Work_Sts:
                            showDoorState(jsonObject.getIntValue("data"));
                            break;
                    }
                } catch (IOException e) {
                    // 命令解释错误则重新读取命令
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            UDP_receive();
        }
    }

    private synchronized void setAuto() {
        videoView.setVideoURI(Playlist.get(videoindex % Playlist.size()));
        videoView.setOnCompletionListener(sequencePlay);
        videoView.seekTo(msec);
        videoView.start();
        Log.d(TAG, "setAuto: 开始于 ： " + videoindex + "/" + msec);
        AutoState = true;
    }

    private synchronized void setRemote() {
        videoView.setVideoURI(list.get(welcome));
        videoView.setOnCompletionListener(circulPlay);
        msec = 0;
        videoindex = 0;
        videoView.start();
        AutoState = false;
    }

    // 显示门的状态
    private void showDoorState(final int DoorState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //更新UI
                switch (DoorState) {
                    // opening
                    case 1:
                        doingScene();
                        break;
                    // opened
                    case 3:
                        didScene();
                        break;
                    // closing
                    case 4:
                        doingScene();
                        break;
                    // closed
                    case 0:
                        didScene();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void didScene() {
        if (AutoState) {
            setAuto();
            myTimer.startTimer();
        } else {
            setRemote();
            myTimer.cancelTimer();
        }
    }

    private void doingScene() {
        myTimer.pauseTimer();
        videoView.pause();
        if (AutoState) {
            if (videoView.getCurrentPosition() == videoView.getDuration())
                msec = 0;
            else
                msec = videoView.getCurrentPosition();
        }
        Log.d(TAG, "run: 暂停于 ： " + videoindex + "/" + msec);
        videoView.setVideoURI(list.get(welcome));
        videoView.setOnCompletionListener(circulPlay);
        videoView.start();
    }

    class circulPlay implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion: " + "循环播放");
            mp.start();
        }
    }

    class sequencePlay implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
            videoindex++;
            Log.d(TAG, "onCompletion: " + "切换到 " + videoindex);
            videoView.setVideoURI(Playlist.get(videoindex % Playlist.size()));
            videoView.start();
        }
    }

    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
