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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;


public class FullscreenActivity extends AppCompatActivity {

    /*
    // -- auto hide ---
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;
    private static final int UI_ANIMATION_DELAY = 2000;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {

        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(2000);
    }

    private void toggle() {
        //Log.i("toggle", "mVisible=" + mVisible);
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);

        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    */
//    /**
//     * Touch listener to use for in-layout UI controls to delay hiding the
//     * system UI. This is to prevent the jarring behavior of controls going away
//     * while interacting with activity UI.
//     */
//    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            if (AUTO_HIDE) {
//                delayedHide(AUTO_HIDE_DELAY_MILLIS);
//            }
//            return false;
//        }
//    };
    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    // ----- timer setting ----
    private static final float SCROLL_STEP = 10f;
    private static final String STRING_TIMER_SAMPLE = "25:00";
    private static final int DEFAULT_TIMER = 25 * 60;

    private static final int MAX_TIMER = 99 * 60;
    private static final int MIN_TIMER = 1;

    private static final int GESTURE_MODE_NONE = 0;
    private static final int GESTURE_MODE_ZOOM = 1;
    private static final int GESTURE_MODE_PINCH = 2;

    private static final int MIN_FONT_SIZE = 10;
    private static final int MAX_FONT_SIZE = 50;
    private static final int FONT_MARGIN = 10;

//    private static final int TIMER_STATE_READY = 0;
//    private static final int TIMER_STATE_RUNNING = 1;
//    private static final int TIMER_STATE_PAUSE = 2;
//    private static final int TIMER_STATE_FINISHED = 3;
    private static int STOP_TIMER_FLING_THRESHOLD = 200;

    private static final int BOTH = 0;
    private static final int VERTICAL = 1;
    private static final int HORIZONTAL = 2;
    private static final int PINCHING = -1;

    private static final int[] PREDEFINED_TIMERS = {25, 30, 45, 1, 3, 5, 7, 10, 15, 20};
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

    CountDownTimer countdowntimer;
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
        int height = dm.heightPixels;
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

    private ScaleGestureDetector mScaleDetector;
    // We can be in one of these 2 states


    float oldDist = 1f;
    float lastScrollDist = 1f;
    int gestureMode = GESTURE_MODE_NONE;


    private TextView mContentView;


    //private View mControlsView;

    private void updateTimerSetting() {

        int min = currentSetTimer / 60;
        int sec = currentSetTimer % 60;

        mContentView.setText(min + ":" + String.format("%02d", sec));

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

        //getSupportActionBar().setDisplayShowTitleEnabled(false);
        //getSupportActionBar().setDisplayShowHomeEnabled(false);
        adjustFontSize();

    }

    private boolean timerFinishing = false;

    private void resetTimer() {
        //if (timerFinishing) {
        stopTimer();
        mContentView.setBackgroundColor(0xFF000000);//black;
        updateTimerSetting();
        //timerState = TIMER_STATE_READY;
        timerFinishing = false;
        //}
    }

    private static final String NORTHEN_BANK_PREFS = "NORTHEN_BANK_POMODORO";
    private static final String PREFS_POMODORO_TIMER = "PREFS_POMODORO_TIMER";

    private int getDefaultTimer() {
        int t = DEFAULT_TIMER;
        SharedPreferences prefs = getSharedPreferences(NORTHEN_BANK_PREFS, MODE_PRIVATE);
        if (prefs.contains(PREFS_POMODORO_TIMER)) {
            t = prefs.getInt(PREFS_POMODORO_TIMER, t);
            //Log.i("getPref", "Timer found, t=" + t);
        } else {
            //Log.i("getPref", "Timer not found, use default 25min");
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
            //Log.i("ServiceBund:", "timerState:" + mService.getTimerState());
            mBound = true;
            if (mService.getTimerState() == TimerService.TIMER_STATE_PAUSE){
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
        Log.i("Activity","onStart");
        super.onStart();
        // Create an Explicit Intent
        Intent intent = new Intent(this, TimerService.class);
// Set some data that the Service might require/use
        intent.putExtra("key", "val");
// Start the Service
        // startService(intent);
        // Bind to TimerService
        //Intent intent = new Intent(this, TimerService.class);
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
                long remainMS = intent.getLongExtra("TIMER_STATE_UPDATE", 0);
                // Log.i("On Receive", "timer update message:" + intent.getAction()+" | remain:"+remainMS);
                onTickUpdate(remainMS);

            }
        };
        IntentFilter progressfilter = new IntentFilter("TIMER_STATE_UPDATE");
        registerReceiver(broadcastReceiver, progressfilter);
    }


    //@SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        //mVisible = true;
        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        currentSetTimer = getDefaultTimer();
        updateTimerSetting();
        activateFullScreen();



        //------ double tap ------


        final GestureDetector gd = new GestureDetector(mContentView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            private int getScrollType(MotionEvent e1, MotionEvent e2, float dX, float dY) {
                String TAG = "ScrollType:";
                //Log.i(TAG, "e1:" + e1.getX() + "," + e1.getY() + " | e2:" + e2.getX() + "," + e2.getY() + " | dx:" + dX + "," + "dy:" + dY);
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
                        //Log.i(TAG, "scale:" + scale + " = (" + deltaH + "/" + deltaV+")");
                        if (scale > 3f) {
                           // Log.i(TAG, "Horisontal scale:" + scale);
                            return HORIZONTAL;
                        } else {
                            if (scale < 0.33f) {
                               // Log.i(TAG, "vertical");
                                return VERTICAL;
                            } else {
                               // Log.i(TAG, "both: scale=" + scale);
                            }
                        }
                    }
                }
                return BOTH;
            }
            //here is the method for double tap


            @Override
            public boolean onDoubleTap(MotionEvent e) {

                //your action here for double tap e.g.
                Log.i("OnDoubleTapListener", "onDoubleTap");
                startTimer();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // My fling event
                //Log.i("onFling:", "x:" + velocityX + "  pointers:" + e1.getPointerCount());

                if (e1.getPointerCount() > 1) {
                    return false;
                }
                if (gestureMode == GESTURE_MODE_ZOOM) {
                    return false;
                }
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int width = dm.widthPixels - STOP_TIMER_FLING_THRESHOLD;
                //width = dm.widthPixels *3/4;
                float scale = e1.getX() - e2.getX();
                if (mBound) {
                    if (mService.getTimerState() == TimerService.TIMER_STATE_RUNNING || mService.getTimerState() == TimerService.TIMER_STATE_PAUSE) {
                        if (Math.abs(scale) > width) {
                            stopTimer();
                        }
                    } else {
                        if (Math.abs(scale) > 500) {
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
                //Log.i(TAG, "timeState:"+timerState);
                if (!mBound) {
                    return super.onScroll(e1, e2, distanceX, distanceY);
                }
                if (mService.getTimerState() != TimerService.TIMER_STATE_READY) {
                    return true;
                }

                if (getScrollType(e1, e2, distanceX, distanceY) == VERTICAL) {

                    float scrollDist = e1.getY() - e2.getY();
                    //Log.i(TAG, "new distY:" + scrollDist + " | y:" + distanceY);

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
                } else {
                    //Log.i(TAG, "Not Vertical scroll");
                }

                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.i("OnDoubleTapListener", "onLongPress");
                if (mBound){
                    if (mService.getTimerState() == TimerService.TIMER_STATE_RUNNING){
                        startTimer();
                    }
                }
                //super.onLongPress(e);

            }



//            @Override
//            public boolean onDoubleTapEvent(MotionEvent e) {
//                //Log.d("OnDoubleTapListener", "onDoubleTapEvent");
//                return false;
//                //return super.onDoubleTapEvent(e);
//            }
//
            @Override
            public boolean onDown(MotionEvent e) {
                Log.i("OnDoubleTapListener", "onDown");
                return true;
                //return super.onDown(e);
            }


        });
        //Log.i ("onCreate:","gd created.");
        //Pinch to change the timer setting;

        mContentView.setOnTouchListener(new View.OnTouchListener() {

            private float spacing(MotionEvent event) {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            }



            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("onTouch", "onTouch");
                //activateFullScreen();
                //toggle();
                if (!mBound) {
                    return true;
                }

                if (mService.getTimerState() != TimerService.TIMER_STATE_READY) {
                    return gd.onTouchEvent(event);
                }
                String TAG = "Pinich:";
                //pinch
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        //Log.i(TAG, "oldDist=" + oldDist);
                        if (oldDist > SCROLL_STEP) {
                            gestureMode = GESTURE_MODE_ZOOM;
                            //Log.i(TAG, "mode=ZOOM");
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        gestureMode = GESTURE_MODE_NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (gestureMode == GESTURE_MODE_ZOOM) {
                            float newDist = spacing(event);
                            // If you want to tweak font scaling, this is the place to go.
                            float scale = newDist - oldDist;

                            if (Math.abs(scale) > SCROLL_STEP) {

                                int delta = 1;
                                if (scale > 0) {

                                    //scale = 1.1f;
                                } else if (scale < 0) {
                                    //scale = 0.95f;
                                    delta = -1;
                                }
                                changeTimterSetting((delta));
                                oldDist = newDist;
                                //float currentSize = textView.getTextSize() * scale;
//                                if ((currentSize < MAX_FONT_SIZE && currentSize > MIN_FONT_SIZE)
//                                        ||(currentSize >= MAX_FONT_SIZE && scale < 1)
//                                        || (currentSize <= MIN_FONT_SIZE && scale > 1)) {
//                                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize);
//                                }
                            }
                        }
                        break;
                }

                return gd.onTouchEvent(event);
            }
        });

        //Log.i ("onCreate:","pinch created.");
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


    //private int timerState = TIMER_STATE_READY;
    //private long remainMilliseconds = 0;

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
            return;
        }
    }


    public void onTickUpdate(long millisUntilFinished) {

        //remainMilliseconds = millisUntilFinished;
        int progress = (int) (millisUntilFinished / 1000);
        int min = progress / 60;
        int sec = progress % 60;

        mContentView.setText(min + ":" + String.format("%02d", sec));
        if (millisUntilFinished == 0) {
            onTimerFinish();
        }

    }


    public void onTimerFinish() {

        timerFinishing = true;
        mContentView.setBackgroundColor(0xFFE64431);//red 230,68, 49

    }



}
