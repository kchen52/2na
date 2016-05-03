# 2na
Can be found at https://play.google.com/store/apps/details?id=com.kchen52.noNameYet

An Android application that sends a text to the incoming caller that the owner is driving, and they're unable to pick up the phone at the moment by sending a user-defined text.

The message is set by launching the Settings activity, and set from there.
Toggle between modes by clicking on the corresponding action in the notification bar.


#### Permissions used:

android.permission.READ_PHONE_STATE - To detect incoming calls

android.permission.CALL_PHONE - Needed this to reject calls for some reason.

android.permission.SEND_SMS - To send a predefined message to the incoming caller when the app is set to Driving mode.

android.permission.RECEIVE_BOOT_COMPLETED - To allow the app to start its service in the background after booting up the device. 
