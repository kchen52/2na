package com.example.kevin.myfirstrecentapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MyIntentService extends IntentService {
        // Constructor
        public MyIntentService() {
            super("MyIntentService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            /*try {
                receiveUDP();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Toast.makeText(this, "Service Starting", Toast.LENGTH_SHORT).show();
            Log.i("lol", "starting");
            return super.onStartCommand(intent,flags,startId);
        }

        @Override
        public void onDestroy() {
            Toast.makeText(this, "Service Ending", Toast.LENGTH_SHORT).show();
            Log.i("lol", "ending");
            super.onDestroy();
        }

        public void receiveUDP() throws IOException {
            Toast.makeText(this, "HI!", Toast.LENGTH_SHORT).show();
            String text;
            int serverPort = 1234;
            byte[] message = new byte[1500];
            DatagramPacket p = new DatagramPacket(message, message.length);
            DatagramSocket s = new DatagramSocket(serverPort);

            int counter = 0;
            while (counter < 10) {
                //Log.d("LOL","LOL" + counter);
                s.receive(p);
                text = new String(message, 0, p.getLength());
                Log.d("LOL", text);
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                counter++;
            }
            s.close();
        }

}
