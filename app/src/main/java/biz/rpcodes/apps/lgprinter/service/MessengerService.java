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
import biz.rpcodes.apps.printhelper.tempsolution.CheckLGConnection;

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

    /** Does the printing **/
    public BluetoothFileTransfer mLGFileTransfer;

    // true when we finish init and expect 1+ clients from now on
    public boolean mIsInit = false;

    public MessengerService(){
        mHandler = new IncomingHandler2(this);
        mMessenger = new Messenger(mHandler);
        Log.i(TAG, "Constructor Messenger Service");
    }

    public ArrayList<Messenger> getClients(){
        if ( mClients.size() == 0 && mIsInit) {
            Log.i(TAG, ", 0 clients using this messenger service");
            this.stopSelf();
        }
        return mClients;
    }


    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        private static DebugStringManager debug;
        private static final String TAG = "LGPrintMsgHandler";
        private static final long CHECK_DELAY_MS = 3000;
        // Was 4 * CHECK DELAY MS, made huge to make sure its not this shit
        private static final long PRINT_TIMEOUT_TRANSFER_MS = 60000;
        private boolean mStarted;
        WeakReference<MessengerService> mService;

        /**
         indicates if printer can be reached recently, not
         wether the socket is actually connected and still valid
        note the issue is that even when connected, an out of range
         or off printer does not trigger messages or Exception
        */
        private boolean mIsConnected = true;

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
            debug = new DebugStringManager();
            debug.addString("New handler created, " + this
                    + ", mClient size " + svc().getClients().size());
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
                    Message m = this.obtainMessage(PrintIntentConstants.MSG_RESPONSE_STATUS
                            , mIsConnected ?
                            PrintIntentConstants.AVAILABLE
                            : PrintIntentConstants.UNAVAILABLE
                            , mErrorCode
                    );
                    Bundle b = new Bundle();
                    b.putString("error", getFailState());
                    m.setData(b);
                    // Increases counter, we only can send so many
                    LGPrintHelper.setDebugString(m, debug.getDebugMessage());

                    svc().getClients().get(i).send(m);
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mService.get().getClients().remove(i);
                }
            }
        }

        private void sendPrintJobStatus() {
            if ( null != svc() && svc().getClients() != null) {


                for (int i = svc().getClients().size() - 1; i >= 0; i--) {
                    try {
                        Message m = this.obtainMessage(PrintIntentConstants.MSG_RESPONSE_PRINT_JOB
                                , mIsLastPrintJobSuccessful ?
                                        PrintIntentConstants.SUCCESS
                                        : PrintIntentConstants.FAILURE
                                ,  mErrorCode

                        );
                        Bundle b = new Bundle();
                        b.putString("error", getFailState());
                        m.setData(b);
                        // Increases counter, we only can send so many
                        LGPrintHelper.setDebugString(m, debug.getDebugMessage());
                        svc().getClients().get(i).send(m);
                    } catch (RemoteException e) {
                        // The client is dead.  Remove it from the list;
                        // we are going through the list from back to front
                        // so this is safe to do inside the loop.
                        mService.get().getClients().remove(i);
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {

            /*
             * Registered clients get periodic messages about printer status
             * that they can use to update UI and Logic.
             * FIRST handle add/remove client
             * NEXT handle print
             * This is done ON PURPOSE so stray 0-client instances are IGNORED
             */
            try {
                switch (msg.what) {
                    case PrintIntentConstants.MSG_REGISTER_CLIENT:
                        if (msg.replyTo == null) {
                            throw new IllegalStateException("Message to LG Print Service MUST include reply to address");
                        }

                        svc().getClients().add(msg.replyTo);

                        if (svc().mClients != null && svc().mClients.size() > 0) {
                            // Display a notification about us starting.
                            svc().showNotification();
                            // Start up the checking for the printer, unless its already going
                            if ( !mIsChecking ) {
                                this.obtainMessage(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS).sendToTarget();
                            }
                            svc().mIsInit = true;
                        }
                        break;
                    case PrintIntentConstants.MSG_UNREGISTER_CLIENT:
                        if (msg.replyTo == null) {
                            throw new IllegalStateException("Message to LG Print Service MUST include reply to address");
                        }
                        svc().getClients().remove(msg.replyTo);

                        break;
                    default:
                        // IGNORE 0-client INSTANCES!
                        if ( svc().getClients() == null || svc().getClients().size() < 1)
                        {
                            debug.addString("Ignoring 0-client-instance " + svc() + " " + this + " connected? " + mIsConnected + " Checking? " + mIsChecking);
                            return;
                        }
                }

                //
                // These are split ON PURPOSE: Register and Print handling should be separate!
                //

                // TODO: handle this differently
                switch(msg.what){
                    // User Print
                    case PrintIntentConstants.MSG_REQUEST_PRINT_JOB:
                        // get the  filepath
                        // String mFileName = (String) msg.obj;
                        Bundle bund = msg.getData();
                        String mFileName = bund.getString("filepath");
                        if (isPrinting) { // Make sure we aren't already printing
                            debug.addString(" MSG_REQUEST_PRINT_JOB Revcd Print requet when already Printing Already PRINTING");
                            Log.e("PRINTREQUEST", "Already printing, wait until job complete.");
                        } else {
                            if (mFileName == null) {
                                debug.addString(" MSG_REQUEST_PRINT_JOB FILENAME IS NULL");
                                Log.e("PRINTREQUEST", "mFileName is null");
                                // break;
                            } else {
                                Log.i(TAG, "PRINT isPrinting TRUE, Printing " + mFileName);

                                isPrinting = true;

                                // Remove any stale messages
                                this.removeCallbacksAndMessages(null);

                                Uri imgUri = Uri.parse("file://" + mFileName);
                                svc().mLGFileTransfer = new BluetoothFileTransfer(this.svc()
                                        , null, imgUri, this);
                                // This ALSO starts the transfer
                                svc().mLGFileTransfer.getPairedDevices();

                                // set a timeout, in case the printer shuts off and wew dont catch it
                                /// (kicks off check lg again)
                                this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_SEND_TIMEOUT), PRINT_TIMEOUT_TRANSFER_MS);
                            }
                        }
                        break;
                    //
                    // Print processing
                    //
                    // LG Internal Messages
                    case Opptransfer.BLUETOOTH_SOCKET_CONNECTED:
                        // Print START SUCCESS
                        mIsChecking = false;
                        if (false == isPrinting) {
                            if (false == mIsConnected || mFirstTime) {
                                mFirstTime = false;
                                mIsConnected = true;
                                // SHow notification
                                svc().showNotification("Printer Available", "The printer is within range.");
                            }

                            // announce to clients
                            sendPrinterStatusMessage();
                        }
                        String debugString = "BLUETOOTH_SOCKET_CONNECTED ";
                        debugString += "isPrinting? " + isPrinting + " mIsConnected? "
                                + mIsConnected + " isChecking? " + mIsChecking;
                        debug.addString(debugString);
                        break;

                    case Opptransfer.BLUETOOTH_CONNECTION_INTERRUPTED:
                        mIsChecking = false;
                        debug.addString("CONNECTION INTERRUPTED: Message Status: " + msg.arg2 + " " + msg.arg2);
                        // CHECK FAILURE/INTERRUPT
                        // We were interupted, but dont change connection, because
                        // we generally interupt when we are ready to print
                        Log.d(TAG, "BT check connection interupted");
                        debug.addString("BT Check Connection Interrupted BLUETOOTH_CONNECTION_INTERRUPTED");
                        break;

                    case Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL:
                        setFailState(msg);

                        // PRINT FAILURE
                        removeMessages(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS);
                        removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL);


                        mIsChecking = false;

                        if (!isPrinting) {
                            if (mIsConnected || mFirstTime) {
                                mFirstTime = false;
                                mIsConnected = false;
                                svc().showNotification("Printer Unavailable", "Issue connecting to printer");
                            }
                            sendPrinterStatusMessage();
                        }

                        this.sendMessageDelayed(obtainMessage(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS), CHECK_DELAY_MS);

                        String debugStringCF = " BLUETOOTH_SOCKET_CONNECT_FAIL ";
                        debugStringCF += "isPrinting? " + isPrinting + " mIsConnected? "
                                + mIsConnected + " isChecking? " + mIsChecking;
                        debug.addString(debugStringCF);

                        break;

                    case Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS:

                        setFailState(msg);

                        String debugStringRC = "CHECK BT RETRY FOR CONNECTION " +
                                " this " + this
                                + " checking? " + mIsChecking
                                + " Connected " + mIsConnected
                                + " clients " + svc().getClients().size()
                                + " mCheckLG " +
                                (svc().mCheckLG != null ?
                                        svc().mCheckLG.hashCode() + " " + svc().mCheckLG
                                        : "null");
                        Log.i(TAG, debugStringRC);
                        debug.addString(debugStringRC);
                        break;

                    case Opptransfer.BLUETOOTH_SEND_TIMEOUT:
                        this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);
                        debug.addString("TIMEOUT FIRED");
                        // PASS THROUGH TO CANCEL PRINTING //
                    case Opptransfer.BLUETOOTH_SEND_FAIL:
                        setFailState(msg);
                        String debugStringSF = "*** BLUETOOTH_SEND_FAIL (Entry)";
                        debugStringSF += "isPrinting? " + isPrinting + " mIsConnected? "
                                + mIsConnected + " isChecking? " + mIsChecking
                        + " Message Status: " + getFailState() + " " + msg.arg2 + " " + msg.arg2;
                        debug.addString(debugStringSF);

                        this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                        // Check INTERUPTED?
                        // Print Job FAILURE
                        // This is for BOTH flows when there is RFCOMM ERROR
                        // or when a Share error during Printing occurs

                        // We might be here for print timeout

                        // if we try to print and we get this,
                        // we will be marked false.
                        mIsChecking = false;
                        if (isPrinting) {
                            // isPrinting set by first client who tries
                            // to print.
                            isPrinting = false;
                            Log.d(TAG, "isPrinting FALSE");

                            // Destroy LG print thread
                            svc().destroyPrintThread();

                            mIsLastPrintJobSuccessful = false;
                            sendPrintJobStatus();
                        } else {
                            // if already unconnected dont show noti again
                            if (mIsConnected || mFirstTime) {
                                mFirstTime = false;
                                mIsConnected = false;

                                svc().showNotification("Printer Unavailable", "Issue connecting to printer");
                            }
                            // we want to broadcast always
                            sendPrinterStatusMessage();

                        }
                        // this doesnt mean we left the area, so dont set disconnected
                        this.removeMessages(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS);
                        this.sendMessageDelayed(obtainMessage(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS), CHECK_DELAY_MS);

                        break;

                    // Sending image data via Bluetooth
                    case Opptransfer.BLUETOOTH_SEND_PACKET:

                        // We remove any send timeouts, which may be waiting
                        this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);
                        // We also dont risk checking again
                        this.removeMessages(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS);

                        int per = (int) ((msg.arg1 / (float) msg.arg2) * 100);
                        Log.i(TAG, "Print Job %" + String.valueOf(per));
                        debug.addString("Print Job Send Packet : " + String.valueOf(per) + "%");
                        break;

                    // Complete to send image data
                    case Opptransfer.BLUETOOTH_SEND_COMPLETE:

                        setFailState(msg);

                        this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);

                        this.removeMessages(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS);

                        // Print Job SUCCESS COMPLETE

                        mIsLastPrintJobSuccessful = true;
                        isPrinting = false;
                        Log.d(TAG, "isPrinting FALSE");
                        sendPrintJobStatus();

                        // Destroy LG print thread
                        svc().destroyPrintThread();

                        // this.removeMessages(Opptransfer.CHECK_BT_RETRY_FOR_CONNECTION);
                        this.sendMessageDelayed(obtainMessage(Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS), CHECK_DELAY_MS);

                        debug.addString("PRINT Successful ");
                        break;
                    // END LG

                    default:
                        super.handleMessage(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Handle Message Exception: " + e.getClass() + " "
                        + e.getMessage());
                String str = "EXCEPTION during handle message\n" + e.getMessage();
                for (StackTraceElement s : e.getStackTrace()){
                    str += s.getClassName() + " " + s.getFileName() + " "
                            + s.getMethodName() + " on line " + s.getLineNumber();
                }
                debug.addString(str);
                sendPrintJobStatus();
            }

            // end handle Message
            return;
        }




        public void setFailState(Message m){
            mErrorCode = m.arg1;
        }

        public String getFailState()
        {

            String errStr = null; //"SUCCESS";

            int error = mErrorCode;

            switch(error)
            {

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_BUSY:
                    errStr = "BUSY";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_JAM:
                    errStr = "DATA ERROR";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_EMPTY:
                    errStr = "PAPER EMPTY";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_WRONG_PAPER:
                    errStr = "PAPER MISMATCH";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_DATA_ERROR:
                    errStr = "DATA ERROR";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_COVER_OPEN:
                    errStr = "COVER OPEN";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_SYSTEM_ERROR:
                    errStr = "SYSTEM ERROR";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_BATTERY_LOW:
                    errStr = "BATTERY LOW";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_HIGH_TEMPERATURE:
                    errStr = "HIGH TEMPERATURE";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_LOW_TEMPERATURE:
                    errStr = "LOW TEMPERATURE";
                    break;

                case ErrorCodes.BLUETOOTH_RESPONSE_TARGET_COOLING_MODE:
                    errStr = "HIGH TEMPERATURE";
                    break;

            }

            if ( null != errStr)
                debug.addString("Error Code: " + errStr);

            return errStr;
        }


        // End Handler
    }

    public void destroyPrintThread(){
        // Destroy LG print thread
        if ( null != this.mLGFileTransfer){
            Log.i(TAG, "Stopping LG print image thread");
            this.mLGFileTransfer.cancelBT_Connecting();
            this.mLGFileTransfer.stopTransfer();
            this.mLGFileTransfer = null;
            //mHandler.debug.addString("Stopped PRINT thread " + mLGFileTransfer);
        } else {
            //mHandler.debug.addString("No PRINT thread to stop.");
        }
    }
    public void stopCheckLGThread(){
        // Destroy LG print thread

        if (null != mCheckLG) {
            mCheckLG.stopTransfer();
            mCheckLG.cancelBT_Connecting();

            //mHandler.debug.addString("Stopped LG check thread " + mCheckLG);
        } else {
            //mHandler.debug.addString("No Check connect thread to stop.");
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
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "onUnBind called: Cancelling LG print threads.");
        stopCheckLGThread();

        destroyPrintThread();

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
    public void showNotification(String title, String aText) {
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