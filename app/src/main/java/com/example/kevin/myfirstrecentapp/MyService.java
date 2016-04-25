package com.example.kevin.myfirstrecentapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MyService extends Service {
    BroadcastReceiver myReceiver;

    private Status currentStatus = Status.NOT_DRIVING;
    private String awayMessage = "I'm currently driving, and I can't pick up the phone right now.";

    @Override
    public void onCreate() {
        Toast.makeText(this, "Service is now running.", Toast.LENGTH_LONG).show();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        myReceiver = new PhoneReceiver();
        registerReceiver(myReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public class PhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!(currentStatus == Status.NOT_DRIVING)) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Incoming call
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Toast.makeText(context, "Incoming call from: " + incomingNumber, Toast.LENGTH_LONG).show();
                    // Hang up, and send a text back saying "I'm driving lol"
                    //killCall(context);
                    sendSMS(incomingNumber, awayMessage);
                }
            } else {
                // Don't do anything, and let the call go through normally
            }
        }
    }

    public void setStatus(Status newStatus) {
        currentStatus = newStatus;
    }

    public Status getStatus() { return currentStatus; }
    public String getAwayMessage() { return awayMessage; }

    public void setAwayMessage(String newMessage) {
        awayMessage = newMessage;
    }


    //http://stackoverflow.com/questions/26311243/sending-sms-programmatically-without-opening-message-app
    private void sendSMS(String phoneNo, String msg){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getApplicationContext(), "Message Sent",
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
