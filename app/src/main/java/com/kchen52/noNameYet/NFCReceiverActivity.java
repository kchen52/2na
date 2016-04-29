package com.kchen52.noNameYet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class NFCReceiverActivity extends ActionBarActivity {

    private static final String FLIP_STATUS = "com.kchen52.noNameYet.FLIP_STATUS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcreceiver);

        // Send an intent to the main service switching DRIVING/NOT_DRIVING
        Intent intent = new Intent(FLIP_STATUS);
        sendBroadcast(intent);

        finish();

    }

}
