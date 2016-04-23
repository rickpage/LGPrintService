package biz.rpcodes.apps.lgprinter.service;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.lge.pocketphoto.bluetooth.BluetoothFileTransfer;
import com.lge.pocketphoto.bluetooth.ErrorCodes;
import com.lge.pocketphoto.bluetooth.Opptransfer;

import java.lang.ref.WeakReference;

import biz.rpcodes.apps.lgprinter.LGPrintHelper;
import biz.rpcodes.apps.lgprinter.PrintIntentConstants;

/**
 * Created by page on 4/22/16.
 */
public class IncomingHandler2 extends Handler {
    private static final int PRINT_TIMEOUT_TRANSFER_MS = 60000;
    private final WeakReference<MessengerService> mService;

    public static DebugStringManager debug;
    private static final String TAG = "LGPrintMsgHandler";

    private final int CHECK_CONNECTION_INTERVAL_MS = 3000;
    private boolean mIsPrinting;
    private boolean mIsConnected;
    private boolean mIsChecking;
    private boolean mIsLastPrintJobSuccessful;
    private int mErrorCode = PrintIntentConstants.NO_ERROR_CODE;

    public IncomingHandler2(MessengerService s){
        mIsConnected = false;
        mIsPrinting = false;
        Log.i(TAG, "New handler: " + this);
        mService = new WeakReference<MessengerService>(s);
        debug = new DebugStringManager();
        debug.addString("New handler created, " + this
                + ", mClient size " + svc().getClients().size());
    }

    private MessengerService svc(){
        if(null != mService)
            return mService.get();
        else return null;
    }

    private String craftDebugInfo(){
        String s = "";
        s += (mIsConnected ? "Connected, " : "DISCONNECTED, ");
        s += (mIsPrinting ? "Printing, " : "No Print, ");
        s += (mIsChecking ? "Checking, " : "Not Checking, ");

        // known bad states
        boolean notKilled = (mIsChecking && mIsPrinting);

        if (notKilled){
            s += "Checking and Printing at Same Time!!!";
        }
        return s;
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

                        //
                        // todo: actually do this with checker
                        //
                        mIsConnected = true;
                        sendPrinterStatusMessage();

                        svc().mIsInit = true;
                    }
                    break;
                case PrintIntentConstants.MSG_UNREGISTER_CLIENT:
                    if (msg.replyTo == null) {
                        throw new IllegalStateException(
                                "Message to LG Print Service MUST include reply to address");
                    }
                    svc().getClients().remove(msg.replyTo);

                    break;
                default:
                    // IGNORE 0-client INSTANCES!
                    if ( svc().getClients() == null || svc().getClients().size() < 1)
                    {
                        debug.addString("Ignoring 0-client-instance " + svc() + " " + this + " connected? "
                                + mIsConnected + " Checking? " + mIsChecking);
                        return;
                    }
            }

            // TODO: handle this differently
            //
            // These are split ON PURPOSE: Register and Print handling should be separate!
            //

            // set error code
            setFailState(msg);

            switch(msg.what){
                // User Print
                case PrintIntentConstants.MSG_REQUEST_PRINT_JOB:
                    // get the  filepath
                    // String mFileName = (String) msg.obj;
                    Bundle bund = msg.getData();
                    String mFileName = bund.getString("filepath");
                    if (mIsPrinting) { // Make sure we aren't already printing
                        debug.addString(" MSG_REQUEST_PRINT_JOB Revcd Print requet when already Printing Already PRINTING");
                        Log.e("PRINTREQUEST", "Already printing, wait until job complete.");
                    } else {
                        if (mFileName == null) {
                            debug.addString(" MSG_REQUEST_PRINT_JOB FILENAME IS NULL");
                            Log.e("PRINTREQUEST", "mFileName is null");
                            // break;
                        } else {
                            Log.i(TAG, "PRINT isPrinting TRUE, Printing " + mFileName);

                            mIsPrinting = true;

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
                    break;

                case Opptransfer.BLUETOOTH_CONNECTION_INTERRUPTED:
                    break;

                case Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL:
                    debug.addString("BLUETOOTH_CONNECT_FAIL");
                    Log.i(TAG, "BLUETOOTH_CONNECT_FAIL");
                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = false;
                    break;

                case Opptransfer.CHECK_BT_RETRY_FOR_CONNECT_STATUS:
                    break;

                case Opptransfer.BLUETOOTH_SEND_TIMEOUT:
                    debug.addString("BLUETOOTH_SEND_TIMEOUT");
                    Log.i(TAG, "BLUETOOTH SEND TIMEOUT");
                    break;

                case Opptransfer.BLUETOOTH_SEND_FAIL:
                    debug.addString("BLUETOOTH_SEND_FAIL");
                    Log.i(TAG, "BLUETOOTH SEND FAIL");
                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = false;
                    sendPrintJobStatus();
                    break;

                // Sending image data via Bluetooth
                case Opptransfer.BLUETOOTH_SEND_PACKET:
                    break;

                // Complete to send image data
                case Opptransfer.BLUETOOTH_SEND_COMPLETE:
                    debug.addString("BLUETOOTH_SEND_COMPLETE");
                    Log.i(TAG, "BLUETOOTH SEND COMPLETE");
                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = true;
                    sendPrintJobStatus();
                    break;

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
            // sendPrintJobStatus();
        }

        // end handle Message
        return;
    }


    //
    // STATUS
    //

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

    public void setFailState(Message m) {
        Log.i(TAG, "Message Type " + m.what + " With codes " + m.arg1 + " " + m.arg2);

        mErrorCode = m.arg1;
    }
}
