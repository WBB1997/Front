package com.wubeibei.front.util;

import android.util.Log;

public class MyTimer extends Thread {
    private static final String TAG = "MyTimer";
    private Runnable target;
    private volatile boolean pause = false;
    private volatile boolean start = false;
    private final static long timeDiff = 162 * 1000;

    public MyTimer(Runnable target) {
        this.target = target;
    }

    @Override
    public void run() {
        long startTime = 0;
        long remainTime = timeDiff;
        while (true){
            if(!isStart())
                continue;
            try {
                if(remainTime <= 0){
                    Log.d(TAG, "run: 时间到了" + remainTime);
                    target.run();
                    remainTime = timeDiff;
                }
                startTime = System.currentTimeMillis();
                Log.d(TAG, "run: 休眠" + remainTime);
                Thread.sleep(remainTime);
                remainTime -= System.currentTimeMillis() - startTime;
            } catch (InterruptedException e) {
                // 如果是暂停,而不是取消
                if(isStart()) {
                    // 获取已经度过的时间
                    remainTime -= System.currentTimeMillis() - startTime;
                    Log.d(TAG, "run: 还剩余" + remainTime);
                    // 开始暂停
                    while (isPause());
                    // 如果在暂停中没有取消任务
                    if(isStart()){
                        // 则重新开始计时
                        startTime = System.currentTimeMillis();
                    }
                }else {
                    remainTime = timeDiff;
                    Log.d(TAG, "run: cancle");
                }
            }
        }
    }

    private boolean isPause() {
        return pause;
    }

    private boolean isStart() {
        return start;
    }

    private void setPause(boolean pause) {
        this.pause = pause;
    }

    private void setStart(boolean start) {
        this.start = start;
    }

    public synchronized void cancelTimer() {
        setStart(false);
        this.interrupt();
    }

    public synchronized void pauseTimer() {
        setPause(true);
        this.interrupt();
    }

    public synchronized void startTimer() {
        setPause(false);
        setStart(true);
    }
}
