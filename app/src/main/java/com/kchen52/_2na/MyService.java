package com.kchen52._2na;

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
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MyService extends Service {

    public static boolean isRunning = false;

    private static final String CURRENTLY_DRIVING = "com.kchen52._2na.CURRENTLY_DRIVING";
    private static final String NOT_DRIVING = "com.kchen52._2na.NOT_DRIVING";
    private static final String MESSAGE_CHANGED = "com.kchen52._2na.MESSAGE_CHANGED";
    private static final String PREFERENCES = "myPreferencesFile";
    private static final String AWAY_MESSAGE_KEY = "awayMessage";
    private static final int NOTIFICATION_ID = 001;

    BroadcastReceiver phoneReceiver;
    BroadcastReceiver statusChangeReceiver;

    private Status currentStatus = Status.NOT_DRIVING;
    private String awayMessage = "I'm currently driving, and I can't pick up the phone right now.";

    private NotificationManager notificationManager;

    private void updateNotification(Status status) {
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(this);

        if (status == Status.DRIVING) {
            myBuilder.setSmallIcon(com.kchen52._2na.R.mipmap.ic_directions_car_black_24dp)
                    .setContentTitle("Currently driving.")
                    .setContentText("Will send a text message to callers.")
                    .setOngoing(true);
        } else if (status == Status.NOT_DRIVING) {
            myBuilder.setSmallIcon(com.kchen52._2na.R.mipmap.ic_directions_walk_black_24dp)
                    .setContentTitle("Currently not driving.")
                    //.setContentText("Not gonna do anything lol.")
                    .setOngoing(true);
        }

        // Setting up some intents that'll happen when user hits on them in the notification

        // Starts up the main activity where the user can change settings and stuff
        Intent startMainActivity = new Intent(this, SettingsActivity.class);
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

        myBuilder.addAction(com.kchen52._2na.R.mipmap.ic_directions_car_black_24dp, "Driving", broadcastDriving_PI);
        myBuilder.addAction(com.kchen52._2na.R.mipmap.ic_directions_walk_black_24dp, "Not Driving", broadcastNotDriving_PI);
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
        updateNotification(Status.NOT_DRIVING);
    }


    @Override
    public void onCreate() {
        isRunning = true;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        IntentFilter phoneFilter = new IntentFilter();
        IntentFilter statusChangeFilter = new IntentFilter();
        IntentFilter bootCompletedFilter = new IntentFilter();

        // Set up each filter so that it catches the following intent broadcasts
        phoneFilter.addAction("android.intent.action.PHONE_STATE");
        statusChangeFilter.addAction(NOT_DRIVING);
        statusChangeFilter.addAction(CURRENTLY_DRIVING);
        statusChangeFilter.addAction(MESSAGE_CHANGED);

        // TODO: Allow the user to choose whether the app starts on boot automatically
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES, Activity.MODE_PRIVATE);

        if (!settings.contains(AWAY_MESSAGE_KEY)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(AWAY_MESSAGE_KEY, awayMessage);
            editor.commit();
        }

        // Grabs the saved away message if it exists. If not, use the default one.
        String savedAwayMessage = settings.getString(AWAY_MESSAGE_KEY, awayMessage);
        awayMessage = savedAwayMessage;

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
                updateNotification(currentStatus);
            } else if (intent.getAction().equals(CURRENTLY_DRIVING)) {
                currentStatus = Status.DRIVING;
                updateNotification(currentStatus);
            } else if (intent.getAction().equals(MESSAGE_CHANGED)) {
                SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES, Activity.MODE_PRIVATE);
                // Grabs the saved away message if it exists. If not, use the default one.
                String newAwayMessage= settings.getString(AWAY_MESSAGE_KEY, awayMessage);
                awayMessage = newAwayMessage;
            }
        }
    }

    public class PhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentStatus == Status.DRIVING) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Incoming call
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    // Hang up, and send a text back saying "I'm driving lol"
                    //killCall(context);
                    sendSMS(incomingNumber, awayMessage);
                }
            }
        }
    }

    // Also saves the message to the sharedpreferences file automatically
    public void setAwayMessage(String newMessage) {
        awayMessage = newMessage;
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(AWAY_MESSAGE_KEY, awayMessage);
        editor.commit();
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
