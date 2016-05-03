package com.kchen52.noNameYet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MyService extends Service {

    public static boolean isRunning = false;

    // Custom intents we're broadcasting and receiving
    private static final String CURRENTLY_DRIVING = "com.kchen52.noNameYet.CURRENTLY_DRIVING";
    private static final String NOT_DRIVING = "com.kchen52.noNameYet.NOT_DRIVING";
    private static final String SETTINGS_CHANGED = "com.kchen52.noNameYet.SETTINGS_CHANGED";

    // Keys for the sharedpreference
    private static final String AWAY_MESSAGE_KEY = "awayText";
    private static final String HANG_UP_KEY = "automaticallyHangUpCalls";

    // For grouping missed calls together as a notification
    private static final String MISSED_CALL_GROUP = "missedCallGroup";
    private static final int NOTIFICATION_ID = 001;
    private int missedCallNotificationID = 2;

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
                    .setContentTitle("Currently driving")
                    .setOngoing(true);
            if (hangupValue) {
                myBuilder.setContentText("Rejecting calls.");
            } else {
                myBuilder.setContentText("Not rejecting calls.");
            }
        } else if (currentStatus == Status.NOT_DRIVING) {
            myBuilder.setSmallIcon(com.kchen52.noNameYet.R.mipmap.ic_directions_walk_black_24dp)
                    .setContentTitle("Not driving")
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

        long[] pattern = {50, 50};
        myBuilder.setVibrate(pattern);

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

            }
            updateNotification();
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
                        createMissedCallNotification(incomingNumber);
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

    //http://stackoverflow.com/questions/15012082/rejecting-incoming-call-in-android
    private boolean killCall(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method method = classTelephony.getDeclaredMethod("getITelephony");
            // Disable access check
            method.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = method.invoke(telephonyManager);
            // Get the endCall method from ITelephony
            Class<?> telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void createMissedCallNotification(String numberMissed) {
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(this);
        myBuilder.setContentTitle("Missed call while driving.");
        myBuilder.setContentText("Call from " + numberMissed);

        myBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        myBuilder.setSmallIcon(R.drawable.ic_call_missed_black_24dp);

        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:" + numberMissed));

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, phoneIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        myBuilder.setContentIntent(resultPendingIntent);
        myBuilder.setGroup(MISSED_CALL_GROUP);

        Notification notification = myBuilder.build();
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(missedCallNotificationID, notification);
        // Increment it so that each new missed call has its own notification
        missedCallNotificationID++;
    }
}
