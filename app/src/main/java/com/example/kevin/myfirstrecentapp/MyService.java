package com.example.kevin.myfirstrecentapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MyService extends Service {

    private static final String CURRENTLY_DRIVING = "com.example.kevin.myfirstrecentapp.CURRENTLY_DRIVING";
    private static final String NOT_DRIVING = "com.example.kevin.myfirstrecentapp.NOT_DRIVING";
    private static final String PREFERENCES = "myPreferencesFile";
    private static final String AWAY_MESSAGE_KEY = "awayMessage";
    private static final int NOTIFICATION_ID = 001;

    BroadcastReceiver phoneReciever;
    BroadcastReceiver statusChangeReciever;


    private Status currentStatus = Status.NOT_DRIVING;
    private String awayMessage = "I'm currently driving, and I can't pick up the phone right now.";

    private NotificationManager notificationManager;

    private void updateNotification(Status status) {
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(this);
        if (status == Status.DRIVING) {
            myBuilder.setSmallIcon(R.drawable.small_icon)
                    .setContentTitle("Currently driving.")
                    .setContentText("Will send a text to callers.")
                    .setOngoing(true);
        } else if (status == Status.NOT_DRIVING) {
            myBuilder.setSmallIcon(R.drawable.small_icon)
                    .setContentTitle("Currently not driving.")
                    .setContentText("Not gonna do anything lol.")
                    .setOngoing(true);
        }

        Intent resultIntent = new Intent(this, CurrentDrivingOrNot.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        myBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(NOTIFICATION_ID, myBuilder.build());
    }

    private void createNotification() {
        // Default by setting it in non-driving mode
        updateNotification(Status.NOT_DRIVING);
    }


    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        IntentFilter phoneFilter = new IntentFilter();
        IntentFilter statusChangeFilter = new IntentFilter();

        // Set up the filter so that it catches the following intent broadcasts
        phoneFilter.addAction("android.intent.action.PHONE_STATE");
        statusChangeFilter.addAction(NOT_DRIVING);
        statusChangeFilter.addAction(CURRENTLY_DRIVING);


        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        // Grabs the saved away message if it exists. If not, use the default one.
        String savedAwayMessage = settings.getString(AWAY_MESSAGE_KEY, awayMessage);
        awayMessage = savedAwayMessage;

        phoneReciever = new PhoneReceiver();
        statusChangeReciever = new StatusChangeReceiver();
        registerReceiver(statusChangeReciever, statusChangeFilter);
        registerReceiver(phoneReciever, phoneFilter);
        createNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class StatusChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NOT_DRIVING)) {
                Toast.makeText(context, "Changing status to NOT DRIVING", Toast.LENGTH_LONG).show();
                currentStatus = Status.NOT_DRIVING;
            } else if (intent.getAction().equals(CURRENTLY_DRIVING)) {
                Toast.makeText(context, "Changing status to DRIVING", Toast.LENGTH_LONG).show();
                currentStatus = Status.DRIVING;
            }
            updateNotification(currentStatus);

        }
    }

    public class PhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentStatus == Status.DRIVING) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Incoming call
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Toast.makeText(context, "Incoming call from: " + incomingNumber, Toast.LENGTH_LONG).show();
                    // Hang up, and send a text back saying "I'm driving lol"
                    //killCall(context);
                    sendSMS(incomingNumber, awayMessage);
                }
            }
        }
    }

    public void setStatus(Status newStatus) {
        currentStatus = newStatus;
    }

    public Status getStatus() { return currentStatus; }
    public String getAwayMessage() { return awayMessage; }

    // Also saves the message to the sharedpreferences file automatically
    public void setAwayMessage(String newMessage) {
        awayMessage = newMessage;
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        settings.edit().putString(AWAY_MESSAGE_KEY, awayMessage);
        settings.edit().commit();
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
