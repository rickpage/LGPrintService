package biz.rpcodes.apps.lgprinter.service;
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.lge.pocketphoto.bluetooth.BluetoothFileTransfer;
import com.lge.pocketphoto.bluetooth.ErrorCodes;
import com.lge.pocketphoto.bluetooth.Opptransfer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import biz.rpcodes.apps.lgprinter.LGPrintHelper;
import biz.rpcodes.apps.lgprinter.PrintIntentConstants;
import biz.rpcodes.apps.lgprinter.R;
import biz.rpcodes.apps.printhelper.tempsolution.PatientBluetoothFileTransfer;

//BEGIN_INCLUDE(service)
public class MessengerService extends Service {

    private static final String TAG = "MsngrSvcMain";
    private static final int NOTIFICATION_ID = 509;
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();


    /**
     * Does the printing
     * First connects and saves the )open) socket instance
     * Then when we send the URI, a BluetoothShare instance
     * is created and we send the print commands
     * **/
    public PatientBluetoothFileTransfer mPatientLGFileTransfer;

    // true when we finish init and expect 1+ clients from now on
    public boolean mIsInit = false;

    public MessengerService(){
        mHandler = new IncomingHandler2(this);
        mMessenger = new Messenger(mHandler);
        Log.i(TAG, "Constructor Messenger Service " + this);
    }

    public ArrayList<Messenger> getClients(){
        if ( mClients.size() == 0 && mIsInit) {
            Log.i(TAG, ", 0 clients using this messenger service");
            this.destroyPatientLGThread();
        }
        return mClients;
    }

    public void destroyPatientLGThread(){
        // Destroy LG print thread
        if ( null != this.mPatientLGFileTransfer) {

            String s = "Stopping LG print image thread";
            mHandler.debug.addString(s);
            Log.i(TAG,s );

            this.mPatientLGFileTransfer.cancelBT_Connecting();

            this.mPatientLGFileTransfer.stopTransfer();

            this.mPatientLGFileTransfer = null;
        }
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final IncomingHandler2 mHandler;// = new IncomingHandler(this);
    final Messenger mMessenger;// = new Messenger();


    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Log.d(TAG, "Created SERVICE");
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancelAll();

        // Tell the user we stopped.
        // Toast.makeText(this, "No longer monitoring Printer", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "No longer monitoring LG Printer");

        super.onDestroy();
    }
    
    /**
     * This is only called first when the service starts,
     * the next clients to register won't call this
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");

        return mMessenger.getBinder();
    }
    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onREBind called");

    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "onUnBind called: Cancelling LG print threads.");
        destroyPatientLGThread();
        // return true if subsequent calls should initiate onRebind
        // Currently not the design
        return false;
    }

    /**
     * Show a notification while this service is running.
     */
    public void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Printer Service Loading"; //getText(R.string.remote_service_started);

        mNM.cancel(NOTIFICATION_ID);
        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.prntdisconnected)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Searching for Printer")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                // .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(NOTIFICATION_ID, notification);
    }


    /**
     * Show a notification while this service is running.
     * Specify text to show, i.e. to indicate connected
     * or not connected.
     */
    private void showNotification(String title, String aText, int drawId) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = (CharSequence) aText; //getText(R.string.remote_service_started);

        mNM.cancel(NOTIFICATION_ID);
        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(drawId)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(title)  // the label of the entry
                .setContentText(text)  // the contents of the entry
                        // .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.
        // We use it later to cancel.
        mNM.notify(NOTIFICATION_ID, notification);
    }


    public void showConnectedNotification() {
        showNotification("Printer Available"
                , "The printer is within range."
                , R.drawable.prntconnected);
    }

    public void showDisconnectedNotification() {

        showNotification("Printer Unavailable"
                , "Issue connecting to printer"
                , R.drawable.prntdisconnected);

    }
    }
}
//END_INCLUDE(service)