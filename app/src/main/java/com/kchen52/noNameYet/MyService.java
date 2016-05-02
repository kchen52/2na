package com.kchen52.noNameYet;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MyService extends Service {

    public static boolean isRunning = false;

    private static final String CURRENTLY_DRIVING = "com.kchen52.noNameYet.CURRENTLY_DRIVING";
    private static final String NOT_DRIVING = "com.kchen52.noNameYet.NOT_DRIVING";
    private static final String FLIP_STATUS = "com.kchen52.noNameYet.FLIP_STATUS";
    private static final String SETTINGS_CHANGED = "com.kchen52.noNameYet.SETTINGS_CHANGED";
    private static final String AWAY_MESSAGE_KEY = "awayText";
    private static final String HANG_UP_KEY = "automaticallyHangUpCalls";
    private static final int NOTIFICATION_ID = 001;

    BroadcastReceiver phoneReceiver;
    BroadcastReceiver statusChangeReceiver;

    private Status currentStatus = Status.NOT_DRIVING;
    private String awayMessage = "I'm currently driving, and I can't pick up the phone right now.";
    private boolean hangupValue = false;

    private NotificationManager notificationManager;


    private void updateNotification() {
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(this);

        if (currentStatus == Status.DRIVING) {
            myBuilder.setSmallIcon(com.kchen52.noNameYet.R.mipmap.ic_directions_car_black_24dp)
                    .setContentTitle("Currently driving.")
                    .setOngoing(true);
            if (hangupValue) {
                myBuilder.setContentText("Hanging up incoming calls and sending text.");
            } else {
                myBuilder.setContentText("Will send a text message to callers.");
            }
        } else if (currentStatus == Status.NOT_DRIVING) {
            myBuilder.setSmallIcon(com.kchen52.noNameYet.R.mipmap.ic_directions_walk_black_24dp)
                    .setContentTitle("Currently not driving.")
                    //.setContentText("Not gonna do anything lol.")
                    .setOngoing(true);
        }

        // Setting up some intents that'll happen when user hits on them in the notification
        // Starts up the main activity where the user can change settings and stuff
        Intent startMainActivity = new Intent(this, SettingsActivity_OLD.class);
        PendingIntent startMainActivity_PI =
            PendingIntent.getActivity(this, 0, startMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        // Changes the current status to driving
        Intent broadcastDriving = new Intent();
        broadcastDriving.setAction(CURRENTLY_DRIVING);
        PendingIntent broadcastDriving_PI =
            PendingIntent.getBroadcast(this, 0, broadcastDriving, PendingIntent.FLAG_UPDATE_CURRENT);

        // Changes the current status to not driving
        Intent broadcastNotDriving = new Intent();
        broadcastNotDriving.setAction(NOT_DRIVING);
        PendingIntent broadcastNotDriving_PI =
                PendingIntent.getBroadcast(this, 0, broadcastNotDriving, PendingIntent.FLAG_UPDATE_CURRENT);


        // Using the NotificationCompat.builder vibrate doesn't seem to work
        // So I'm using this workaround for now
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);


        myBuilder.addAction(com.kchen52.noNameYet.R.mipmap.ic_directions_car_black_24dp, "Driving", broadcastDriving_PI);
        myBuilder.addAction(com.kchen52.noNameYet.R.mipmap.ic_directions_walk_black_24dp, "Not Driving", broadcastNotDriving_PI);
        myBuilder.setCategory(Notification.CATEGORY_SERVICE);
        // Makes it so that the phone doesn't have to be unlocked to change status
        myBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // When the user hits the notification itself (not the buttons), start up the settings
        // activity.
        myBuilder.setContentIntent(startMainActivity_PI);

        notificationManager.notify(NOTIFICATION_ID, myBuilder.build());
    }

    private void createNotification() {
        // Default by setting it in non-driving mode
        currentStatus = Status.NOT_DRIVING;
        updateNotification();
    }


    @Override
    public void onCreate() {
        isRunning = true;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        IntentFilter phoneFilter = new IntentFilter();
        IntentFilter statusChangeFilter = new IntentFilter();

        // Set up each filter so that it catches the following intent broadcasts
        phoneFilter.addAction("android.intent.action.PHONE_STATE");

        statusChangeFilter.addAction(NOT_DRIVING);
        statusChangeFilter.addAction(CURRENTLY_DRIVING);
        statusChangeFilter.addAction(SETTINGS_CHANGED);
        statusChangeFilter.addAction(FLIP_STATUS);

        // TODO: Allow the user to choose whether the app starts on boot automatically
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (!settings.contains(AWAY_MESSAGE_KEY)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(AWAY_MESSAGE_KEY, awayMessage);
            editor.commit();
        }

        // Grabs the saved away message if it exists. If not, use the default one.
        String savedAwayMessage = settings.getString(AWAY_MESSAGE_KEY, awayMessage);
        awayMessage = savedAwayMessage;
        hangupValue = settings.getBoolean(HANG_UP_KEY, false);

        phoneReceiver = new PhoneReceiver();
        statusChangeReceiver = new StatusChangeReceiver();

        registerReceiver(statusChangeReceiver, statusChangeFilter);
        registerReceiver(phoneReceiver, phoneFilter);
        createNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class StatusChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NOT_DRIVING)) {
                currentStatus = Status.NOT_DRIVING;
            } else if (intent.getAction().equals(CURRENTLY_DRIVING)) {
                currentStatus = Status.DRIVING;
            } else if (intent.getAction().equals(SETTINGS_CHANGED)) {
                // Grab all the new settings
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                // Grabs the saved away message if it exists. If not, use the default one.
                String newAwayMessage= settings.getString(AWAY_MESSAGE_KEY, awayMessage);
                boolean newHangupValue = settings.getBoolean(HANG_UP_KEY, false);

                awayMessage = newAwayMessage;
                hangupValue = newHangupValue;

            } else if (intent.getAction().equals(FLIP_STATUS)) {
                flipStatus();
            }
            updateNotification();
        }
    }

    void flipStatus() {
        if (currentStatus == Status.DRIVING) {
            currentStatus = Status.NOT_DRIVING;
        } else if (currentStatus == Status.NOT_DRIVING) {
            currentStatus = Status.DRIVING;
        }
    }

    public class PhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentStatus == Status.DRIVING) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Incoming call
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                    // If the user has set the app to automatically hang up calls, do so
                    if (hangupValue) {
                        killCall(context);
                    }
                    // Regardless, send a SMS to the incoming caller with some user defined message
                    sendSMS(incomingNumber, awayMessage);
                }
            }
        }
    }

    //http://stackoverflow.com/questions/26311243/sending-sms-programmatically-without-opening-message-app
    private void sendSMS(String phoneNo, String msg){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getApplicationContext(), "Away message sent to " + phoneNo,
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    //http://stackoverflow.com/questions/33266447/how-to-programmatically-reject-hang-up-incoming-call-on-android-in-delphi
    private boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d("Tag","PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }
}
