package app.fitness.sanya.fitnessapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Date;import app.fitness.sanya.fitness.R;

/**
 * Created by Sanya on 04/04/15.
 */
public class WalkFragment extends Fragment {

   // public static final String TAG = "StepCounterRecorderService";

    // Batch sensor latency is specified in microseconds
    private static final int BATCH_LATENCY_5s = 5000000;
    private static final int BATCH_LATENCY_10s = 10000000;


    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START = "app.fitness.sanya.fitness.action.START";
    public static final String ACTION_STOP = "app.fitness.sanya.fitness.action.STOP";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "app.fitness.sanya.fitness.BROADCAST";

    public static final String STEPS = "steps";
    public static final String AGE_OF_EVENTS = "ageOfEvents";

    // Bundle tags used to store data when restoring application state
    private static final String STATE = "state";

    private static final String BUNDLE_STATE = "state";
    private static final String BUNDLE_LATENCY = "latency";
    private static final String BUNDLE_STEPS = "steps";


    // Number of events to keep in queue and display on card
    private static final int EVENT_QUEUE_LENGTH = 10;
    // List of timestamps when sensor events occurred
    private float[] eventDelays = new float[EVENT_QUEUE_LENGTH];
    // number of events in event list
    private int eventLength = 0;
    // pointer to next entry in sensor event list
    private int eventData = 0;

    // State of application, used to register for sensors when app is restored
    public static final int STATE_OTHER = 0;
    public static final int STATE_COUNTER = 1;
    public static final int STATE_DETECTOR = 2;

    // Steps counted in current session
    private int steps;
    // Value of the step counter sensor when the listener was registered.
    // (Total steps are calculated from this value.)
    private int stepCounterSteps = 0;
    // Steps counted by the step counter previously. Used to keep counter consistent across rotation
    // changes
    private int previousStepCounterSteps = 0;

    private final StringBuffer delayStringBuffer = new StringBuffer();

    // State of the app (STATE_OTHER, STATE_COUNTER or STATE_DETECTOR)
    private int mState = STATE_OTHER;
    // When a listener is registered, the batch sensor delay in microseconds
    private int mMaxDelay = 0;

    private Context content;

    private static final String TAG = "WalkFragment";

    private TextView tvSteps;
    private TextView tvAgeOfEvents;
    private int state = 0;
    private ImageButton btnStartStop;
    private TextView StartStop;

    public void setState(){
        if(btnStartStop.getBackground().equals(R.drawable.start))
            state = 0;
        else
            state = 1;

    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = layoutInflater.inflate(R.layout.fragment_walk, container, false);

        btnStartStop = (ImageButton) rootView.findViewById(R.id.btnStartStop);
        StartStop = (TextView) rootView.findViewById(R.id.lblStartStop);
        //state = 0;
        //setState();
        /*if(state == 0) {
            btnStartStop.setBackgroundResource(R.drawable.start);
        }
        else {
            btnStartStop.setBackgroundResource(R.drawable.stop);
        }*/
        //FragmentManager fm =  getFragmentManager();
      // FragmentTransaction ft = fm.beginTransaction();
        //ft.replace(R.id.map, supportMapFragment);
       // ft.commit();

       //FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
      //  final SupportMapFragment supportMapFragment = (SupportMapFragment) fragmentManager
            //    .findFragmentById(R.id.map);
       // Log.i(TAG, "Setup button listeners");


        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("state = ",String.valueOf(state));


                if (state == 0) {
                    Log.i(TAG, "Changing background to start.");
                    btnStartStop.setBackgroundResource(R.drawable.stop);
                    StartStop.setText("Stop");
                    startRecordingSteps();
                    /*Intent intent = new Intent(getActivity(), StepCounterRecordService.class);
                    startActivity(intent);*/
                    /*StepCounterRecordService nextFrag= new StepCounterRecordService();
                    this.getFragmentManager().beginTransaction()
                            .replace(R.layout.fragment_walk, nextFrag,TAG_FRAGMENT)
                            .addToBackStack(null)
                            .commit();*/
                    state = 1;
                } else {
                    Log.i(TAG, "Changing background to stop.");
                    btnStartStop.setBackgroundResource(R.drawable.start);
                    StartStop.setText("Start");
                    stopRecordingSteps();
                    state = 0;
                }

            }
        });

        // Instantiates a new DownloadStateReceiver
        StepCounterBroadcastReceiver stepCounterBroadcastReceiver =
                new StepCounterBroadcastReceiver();
        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(BROADCAST_ACTION);

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                stepCounterBroadcastReceiver,
                mStatusIntentFilter);

        tvSteps = (TextView) rootView.findViewById(R.id.tvSteps);
        tvAgeOfEvents = (TextView) rootView.findViewById(R.id.tvAgeOfEvents);

        return rootView;
    }





    public void startRecordingSteps() {

        // Tell the user service started.
        // Toast.makeText(this, "Service started.", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "service started");
        if (!isKitkatWithStepSensor()) {
            return;
        }

        //handleActionStart(this);
    }


    public void stopRecordingSteps() {
        //super.onPause();
        // Unregister the listener when the application is paused
        unregisterListeners();
    }

    final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // store the delay of this event
            recordDelay(event);
            final String delayString = getDelayString();

            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                // A step detector event is received for each step.
                // This means we need to count steps ourselves

                steps += event.values.length;


                Log.i(TAG,"New step detected by STEP_DETECTOR sensor. Total step count: " + steps);

            } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

                /*
                A step counter event contains the total number of steps since the listener
                was first registered. We need to keep track of this initial value to calculate the
                number of steps taken, as the first value a listener receives is undefined.
                 */
                if (stepCounterSteps < 1) {
                    // initial value
                    stepCounterSteps = (int) event.values[0];
                }

                // Calculate steps taken based on first counter value received.
                steps = (int) event.values[0] - stepCounterSteps;

                // Add the number of steps previously taken, otherwise the counter would start at 0.
                // This is needed to keep the counter consistent across rotation changes.
                steps = steps + previousStepCounterSteps;

                // Update the card with the latest step count

                Log.i(TAG, "New step detected by STEP_COUNTER sensor. Total step count: " + steps);

                Intent localIntent =
                        new Intent(BROADCAST_ACTION)
                                // Puts the status into the Intent
                                .putExtra(STEPS, steps)
                                .putExtra(AGE_OF_EVENTS, delayStringBuffer.toString());
                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(localIntent);
            }
        }


        /*@Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }*/


        // @Override
    /*public void onSensorChanged(SensorEvent event) {
       Log.i(TAG, "New step detected by STEP_COUNTER sensor.");
        // Tell the user service started.
        //Toast.makeText(this, "onSensorChanged", Toast.LENGTH_SHORT).show();
        // store the delay of this event
        recordDelay(event);
        final String delayString = getDelayString();

            /*
                A step counter event contains the total number of steps since the listener
                was first registered. We need to keep track of this initial value to calculate the
                number of steps taken, as the first value a listener receives is undefined.
                 */
       /* if (stepCounterSteps < 1) {
            // initial value
            stepCounterSteps = (int) event.values[0];
        }
        // Calculate steps taken based on first counter value received.
        steps = (int) event.values[0] - stepCounterSteps;

        // Add the number of steps previously taken, otherwise the counter would start at 0.
        // This is needed to keep the counter consistent across rotation changes.
        steps = steps + previousStepCounterSteps;

       Log.i(TAG, "Total step count: " + steps);

         /*
     * Creates a new Intent containing a Uri object
     * BROADCAST_ACTION is a custom Intent action
     */
        /*Intent localIntent =
                new Intent(BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra(STEPS, steps)
                        .putExtra(AGE_OF_EVENTS, delayStringBuffer.toString());
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }*/

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {


        }
    };


    /**
     * Unregisters the sensor listener if it is registered.
     */
    private void unregisterListeners() {
        SensorManager sensorManager =
                (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.unregisterListener(mListener);
        Log.i(TAG, "Sensor listener unregistered.");
        WorkoutDBAdapter workoutDBAdapter = new WorkoutDBAdapter(getActivity());
        workoutDBAdapter.saveCurrentSessionSteps(new Date(),steps,  convertToCalories(steps) , convertToDistance(steps));

        Log.i(TAG, "Saved steps information to database.");

    }



    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private void registerEventListener(int maxdelay, int sensorType) {

        // Keep track of state so that the correct sensor type and batch delay can be set up when
        // the app is restored (for example on screen rotation).
        mMaxDelay = maxdelay;
        if (sensorType == Sensor.TYPE_STEP_COUNTER) {
            mState = STATE_COUNTER;
            /*
            Reset the initial step counter value, the first event received by the event listener is
            stored in mCounterSteps and used to calculate the total number of steps taken.
             */
            stepCounterSteps = 0;
            Log.i(TAG, "Event listener for step counter sensor registered with a max delay of "
                    + mMaxDelay);
        } else {
            mState = STATE_DETECTOR;
            Log.i(TAG, "Event listener for step detector sensor registered with a max delay of "
                    + mMaxDelay);
        }

        // Get the default sensor for the sensor type from the SenorManager
        SensorManager sensorManager =
                (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        // sensorType is either Sensor.TYPE_STEP_COUNTER or Sensor.TYPE_STEP_DETECTOR
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);

        // Register the listener for this sensor in batch mode.
        // If the max delay is 0, events will be delivered in continuous mode without batching.
        final boolean batchMode = sensorManager.registerListener(
                mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, maxdelay);

        if (!batchMode) {
            // Batch mode could not be enabled, show a warning message and switch to continuous mode
            Log.w(TAG, "Could not register sensor listener in batch mode, " +
                    "falling back to continuous mode.");
        }





    }

    private boolean isKitkatWithStepSensor() {
        // Require at least Android KitKat
        int currentApiVersion = Build.VERSION.SDK_INT;
        Log.i(TAG, "Device current Android version: " + currentApiVersion);
        // Check that the device supports the step counter and detector sensors
        PackageManager packageManager = getActivity().getPackageManager();
        return currentApiVersion >= Build.VERSION_CODES.KITKAT && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    private void resetCounter() {
        steps = 0;
        stepCounterSteps = 0;
        eventLength = 0;
        eventDelays = new float[EVENT_QUEUE_LENGTH];
        previousStepCounterSteps = 0;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Fragment is being restored, reinitialise its state with data from the bundle




            Log.i(TAG, "Reinstating fragment state with data from bundle.");
            Log.i(TAG, String.valueOf(state));

            if (savedInstanceState != null) {
                super.onViewStateRestored(savedInstanceState);
                resetCounter();
                state = savedInstanceState.getInt(STATE);
                steps = savedInstanceState.getInt(BUNDLE_STEPS);
                mState = savedInstanceState.getInt(BUNDLE_STATE);
                mMaxDelay = savedInstanceState.getInt(BUNDLE_LATENCY);

                // Register listeners again if in detector or counter states with restored delay
                if (mState == STATE_DETECTOR) {
                    registerEventListener(mMaxDelay, Sensor.TYPE_STEP_DETECTOR);
                } else if (mState == STATE_COUNTER) {
                    // store the previous number of steps to keep  step counter count consistent
                    previousStepCounterSteps = steps;
                    registerEventListener(mMaxDelay, Sensor.TYPE_STEP_COUNTER);
                }
            }

    }


    /**
     * Resets the step counter by clearing all counting variables and lists.
     */





    /**
     * Records the delay for the event.
     *
     * @param event
     */
    private void recordDelay(SensorEvent event) {
        // Calculate the delay from when event was recorded until it was received here in ms
        // Event timestamp is recorded in us accuracy, but ms accuracy is sufficient here
        eventDelays[eventData] = System.currentTimeMillis() - (event.timestamp / 1000000L);

        // Increment length counter
        eventLength = Math.min(EVENT_QUEUE_LENGTH, eventLength + 1);
        // Move pointer to the next (oldest) location
        eventData = (eventData + 1) % EVENT_QUEUE_LENGTH;
    }

    /**
     * Returns a string describing the sensor delays recorded in
     * {@link #recordDelay(android.hardware.SensorEvent)}.
     *
     * @return
     */
    private String getDelayString() {
        // Empty the StringBuffer
        delayStringBuffer.setLength(0);

        // Loop over all recorded delays and append them to the buffer as a decimal
        for (int i = 0; i < eventLength; i++) {
            if (i > 0) {
                delayStringBuffer.append(", ");
            }
            final int index = (eventData + i) % EVENT_QUEUE_LENGTH;
            final float delay = eventDelays[index] / 1000f; // convert delay from ms into s
            delayStringBuffer.append(String.format("%1.1f", delay));
        }

        return delayStringBuffer.toString();
    }





    private float convertToCalories(long steps) {
        return (float) steps / 20;
    }

    private float convertToDistance(long steps) {
        return (float) (steps*(1/2.5)*(1/1000));
    }

    /**
     * Records the state of the application into the {@link android.os.Bundle}.
     *
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        // Store all variables required to restore the state of the application
        outState.putInt(STATE,state);
        outState.putInt(BUNDLE_LATENCY, mMaxDelay);
        outState.putInt(BUNDLE_STATE, mState);
        outState.putInt(BUNDLE_STEPS, steps);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    // Broadcast receiver for receiving status updates from the IntentService
    private class StepCounterBroadcastReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private StepCounterBroadcastReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            tvSteps.setText(Long.toString(intent.getExtras().getLong(STEPS)));
            tvAgeOfEvents.setText(intent.getExtras().getString(AGE_OF_EVENTS));
        }
    }
}
