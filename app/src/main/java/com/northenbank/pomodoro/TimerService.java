package com.northenbank.pomodoro;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Objects;

public class TimerService extends Service {
    private static final String TAG = "TimerService:";
    private static final String SCREENLOCK = "SCREENLOCK";
    private static final CharSequence CHANNEL_NAME = "CHANNEL_NAME";

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    public TimerService() {
    }

    @Override
    public ComponentName startService(Intent service) {
        return super.startService(service);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        putServiceForeground(getString(R.string.POMODORO_IS_RUNNING));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //communicate with activity

    private void sendBroadcastMessage(long arg1) {
        Intent intent = new Intent(MSG_TYPE_TIMER_STATE_UPDATE);
        intent.putExtra(REMAIN_MS, arg1);
        sendBroadcast(intent);
    }

    private final static String CHANNEL_ID = "13337";
    private final static int NOTIFICATION_ID = 13337;

    private NotificationCompat.Builder createNotificationBuilder(String message) {

        Intent notificationIntent = new Intent(this, FullscreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.tomoto1)
                .setContentTitle(getString(R.string.APP_TITLE))
                .setContentText(message)
                .setContentIntent(pendingIntent);

        return builder;
    }

    private void putServiceForeground(String message) {

        Notification notification;
        NotificationCompat.Builder builder = createNotificationBuilder(message);
        if (Build.VERSION.SDK_INT < 26) {
            notification = builder.build();
        } else {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) Objects.requireNonNull(this.getSystemService(Context.NOTIFICATION_SERVICE))).
                    createNotificationChannel(channel);
            notification = builder.build();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            assert mNotificationManager != null;
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
        startForeground(NOTIFICATION_ID, notification);
    }

    //--------timer class-----------
    static final int TIMER_STATE_READY = 0;
    static final int TIMER_STATE_RUNNING = 1;
    static final int TIMER_STATE_PAUSE = 2;
    static final int TIMER_STATE_FINISHED = 3;
    static final String MSG_TYPE_TIMER_STATE_UPDATE = "TIMER_STATE_UPDATE";
    static final String REMAIN_MS = "REMAIN_MS";

    private int timerState = TIMER_STATE_READY;
    private long remainMilliseconds = 0;
    private CountDownTimer countdownTimer;

    public int getTimerState() {
        return timerState;
    }

    public long getRemainMilliseconds() {
        return remainMilliseconds;
    }

    public void startTimer(long currentSetTimer) {
        if (timerState == TIMER_STATE_READY) {
            Intent intent = new Intent(this, TimerService.class);
            intent.putExtra("key", "val");

            startService(intent);

            putServiceForeground(getString(R.string.POMODORO_IS_RUNNING));
            countdownTimer = new CountDownTimerClass(1000 * currentSetTimer, 1000);
            countdownTimer.start();
            timerState = TIMER_STATE_RUNNING;

        } else if (timerState == TIMER_STATE_RUNNING) {
            try {
                countdownTimer.cancel();
                timerState = TIMER_STATE_PAUSE;

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

        } else {
            countdownTimer = new CountDownTimerClass(remainMilliseconds, 1000);
            countdownTimer.start();
            timerState = TIMER_STATE_RUNNING;
        }

    }

    public void stopTimer() {
        if (timerState == TIMER_STATE_RUNNING) {
            try {
                countdownTimer.cancel();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            stopForeground(true);

        } else if (timerState == TIMER_STATE_PAUSE) {
            stopForeground(true);
        }
        stopSelf();
        remainMilliseconds = 0;
        timerState = TIMER_STATE_READY;
    }

    //-------send notification to lock screen --------
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    private void wakeupScreen() {

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        assert powerManager != null;

        if (!powerManager.isInteractive()) {
            PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, SCREENLOCK);
            wl.acquire(10000);

        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateTick(long remainMS) {
        Log.i(TAG, "updateTick called. remain:" + remainMS + " | timerState=" + timerState);
        sendBroadcastMessage(remainMS);
        if (remainMS <= 2000) {
            wakeupScreen();
        }
    }

    private void onTimerFinished() {
        sendBroadcastMessage(0);
        putServiceForeground(getString(R.string.POMODORO_IS_MATURE));
    }

    public class CountDownTimerClass extends CountDownTimer {

        CountDownTimerClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @SuppressLint("NewApi")
        @Override
        public void onTick(long millisUntilFinished) {
            remainMilliseconds = millisUntilFinished;
            updateTick(remainMilliseconds);
        }

        @Override
        public void onFinish() {
            onTimerFinished();

            timerState = TIMER_STATE_FINISHED;
            remainMilliseconds = 0;

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = new long[]{50, 500, 100, 500, 50, 500, 100, 500};
            vibrator.vibrate(pattern, -1);

            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.done);
            mp.start();
            stopSelf();
        }
    }
}
