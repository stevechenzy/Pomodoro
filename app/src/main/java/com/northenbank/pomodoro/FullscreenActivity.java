package com.northenbank.pomodoro;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


public class FullscreenActivity extends AppCompatActivity {


    // ----- timer setting ----
    private static final float SCROLL_STEP = 10f;
    private static final String STRING_TIMER_SAMPLE = "25:00";
    private static final int DEFAULT_TIMER = 25 * 60;

    private static final int MAX_TIMER = 99 * 60;
    private static final int MIN_TIMER = 1;

    private static final int GESTURE_MODE_NONE = 0;
    private static final int GESTURE_MODE_ZOOM = 1;


    private static final int FONT_MARGIN = 10;
    public static final int SCREEN_EDGE_BORDER = 200;

    private static final int BOTH = 0;
    private static final int VERTICAL = 1;
    private static final int HORIZONTAL = 2;
    private static final int PINCHING = -1;

    private static final int[] PREDEFINED_TIMERS = {25, 30, 45, 1, 3, 5, 7, 10, 15, 20};
    private static final String TIME_FORMAT = "%d:%02d";
    private int timerIndex = 0;


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onRestart() {
        activateFullScreen();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceivers();
    }


    private long exitTime = 0;

    private boolean toExit() {

        if ((System.currentTimeMillis() - exitTime) > 1000) {
            exitTime = System.currentTimeMillis();
            return false;
        }
        mService.stopTimer();
        unbindService(mConnection);
        unregisterReceiver(broadcastReceiver);

        mBound = false;
        //Log.i("toExit", "toExit");
        finish();
        System.exit(0);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            return toExit();
        }
        return super.onKeyDown(keyCode, event);
    }

    private int currentSetTimer = DEFAULT_TIMER;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        adjustFontSize();
        super.onConfigurationChanged(newConfig);
    }

    private void adjustFontSize() {
        //Log.i("Fontsize:","adjust font size.");
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels - FONT_MARGIN;

        int size = 1;
        mContentView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 1);

        do {
            float textWidth = mContentView.getPaint().measureText(STRING_TIMER_SAMPLE);

            if (textWidth < width)
                mContentView.setTextSize(++size);
            else {
                mContentView.setTextSize(--size);
                break;
            }
        } while (true);

    }

    // We can be in one of these 2 states


    float oldDist = 1f;
    float lastScrollDist = 1f;
    int gestureMode = GESTURE_MODE_NONE;


    private TextView mContentView;


    //private View mControlsView;

    private void updateTimerSetting() {

        int min = currentSetTimer / 60;
        int sec = currentSetTimer % 60;

        mContentView.setText(String.format(TIME_FORMAT, min, sec));

    }

    private void changeTimterSetting(int delta) {
        int newTimer = currentSetTimer;
        if (newTimer / 60 > 1) {
            newTimer /= 60;
            newTimer += delta;
            newTimer *= 60;
        } else {
            newTimer += delta;
            if (newTimer > 60) {
                newTimer = 2 * 60;
            }
        }
        //Log.i("ChangeTimer:", "delta:"+delta+"|  newTimer=" + newTimer);
        if (newTimer <= MAX_TIMER && newTimer >= MIN_TIMER) {
            currentSetTimer = newTimer;
            updateTimerSetting();
        }
    }

    private void activateFullScreen() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        adjustFontSize();

    }

    private boolean timerFinishing = false;

    private void resetTimer() {

        stopTimer();
        mContentView.setBackgroundColor(0xFF000000);//black;
        updateTimerSetting();

        timerFinishing = false;

    }

    private static final String NORTHEN_BANK_PREFS = "NORTHEN_BANK_POMODORO";
    private static final String PREFS_POMODORO_TIMER = "PREFS_POMODORO_TIMER";

    private int getDefaultTimer() {
        int t = DEFAULT_TIMER;
        SharedPreferences prefs = getSharedPreferences(NORTHEN_BANK_PREFS, MODE_PRIVATE);
        if (prefs.contains(PREFS_POMODORO_TIMER)) {
            t = prefs.getInt(PREFS_POMODORO_TIMER, t);

        }
        return t;
    }

    private void saveDefaultTimer2Prefs() {
        SharedPreferences.Editor editor = getSharedPreferences(NORTHEN_BANK_PREFS, MODE_PRIVATE).edit();
        editor.putInt(PREFS_POMODORO_TIMER, currentSetTimer);
        editor.apply();
    }

    TimerService mService;
    boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to TimerService, cast the IBinder and get TimerService instance
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            mService = binder.getService();

            mBound = true;
            if (mService.getTimerState() == TimerService.TIMER_STATE_PAUSE) {
                mContentView.setBackgroundColor(0xFFA4C639);
                onTickUpdate(mService.getRemainMilliseconds());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {

        super.onStart();
        // Create an Explicit Intent
        Intent intent = new Intent(this, TimerService.class);
        intent.putExtra("Pomodoro", "Start");

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        registerBroadcastReceivers();


    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private BroadcastReceiver broadcastReceiver;

    private void registerBroadcastReceivers() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intent.getAction();
                long remainMS = intent.getLongExtra(TimerService.REMAIN_MS, 0);

                onTickUpdate(remainMS);

            }
        };
        IntentFilter progressFilter = new IntentFilter(TimerService.MSG_TYPE_TIMER_STATE_UPDATE);
        registerReceiver(broadcastReceiver, progressFilter);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mContentView = findViewById(R.id.fullscreen_content);
        currentSetTimer = getDefaultTimer();
        updateTimerSetting();
        activateFullScreen();

        //------ double tap ------

        final GestureDetector gd = new GestureDetector(mContentView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            private int getScrollType(MotionEvent e1, MotionEvent e2) {
                if (e1.getPointerCount() > 1) {
                    return PINCHING;
                }
                float deltaV = Math.abs(e2.getY() - e1.getY());
                float deltaH = Math.abs(e2.getX() - e1.getX());
                if (deltaV == 0) {
                    if (deltaH > 0) {
                        return HORIZONTAL;
                    } else {
                        return BOTH;
                    }
                } else {
                    if (deltaH == 0) {
                        return VERTICAL;
                    } else {
                        float scale = deltaH / deltaV;

                        if (scale > 3f) {
                            return HORIZONTAL;
                        } else {
                            if (scale < 0.33f) {
                                return VERTICAL;
                            }
                        }
                    }
                }
                return BOTH;
            }


            @Override
            public boolean onDoubleTap(MotionEvent e) {
               startTimer();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getPointerCount() > 1) {
                    return false;
                }
                if (gestureMode == GESTURE_MODE_ZOOM) {
                    return false;
                }
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int width = dm.widthPixels / 2;
                float scale = e1.getX() - e2.getX();
                if (mBound) {
                    if (mService.getTimerState() == TimerService.TIMER_STATE_RUNNING || mService.getTimerState() == TimerService.TIMER_STATE_PAUSE) {
                        if (Math.abs(scale) > width) {
                            stopTimer();
                        }
                    } else {
                        if (Math.abs(scale) > width) {
                            stopTimer();
                            preselectedTimers(scale);
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                String TAG = "OnScroll:";
                if (!mBound) {
                    return super.onScroll(e1, e2, distanceX, distanceY);
                }
                if (mService.getTimerState() != TimerService.TIMER_STATE_READY) {
                    return true;
                }

                if (getScrollType(e1, e2) == VERTICAL) {

                    DisplayMetrics dm = getResources().getDisplayMetrics();

                    if (e1.getY() < SCREEN_EDGE_BORDER || e1.getY() > (dm.heightPixels - SCREEN_EDGE_BORDER)) {
                        Log.i(TAG, "scroll start too close to edge, skip: " + e2.getY() + "," + e1.getY() + " | " + dm.heightPixels);
                        return true;
                    }

                    float scrollDist = e1.getY() - e2.getY();
                    float scale = scrollDist - lastScrollDist;

                    if (Math.abs(scale) > SCROLL_STEP) {
                        int delta = 0;
                        if (scale > 0) {
                            delta = 1;
                        } else if (scale < 0) {
                            delta = -1;
                        }
                        if (delta != 0) {
                            changeTimterSetting(delta);
                        }
                        lastScrollDist = scrollDist;
                    }
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.i("OnDoubleTapListener", "onLongPress");
                if (mBound) {
                    if (mService.getTimerState() == TimerService.TIMER_STATE_RUNNING) {
                        startTimer();
                    }
                }
            }


            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }


        });

        mContentView.setOnTouchListener(new View.OnTouchListener() {

            private float spacing(MotionEvent event) {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mBound) {
                    return true;
                }

                if (mService.getTimerState() != TimerService.TIMER_STATE_READY) {
                    return gd.onTouchEvent(event);
                }

                //pinch
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > SCROLL_STEP) {
                            gestureMode = GESTURE_MODE_ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        gestureMode = GESTURE_MODE_NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (gestureMode == GESTURE_MODE_ZOOM) {
                            float newDist = spacing(event);
                            float scale = newDist - oldDist;
                            if (Math.abs(scale) > SCROLL_STEP) {
                                int delta = 1;
                                if (scale < 0) {
                                    delta = -1;
                                }
                                changeTimterSetting((delta));
                                oldDist = newDist;
                            }
                        }
                        break;
                }
                return gd.onTouchEvent(event);
            }
        });
    }

    private void preselectedTimers(float scale) {
        if (scale > 0) {
            timerIndex += 1;
        } else {
            timerIndex += PREDEFINED_TIMERS.length - 1;
        }
        timerIndex %= PREDEFINED_TIMERS.length;
        currentSetTimer = PREDEFINED_TIMERS[timerIndex] * 60;
        updateTimerSetting();
    }


    private void stopTimer() {
        if (mBound) {
            mService.stopTimer();
        }
        mContentView.setBackgroundColor(0xFF000000);
        updateTimerSetting();
    }

    private void startTimer() {
        if (timerFinishing) {
            resetTimer();
            return;
        }
        if (mBound) {
            if (mService.getTimerState() == TimerService.TIMER_STATE_READY) {
                saveDefaultTimer2Prefs();
            } else if (mService.getTimerState() == TimerService.TIMER_STATE_RUNNING) {
                mContentView.setBackgroundColor(0xFFA4C639);
            } else {
                mContentView.setBackgroundColor(0xFF000000);
            }
            mService.startTimer(currentSetTimer);
        }
    }


    @SuppressLint("SetTextI18n")
    public void onTickUpdate(long millisUntilFinished) {

        int progress = (int) (millisUntilFinished / 1000);
        int min = progress / 60;
        int sec = progress % 60;

        mContentView.setText(String.format(TIME_FORMAT, min, sec));
        if (millisUntilFinished == 0) {
            onTimerFinish();
        }

    }


    public void onTimerFinish() {

        timerFinishing = true;
        mContentView.setBackgroundColor(0xFFE64431);//red 230,68, 49

    }
}
