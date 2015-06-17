package com.example.sophia_xu.teststepsensor;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Sophia_Xu on 2015/6/4.
 */
public class StepSensorService extends Service implements SensorEventListener{

    private String TAG = "SophiaServiceStepCounter";

    private SensorManager sensorManager;
    private Sensor mySensor;

    private int maxdelay = 0;
    private int mSteps = 0;
    private int newDaySteps =0;
    private int mPreSteps = 0;
    private int shareStepsNum = 0;
    private SharedPreferences preferences;
    SharedPreferences.Editor editor;




    // max batch latency is specified in microseconds
    private static final int BATCH_LATENCY_0 = 0; // no batching
    private static final int BATCH_LATENCY_10s = 10000000;
    private static final int BATCH_LATENCY_5s = 5000000;


    @Override
    public void onCreate() {
       super.onCreate();
        preferences = getSharedPreferences("com.example.sophia_xu.teststepsensor",MODE_PRIVATE);
        editor = preferences.edit();
       Log.i(TAG,"service is created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"service is onstartcommand");
        Timer timer = new Timer();     //to control the setting to 0 everyday
        TimerTask timerTask = new TimerTask(){

            @Override
            public void run() {
                newDaySteps = mPreSteps;
                Message timerMsg = new Message();
                timerMsg.what = WearActivityMainActivity.TIMERFLAG;
                WearActivityMainActivity.handler.sendMessage(timerMsg);

            }
        };
//        timer.schedule(timerTask,new Date(00:00:00),);  // have no idea about the 0:0

        new Thread(new Runnable() {
            @Override
            public void run() {
                maxdelay = BATCH_LATENCY_10s;
                sensorManager = (SensorManager) getApplication().getSystemService(SENSOR_SERVICE);
                mySensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);  // make sure of the sensor type
                Log.i(TAG,"step_counter sensor 's max event count is "+ mySensor.getFifoMaxEventCount());
                sensorManager.registerListener(StepSensorService.this,mySensor,SensorManager.SENSOR_DELAY_NORMAL,BATCH_LATENCY_10s);
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"service is destroyed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Log.i(TAG, "event values 's timestamp " + event.timestamp);

        mPreSteps = (int) event.values[0];
        shareStepsNum = preferences.getInt("steps",0);
        if(mPreSteps == 0 && shareStepsNum != 0)   // control the reboot ,event.values[0] is set to 0;
            mSteps = shareStepsNum;
        else
            mSteps = (int) event.values[0] - newDaySteps;

        // Add the number of steps previously taken, otherwise the counter would start at 0.
        // This is needed to keep the counter consistent across rotation changes.
//        mSteps = mSteps + mPreviousCounterSteps;
//        Log.i(TAG,"the mPreviousCounterSteps is " + mPreviousCounterSteps);

        // Update the card with the latest step count
//                mTVstepNum.setText(mSteps);

//            mTVstepNum.setText(getString(R.string.counting_title,mSteps)); // 这种方式很不错。

        Message msg = new Message();
        msg.what = WearActivityMainActivity.FLAG;
        msg.obj = mSteps;
        WearActivityMainActivity.handler.sendMessage(msg);
        editor.putInt("steps",mSteps);
        Log.i(TAG, "New step detected by STEP_COUNTER sensor. Total step count: " + mSteps);



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
