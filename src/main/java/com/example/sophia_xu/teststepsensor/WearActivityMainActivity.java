package com.example.sophia_xu.teststepsensor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;


import java.util.logging.LogRecord;

public class WearActivityMainActivity extends Activity {

    private TextView mTextView;
    private TextView mTVstepNum;
    public static final int FLAG = 1;
    public static final int TIMERFLAG = 2;
    // Steps counted in current session
    private int mSteps = 0;
    // Value of the step counter sensor when the listener was registered.
    // (Total steps are calculated from this value.)
    private int mCounterSteps = 0;
    // Steps counted by the step counter previously. Used to keep counter consistent across rotation
    // changes
    private int mPreviousCounterSteps = 0;
    private int mMaxDelay = 0;
    private int mState = 0;

    public static final String TAG = "SophiaStepSensor";




    // max batch latency is specified in microseconds
    private static final int BATCH_LATENCY_0 = 0; // no batching
    private static final int BATCH_LATENCY_10s = 10000000;
    private static final int BATCH_LATENCY_5s = 5000000;


    // State of application, used to register for sensors when app is restored
    public static final int STATE_OTHER = 0;
    public static final int STATE_COUNTER = 1;
    public static final int STATE_DETECTOR = 2;

    // Bundle tags used to store data when restoring application state
    private static final String BUNDLE_STATE = "state";
    private static final String BUNDLE_LATENCY = "latency";
    private static final String BUNDLE_STEPS = "steps";

    public static Handler handler;
    private Intent intent;
    private SharedPreferences preferences;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rect_activity_wear_activity_main);
        Log.i(TAG,"activity is created!");
        mTVstepNum = (TextView) findViewById(R.id.stepNum);
//        if(savedInstanceState != null){
//            resetCounter();
//            mSteps=savedInstanceState.getInt(BUNDLE_STEPS);
//            mState = savedInstanceState.getInt(BUNDLE_STATE);
//            mMaxDelay = savedInstanceState.getInt(BUNDLE_LATENCY);
//            Log.i(TAG,"savedInstanceState is not null and msteps is " + mSteps);
//            if (mState == STATE_DETECTOR) {
//                registerEventListner(mMaxDelay, Sensor.TYPE_STEP_DETECTOR);
//            } else if (mState == STATE_COUNTER) {
//                // store the previous number of steps to keep  step counter count consistent
//                mPreviousCounterSteps = mSteps;
//                registerEventListner(mMaxDelay, Sensor.TYPE_STEP_COUNTER);
//            }
//
//        }

        if(isKitkatWithStepSensor()) {
            mTVstepNum.setText(String.valueOf(mSteps));  // the step is 0 at the beginning
//            registerEventListner(BATCH_LATENCY_10s, Sensor.TYPE_STEP_COUNTER);
            intent = new Intent(WearActivityMainActivity.this,StepSensorService.class);
            startService(intent);
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what){
                        case FLAG:
                            mTVstepNum.setText(getString(R.string.counting_title,msg.obj));
                            removeMessages(msg.what);  // if will the message queue be fulled?
                            break;
                        case TIMERFLAG:
                            mTVstepNum.setText(getString(R.string.counting_title,0));
                            removeMessages(msg.what);
                            break;
                   }

                }
            };
        }
        else
            mTVstepNum.setText("no sensor,be sure of kitkat version");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"activity is started.");
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,"activity is onResumed.");
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister the listener when the application is paused
//        unregisterListeners();

        Log.i(TAG,"activity is paused.");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"activity is stopped.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intent);
        handler.removeMessages(FLAG);
        Log.i(TAG,"service and activity are destroyed.");
}

    private boolean isKitkatWithStepSensor() {

        // Require at least Android KitKat
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        // Check that the device supports the step counter and detector sensors
        PackageManager packageManager = getPackageManager();
        return currentApiVersion >= android.os.Build.VERSION_CODES.KITKAT
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);

    }

    private void registerEventListner(int maxdelay,int sensorType){

// Keep track of state so that the correct sensor type and batch delay can be set up when
        // the app is restored (for example on screen rotation).
        mMaxDelay = maxdelay;
        if (sensorType == Sensor.TYPE_STEP_COUNTER) {
            mState = STATE_COUNTER;
            /*
            Reset the initial step counter value, the first event received by the event listener is
            stored in mCounterSteps and used to calculate the total number of steps taken.
             */
            mCounterSteps = 0;
            Log.i(TAG, "Event listener for step counter sensor registered with a max delay of "
                    + mMaxDelay);
        } else {
            mState = STATE_DETECTOR;
            Log.i(TAG, "Event listener for step detector sensor registered with a max delay of "
                    + mMaxDelay);
        }

        // Get the default sensor for the sensor type from the SenorManager
        SensorManager sensorManager =
                (SensorManager)getSystemService(Activity.SENSOR_SERVICE);
        // sensorType is either Sensor.TYPE_STEP_COUNTER or Sensor.TYPE_STEP_DETECTOR
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);

        // Register the listener for this sensor in batch mode.
        // If the max delay is 0, events will be delivered in continuous mode without batching.
        final boolean batchMode = sensorManager.registerListener(
                mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, maxdelay);

    }



    /**
     * Listener that handles step sensor events for step detector and step counter sensors.
     */
    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

//            // store the delay of this event
//            recordDelay(event);
//            final String delayString = getDelayString();


            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                // A step detector event is received for each step.
                // This means we need to count steps ourselves

                mSteps += event.values.length;


                // Update the TextView with the latest step count

                if(mTVstepNum != null)
                     mTVstepNum.setText(getString(R.string.counting_title,mSteps)); // this is a nice way
                else
                    Log.i("sophia",
                            "textview is null");


                Log.i(TAG,
                        "New step detected by STEP_DETECTOR sensor. Total step count: " + mSteps);

            } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

                /*
                A step counter event contains the total number of steps since the listener
                was first registered. We need to keep track of this initial value to calculate the
                number of steps taken, as the first value a listener receives is undefined.
                 */
//                if (mCounterSteps < 1) {
//                    // initial value
//                    mCounterSteps = (int) event.values[0];
//                }

                // Calculate steps taken based on first counter value received.
//                mSteps = (int) event.values[0] - mCounterSteps;
                Log.i(TAG,"event values 's length " + event.values.length);
                  mSteps = (int) event.values[0];
                // Add the number of steps previously taken, otherwise the counter would start at 0.
                // This is needed to keep the counter consistent across rotation changes.
                mSteps = mSteps + mPreviousCounterSteps;
                Log.i(TAG,"the mPreviousCounterSteps is " + mPreviousCounterSteps);

                // Update the card with the latest step count
//                mTVstepNum.setText(mSteps);
                if(mTVstepNum != null)
                    mTVstepNum.setText(getString(R.string.counting_title,mSteps)); // 这种方式很不错。
                else
                    Log.i("sophia",
                            "textview is null");

                Log.i(TAG, "New step detected by STEP_COUNTER sensor. Total step count: " + mSteps);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    /**
     * Unregisters the sensor listener if it is registered.
     */
    private void unregisterListeners() {

        SensorManager sensorManager =
                (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.unregisterListener(mListener);
        Log.i(TAG, "Sensor listener unregistered.");


    }



    /**
     * Resets the step counter by clearing all counting variables and lists.
     */
    private void resetCounter() {

        mSteps = 0;
        mCounterSteps = 0;

        mPreviousCounterSteps = 0;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "activity excute onSaveInstanceState");

        // Store all variables required to restore the state of the application
        outState.putInt(BUNDLE_LATENCY, mMaxDelay);
        outState.putInt(BUNDLE_STATE, mState);
        outState.putInt(BUNDLE_STEPS, mSteps);
    }
}
