package com.example.kevin.myfirstrecentapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// This is a transparent activity that only shows the dialog box
public class SettingsActivity extends Activity {

    private static final String PREFERENCES = "myPreferencesFile";
    private static final String AWAY_MESSAGE_KEY = "awayMessage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        final Button changeAwayTextButton = (Button) findViewById(R.id.changeAwayTextBtn);
        final Button setupNFCButton = (Button) findViewById(R.id.NFCSetupBtn);

        final SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES, Activity.MODE_PRIVATE);
        // Grabs the saved away message if it exists. If not, use the default one.
        String savedAwayMessage = settings.getString(AWAY_MESSAGE_KEY, "I'm currently driving, and I can't pick up the phone right now.");

        final EditText editAwayMessageText = (EditText) findViewById(R.id.editText);

        editAwayMessageText.setText(savedAwayMessage);

        changeAwayTextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Save the text in the edit text box to the shared preferences file
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(AWAY_MESSAGE_KEY, editAwayMessageText.getText().toString());
                editor.commit();

                // Send a broadcast to the service indicating the away message has been changed
                Intent broadcastChangeOfAwayMessage= new Intent();
                broadcastChangeOfAwayMessage.setAction("com.example.kevin.myfirstrecentapp.MESSAGE_CHANGED");
                sendBroadcast(broadcastChangeOfAwayMessage);

            }
        });

        setupNFCButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Not yet implemented.", Toast.LENGTH_SHORT).show();
            }
        });


    }
}
