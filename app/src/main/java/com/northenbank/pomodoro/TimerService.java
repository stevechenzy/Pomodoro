package com.northenbank.pomodoro;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class TimerService extends Service {
    private static final String TAG = "TimerService:";

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        TimerService getService() {
            // Return this instance of TimerService so clients can call public methods
            return TimerService.this;
        }
    }
    public TimerService() {
    }

    @Override
    public ComponentName startService(Intent service) {
        //Log.i(TAG, "startService called.");
        return super.startService(service);
    }

    @Override
    public void startActivity(Intent intent) {
        //Log.i(TAG, "startActivity called.");
        super.startActivity(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        //Log.i(TAG, "onRebind called.");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        //Log.i(TAG, "onDestroy called.");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.i(TAG, "onStartCommand called.");
        putServiceForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        //Log.i(TAG, "onCreate called.");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //Log.i(TAG, "onBind called.");
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }

    //communicate with activity

    private void sendBroadcastMessage(String intentFilterName, long arg1, String extraKey) {
        Intent intent = new Intent("TIMER_STATE_UPDATE");
        if (arg1 != -1 && extraKey != null) {
            intent.putExtra(extraKey, arg1);
        }
        sendBroadcast(intent);
    }

    private void putServiceForeground(){
//        Intent notificationIntent = new Intent(this, TimerService.class);
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        Notification notification =
//                new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
//                        .setContentTitle(getText(R.string.notification_title))
//                        .setContentText(getText(R.string.notification_message))
//                        .setSmallIcon(R.drawable.icon)
//                        .setContentIntent(pendingIntent)
//                        .setTicker(getText(R.string.ticker_text))
//                        .build();
//
//        startForeground(ONGOING_NOTIFICATION_ID, notification);

        Intent notificationIntent = new Intent(this, FullscreenActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "13337")
                .setSmallIcon(R.drawable.tomoto1)
                .setContentTitle("Pomodoro")
                .setContentText("pomodoro is running")
                .setContentIntent(pendingIntent).build();

        startForeground(13337, notification);
    }
    //--------timer class-----------
    private static final int TIMER_STATE_READY = 0;
    private static final int TIMER_STATE_RUNNING = 1;
    private static final int TIMER_STATE_PAUSE = 2;
    private static final int TIMER_STATE_FINISHED = 3;

    private int timerState = TIMER_STATE_READY;
    private long remainMilliseconds = 0;
    private CountDownTimer countdownTimer;
    public int getTimerState(){
        return timerState;
    }

    public void startTimer(long currentSetTimer) {
        //Log.i(TAG, "startTimer called. timerState="+timerState );
        if (timerState == TIMER_STATE_READY) {
            Intent intent = new Intent(this, TimerService.class);
            intent.putExtra("key", "val");
            startService(intent);
            putServiceForeground();
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
            //Log.i(TAG, "resuming a timer;");
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

        }else if (timerState == TIMER_STATE_PAUSE){
            stopForeground(true);
        }
        stopSelf();
        remainMilliseconds = 0;
        timerState = TIMER_STATE_READY;
        //updateTimerSetting();

    }
    private void updateTick(long remainMS){
        ////Log.i(TAG, "updateTick called. remain:"+remainMS+" | timerState="+timerState);
        sendBroadcastMessage("TIMER", remainMS, "TIMER_STATE_UPDATE");
    }
    private void onTimerFinished(){
        //Log.i(TAG, "onTimerFinished");
        sendBroadcastMessage("TIMER", 0, "TIMER_STATE_UPDATE");


        // Add as notification
        Intent notificationIntent = new Intent(this, FullscreenActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "13337")
                .setSmallIcon(R.drawable.tomoto1)
                .setContentTitle("Pomodoro")
                .setContentText("pomodoro is mature!")
                .setContentIntent(pendingIntent).build();

        startForeground(13337, notification);
        //NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //manager.notify(0, builder.build());

    }

    public class CountDownTimerClass extends CountDownTimer {

        public CountDownTimerClass(long millisInFuture, long countDownInterval) {

            super(millisInFuture, countDownInterval);

        }


        @Override
        public void onTick(long millisUntilFinished) {

            remainMilliseconds = millisUntilFinished;

            updateTick(remainMilliseconds);
//            int progress = (int) (millisUntilFinished / 1000);
//            int min = progress / 60;
//            int sec = progress % 60;

            //mContentView.setText(min + ":" + String.format("%02d", sec));

        }

        @Override
        public void onFinish() {
            onTimerFinished();
            //mContentView.setText(R.string.count_down_finished);
            timerState = TIMER_STATE_FINISHED;
            remainMilliseconds = 0;

            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.done);
            mp.start();
            stopSelf();

        }
    }
}
