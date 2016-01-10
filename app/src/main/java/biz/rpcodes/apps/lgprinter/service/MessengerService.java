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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.lge.pocketphoto.bluetooth.BluetoothFileTransfer;
import com.lge.pocketphoto.bluetooth.Opptransfer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IllegalFormatException;

import biz.rpcodes.apps.lgprinter.PrintIntentConstants;
import biz.rpcodes.apps.lgprinter.R;
import biz.rpcodes.apps.printhelper.tempsolution.CheckLGConnection;


/**
 * This is an example of implementing an application service that uses the
 * {@link Messenger} class for communicating with clients.  This allows for
 * remote interaction with a service, without needing to define an AIDL
 * interface.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
//BEGIN_INCLUDE(service)
public class MessengerService extends Service {

    private static final String TAG = "MsngrSvcMain";
    private static final int NOTIFICATION_ID = 509;
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    /** Connect to LG and then disconnect, no transfer
     Do this just to check periodically if printer in range
     */
    public CheckLGConnection mCheckLG;

    public BluetoothFileTransfer mLGFileTransfer;


    {
        Log.i(TAG, "Instantiating Messenger Service: " + this);
    }

    /** Holds last value set by a client. */
    int mValue = 0;

    private ArrayList<Messenger> getClients(){
        if ( mClients.size() == 0 ) {
            Log.i(TAG, "Stopping service, 0 clients using this messenger service");
            this.stopSelf();
        }
        return mClients;
    }

    private int getValue(){
        return mValue;
    }

    private void setValue(int v){
        mValue = v;
    }


    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        private static final String TAG = "LGPrintMsgHandler";
        private static final long CHECK_DELAY_MS = 3000;
        private static final long PRINT_TIMEOUT_TRANSFER_MS = 4 * CHECK_DELAY_MS;
        private boolean mStarted;
        WeakReference<MessengerService> mService;

        /**
         indicates if printer can be reached recently, not
         wether the socket is actually connected and still valid
        note the issue is that even when connected, an out of range
         or off printer does not trigger messages or Exception
        */
        private boolean mIsConnected = false;

        /**
         * Indicates that the last print job has failed.
         *
         */
        private boolean mIsLastPrintJobSuccessful = false;
        /**
         * indicates if a client has started printing yet
         */
        private boolean isPrinting;
        private int mErrorCode = 0 ; // 0 is undefined error
        private boolean isWaitingToPrint = false;
        private boolean mFirstTime = true;
        private boolean mIsChecking =  false;


        public IncomingHandler(MessengerService s){
            Log.i(TAG, "New handler: " + this);
            mStarted = false;
            mService = new WeakReference<MessengerService>(s);
        }

        public MessengerService svc(){



            return mService.get();
        }

        // Sends current connected value to all clients
        // This can be done whenever the status changes,
        // the status changes periodically until print job
        // is submitted
        public void sendPrinterStatusMessage(){
            for (int i=svc().getClients().size()-1; i>=0; i--) {
                try {
                    svc().getClients().get(i).send(
                            this.obtainMessage(PrintIntentConstants.MSG_RESPONSE_STATUS
                                    , mIsConnected ?
                                    PrintIntentConstants.AVAILABLE
                                    : PrintIntentConstants.UNAVAILABLE
                                    , mErrorCode)
                    );
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mService.get().getClients().remove(i);
                }
            }
        }

        private void sendPrintJobStatus() {
            for (int i=svc().getClients().size()-1; i>=0; i--) {
                try {
                    svc().getClients().get(i).send(
                            this.obtainMessage(PrintIntentConstants.MSG_RESPONSE_PRINT_JOB
                                    , mIsLastPrintJobSuccessful ?
                                    PrintIntentConstants.SUCCESS
                                    : PrintIntentConstants.FAILURE
                                    , mErrorCode
                            ));
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mService.get().getClients().remove(i);
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            /* TODO:
             * Registered clients get periodic messages about printer status
             * that they can use to update UI and Logic.
             *
             */

            switch (msg.what) {
                case PrintIntentConstants.MSG_REGISTER_CLIENT:
                    if ( msg.replyTo == null ){
                        throw new IllegalStateException("Message to LG Print Service MUST include reply to address");
                    }
                    svc().getClients().add(msg.replyTo);
//
//                    Log.i(TAG, "STarted? " + mStarted);
//                    // Assume the flow is already kicked off
//                    // if we have more than one client
//                    if ( !mStarted ){
//                        Log.i(TAG, "V5 Starting connection checks");
//                        mStarted = true;
//                        mFirstTime = true;
//                        this.sendMessage(obtainMessage(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION));
//                    }
                    break;
                case PrintIntentConstants.MSG_UNREGISTER_CLIENT:
                    if ( msg.replyTo == null ){
                        throw new IllegalStateException("Message to LG Print Service MUST include reply to address");
                    }
                    svc().getClients().remove(msg.replyTo);

                    break;
                // User Print
                case PrintIntentConstants.MSG_REQUEST_PRINT_JOB:
                    // get the  filepath
                    // String mFileName = (String) msg.obj;
                    Bundle bund = msg.getData();
                    String mFileName = bund.getString("filepath");
                    if (isPrinting){ // Make sure we aren't already printing
                        Log.e("PRINTREQUEST", "Already printing, wait until job complete.");
                    } else {
                        if ( mFileName == null){
                            Log.e("PRINTREQUEST", "mFileName is null");
                            // break;
                        } else {
                            Log.i(TAG, "PRINT isPrinting TRUE, Printing " + mFileName);

                            isPrinting = true;
//                            isWaitingToPrint = true;
                            if ( null != svc().mCheckLG){
                                Log.i(TAG, "Stopping check connection thread");
                                svc().mCheckLG.cancelBT_Connecting();
                                svc().mCheckLG.stopTransfer();

                            }

                            // Dont let check connection flow trigger
                            // a false print job failure
                            // TODO: Any reason not to clear ALL scheduled messages?
//                            this.removeMessages(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION);
//                            this.removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL);
//                            this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                            this.removeCallbacksAndMessages(null);

                            Uri imgUri = Uri.parse("file://" + mFileName);
                            svc().mLGFileTransfer = new BluetoothFileTransfer(this.svc()
                                    , null, imgUri, this);
                            // This ALSO starts the transfer
                            svc().mLGFileTransfer.getPairedDevices();

                            // set a timeout, in case the printer shuts off and wew dont catch it
                            /// (kicks off check lg again)
                            this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_SEND_FAIL), PRINT_TIMEOUT_TRANSFER_MS);
                        }
                    }
                    break;
                // LG Internal Messages
                case Opptransfer.BLUETOOTH_SOCKET_CONNECTED:
                    // CHECK START SUCCESS
                    // Print START SUCCESS
                    mIsChecking = false;
                    if ( false == isPrinting) {
                        if ( false == mIsConnected || mFirstTime ) {
                            mFirstTime = false;
                            mIsConnected = true;
                            // Toast.makeText(MainActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
                            // SHow notification
                            svc().showNotification("Printer Available", "The printer is within range.");
                        }

                        // announce to clients
                        sendPrinterStatusMessage();
                    }
                    break;
                case Opptransfer.BLUETOOTH_CONNECTION_INTERRUPTED:
                    mIsChecking = false;
                    // CHECK FAILURE/INTERRUPT
                    // We were interupted, but dont change connection, because
                    // we generally interupt when we are ready to print
                    Log.d(TAG, "BT check connection interupted");
                    break;
                case Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL:
                    // CHECK FAILURE
                    // PRINT FAILURE
                    // We get here in BOTH cases if we cannot connect to
                    // the device, including if not paired.
                    // Automatically retry?
                    // if we are trying to print, send failure
                    // otherwise, send connection status and try again

                    removeMessages(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION);
                    removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL);


                    mIsChecking = false;
//                    if (isPrinting) {
//
//                        mIsConnected = false;
//
//                        isPrinting = false;
//                        Log.d(TAG, "isPrinting FALSE");
//
//                        mIsLastPrintJobSuccessful = false;
//                        // Destroy LG print thread
//                        if ( null != svc().mLGFileTransfer){
//                            Log.i(TAG, "Stopping LG print image thread");
//                            svc().mLGFileTransfer.cancelBT_Connecting();
//                            svc().mLGFileTransfer.stopTransfer();
//                            svc().mLGFileTransfer = null;
//                        }
//
//
//                        sendPrintJobStatus();
//                    } else {
                    if(!isPrinting){
                        if ( mIsConnected || mFirstTime ){
                            mFirstTime = false;
                            mIsConnected = false;
                            svc().showNotification("Printer Unavailable", "Issue connecting to printer");
                        }
                        sendPrinterStatusMessage();
                    }
                    this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION), CHECK_DELAY_MS);

                    break;

                case Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION:


                    Log.i(TAG, "BT RETRY FOR CONNECTION " +
                                    " this " + this
                            + " checking? " + mIsChecking
                             + " Connected " + mIsConnected
                            + " clients " + svc().getClients().size()
                            + " mCheckLG " +
                            (svc().mCheckLG != null ?
                                    svc().mCheckLG.hashCode() + " " + svc().mCheckLG
                                    : "null" )
                    );
                    // Check SUCCESS
                    // This fires on CHECK flow when we disconnect from
                    // the thread. Do not alter the mIsConnected here,
                    // only in FAIL

                    // If we succeed with a CHECK and we are already trying to print
                    if ( isPrinting ){
                        // mIsChecking = false;
                        Log.v(TAG, "isPrinting TRUE However, we are in RETRY for connect.");

                    }
                    // If one CHECK running already, dont getPaired again
                    else if ( svc().mCheckLG != null && mIsChecking == false){
//                        try {
//                            Thread.sleep(30000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        //svc().mCheckLG.stopTransfer();
                        //svc().mCheckLG.cancelBT_Connecting();
                        // this ALso starts check tranfer
                        svc().mCheckLG.getPairedDevices();

                        mIsChecking = true;
                        // If CHECK object is null, we need to use it to kick off the process
                    } else if ( svc().mCheckLG == null ) {

                        svc().mCheckLG = new CheckLGConnection((Context) svc(), this);
                        // This ALSO starts the transfer
                        svc().mCheckLG.getPairedDevices();

                        mIsChecking = true;
                    }
                    break;


                case Opptransfer.BLUETOOTH_SEND_FAIL:

                    this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                    // Check INTERUPTED?
                    // Print Job FAILURE
                    // This is for BOTH flows when there is RFCOMM ERROR
                    // or when a Share error during Printing occurs

//                    sendFailState((int)msg.arg1);
                    //mLGFileTransfer = null;
//                    if (mProgress!=null) mProgress.setProgress(0);

                    // We might be here forprint timeout

                    // if we try to print and we get this,
                    // we will be marked false.
                    mIsChecking = false;
                    if (isPrinting){
                        // isPrinting set by first client who tries
                        // to print.
                        isPrinting = false;
                        Log.d(TAG, "isPrinting FALSE");

                        // Destroy LG print thread
                        if ( null != svc().mLGFileTransfer){
                            Log.i(TAG, "Stopping LG print image thread");
                            svc().mLGFileTransfer.cancelBT_Connecting();
                            svc().mLGFileTransfer.stopTransfer();
                            svc().mLGFileTransfer = null;
                        }

                        mIsLastPrintJobSuccessful = false;
                        sendPrintJobStatus();
                    } else {
                        // if already unconnected dont show noti again
                        if ( mIsConnected || mFirstTime ) {
                            mFirstTime = false;
                            mIsConnected = false;

                            svc().showNotification("Printer Unavailable", "Issue connecting to printer");
                        }
                        // we want to broadcast always
                        sendPrinterStatusMessage();

                    }
                    // this doesnt mean we left the area, so dont set disconnected
                    this.removeMessages(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION);
                    this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION), CHECK_DELAY_MS);

                    break;

                // Sending image data via Bluetooth
                case Opptransfer.BLUETOOTH_SEND_PACKET:

                    // We remove any send fails, which may be waiting
                    this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                    // mIsConnected = true;
                    int per = (int) ((msg.arg1 / (float) msg.arg2) * 100);
                    Log.i(TAG, "Print Job %" + String.valueOf(per));
                    break;

                // Complete to send image data
                case Opptransfer.BLUETOOTH_SEND_COMPLETE:

                    this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                    // Print Job SUCCESS COMPLETE
                    // WHen we are done successfully printing, alert
                    // the clients
                    // If they dont care, they dont handle it
                    // Or at the very least, they should set a client side
                    // check to ignore or handle the message i.e. isWaitingPrintJob

                    // mLGFileTransfer = null;
                    // Toast.makeText(MessengerService.this, "Send Complete", Toast.LENGTH_LONG).show();

                    mIsLastPrintJobSuccessful = true;
                    isPrinting = false;
                    Log.d(TAG, "isPrinting FALSE");
                    sendPrintJobStatus();

                    // Destroy LG print thread
                    if ( null != svc().mLGFileTransfer){
                        Log.i(TAG, "Stopping LG print image thread");
                        svc().mLGFileTransfer.cancelBT_Connecting();
                        svc().mLGFileTransfer.stopTransfer();
                        svc().mLGFileTransfer = null;
                    }

                    this.removeMessages(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION);
                    this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION), CHECK_DELAY_MS);

                    break;
                // END LG

                default:
                    super.handleMessage(msg);
            }
        }

    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final IncomingHandler mHandler;// = new IncomingHandler(this);
    final Messenger mMessenger;// = new Messenger();
    {
        mHandler = new IncomingHandler(this);
        mMessenger = new Messenger(mHandler);
        // Kick off processing
        //if (mClients.size() == 0 || mCheckLG == null && mLGFileTransfer == null) {
            Log.i(TAG, "START Initializing check thread");
            mHandler.obtainMessage(Opptransfer.BLUETOOTH_RETRY_FOR_CONNECTION).sendToTarget();
        //}
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();

        // TODO: Start looking for printer right away
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
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "onUnBind called: Cancelling LG print threads.");
        if ( null != mCheckLG) {
            mCheckLG.stopTransfer();
            mCheckLG.cancelBT_Connecting();
        }

        if ( null != mLGFileTransfer ) {
            mLGFileTransfer.stopTransfer();
            mLGFileTransfer.cancelBT_Connecting();
        }

        return false;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Printer Service Loading"; //getText(R.string.remote_service_started);

        mNM.cancel(NOTIFICATION_ID);
        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)  // the status icon
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
    private void showNotification(String title, String aText) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = (CharSequence) aText; //getText(R.string.remote_service_started);

        mNM.cancel(NOTIFICATION_ID);
        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)  // the status icon
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

}
//END_INCLUDE(service)