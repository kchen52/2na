# 2na
An Android application that sends a text to the incoming caller when the owner is driving, indicating that they're driving and are currently unable to pick up.

Toggle between modes by clicking on the corresponding action in the notification bar.
The message can be set by clicking on the notification bar itself, or by launching the app while the service is already running.

Status toggling can also be done by tapping your NFC-enabled device with a NFC tag (more on this when I finish it).

#### Permissions used:
android.permission.READ_PHONE_STATE - To detect incoming calls

android.permission.SEND_SMS - To send a predefined message to the incoming caller when the app is set to Driving mode.

android.permission.VIBRATE - To give the user feedback when they switch between Driving/Non-Driving states.

android.permission.NFC - To read NFC tags.

android.permission.RECEIVE_BOOT_COMPLETED - To allow the app to start its service in the background after booting up the device. [Controversial - may remove this functionality]
