package com.example.kevin.myfirstrecentapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

// This is a transparent activity that only shows the dialog box
public class CurrentDrivingOrNot extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_driving_or_not);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("My title!");
        builder.setMessage("Currently driving?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If this button is clicked, close current activity
                        Intent intent = new Intent();
                        intent.setAction("com.example.kevin.myfirstrecentapp.CURRENTLY_DRIVING");
                        sendBroadcast(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If this button is clicked, just close the dialog box and do nothing
                        Intent intent = new Intent();
                        intent.setAction("com.example.kevin.myfirstrecentapp.NOT_DRIVING");
                        sendBroadcast(intent);
                        finish();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}
