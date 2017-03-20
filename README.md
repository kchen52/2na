# 2na <img src="http://i.imgur.com/VbrSspD.png" alt="app_icon" width="50" height="50">

# Description
The app can be found at https://play.google.com/store/apps/details?id=com.kchen52.twona

2na is an Android application that allows a user to block all incoming phone calls while driving. By default, any incoming calls will be automatically dropped when in "Driving" mode, although this can be changed in the settings. Any missed calls are displayed as a notification to the user.

The message is set by launching the Settings activity, and set from there.
Toggle between Driving and Non-Driving modes by clicking on the corresponding action in the notification bar, or changing the value in the Settings Activity.

# Screenshots
<img src="http://imgur.com/m73YZ5Y.png" alt="app_icon" width="200" height="400">
<img src="http://imgur.com/gRDJilr.png" alt="app_icon" width="200" height="400">
<img src="http://imgur.com/8P7TCam.png" alt="app_icon" width="200" height="400">
<img src="http://imgur.com/fJjw1j3.png" alt="app_icon" width="200" height="400">




#### Permissions used:

android.permission.READ_PHONE_STATE - To detect incoming calls

android.permission.CALL_PHONE - Needed this to reject calls for some reason.

android.permission.SEND_SMS - To send a predefined message to the incoming caller when the app is set to Driving mode.

android.permission.RECEIVE_BOOT_COMPLETED - To allow the app to start its service in the background after booting up the device. 
