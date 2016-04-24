package biz.rpcodes.apps.lgprinter.service;

import android.bluetooth.BluetoothAdapter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.lge.pocketphoto.bluetooth.ErrorCodes;
import com.lge.pocketphoto.bluetooth.Opptransfer;

import java.lang.ref.WeakReference;

import biz.rpcodes.apps.lgprinter.LGPrintHelper;
import biz.rpcodes.apps.lgprinter.PrintIntentConstants;
import biz.rpcodes.apps.printhelper.tempsolution.PatientBluetoothFileTransfer;

/**
 * Created by page on 4/22/16.
 */
public class IncomingHandler2 extends Handler {
    private static final int PRINT_TIMEOUT_TRANSFER_MS = 60000;
    private static final int RETRY_FOR_BT_SOCKET_INTERVAL_MS = 5000;

    private final WeakReference<MessengerService> mService;

    public static DebugStringManager debug;
    private static final String TAG = "LGPrintMsgHandler";

    private boolean mIsPrinting;
    private boolean mIsConnected;
    private boolean mIsLastPrintJobSuccessful;
    private int mErrorCode = PrintIntentConstants.NO_ERROR_CODE;
    private String mFileName;
    private boolean mFirstTime = true;

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
        String s = "STATE: ";
        s += (mIsConnected ? "Connected " : "DISCONNECTED ");
        s += (mIsPrinting ? ", Printing " : ", No Print ");

        s += ", File: " + mFileName;

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

                        if (!svc().mIsInit) {
                            // Kick off a retry to start getting the socket
                            this.obtainMessage(Opptransfer.RETRY_FOR_BT_SOCKET)
                                    .sendToTarget();
                        }
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
                                + mIsConnected );
                        return;
                    }
            }

            // TODO: handle this differently
            //
            // These are split ON PURPOSE: Register and Print handling should be separate!
            //

            // set error code
            setFailState(msg);

            // debug info
            debug.addString(craftDebugInfo());
            switch(msg.what){
                // User Print
                case PrintIntentConstants.MSG_REQUEST_PRINT_JOB:
                    // When we get here, we may be still
                    // waiting for the connection to complete
                    // or we may be printing already
                    // We can check printing,
                    // but if we have no connection,
                    // we should retry the print when we do.
                    // For now, we just print, using the same
                    // message

                    // get the  filepath
                    // String mFileName = (String) msg.obj;
                    Bundle bund = msg.getData();
                    mFileName = bund.getString("filepath");
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

                            // We want to remove any failed or succeeded or interupted
                            // but DO NOT interupt the checking/connection RETRY_ msg
                            this.removeMessages(Opptransfer.BLUETOOTH_SEND_FAIL);
                            this.removeMessages(Opptransfer.BLUETOOTH_SEND_COMPLETE);
                            this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);
                            this.removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL);



                            // send the file
                            Uri imgUri = Uri.parse("file://" + mFileName);
                            svc().mPatientLGFileTransfer.startPrintingURI(imgUri);

                            // set a timeout, in case the printer shuts off and we don't catch it
                            this.sendMessageDelayed(obtainMessage(Opptransfer.BLUETOOTH_SEND_TIMEOUT), PRINT_TIMEOUT_TRANSFER_MS);
                        }
                    }
                    break;
                //
                // Print processing
                //
                // LG Internal Messages
                case Opptransfer.BLUETOOTH_SOCKET_CONNECTED:
                    debug.addString("BLUETOOTH_SOCKET_CONNECTED");
                    Log.i(TAG, "BLUETOOTH_SOCKET_CONNECTED");
                    if (false == mIsConnected || mFirstTime) {
                        mFirstTime = false;
                        // SHow notification
                        svc().showNotification("Printer Available", "The printer is within range.");
                    }
                    mIsConnected = true;

                    sendPrinterStatusMessage();
                    break;


                case Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL:
                    debug.addString("BLUETOOTH_CONNECT_FAIL " + msg.arg1);
                    Log.i(TAG, "BLUETOOTH_CONNECT_FAIL " + msg.arg1);
                    // msg arg1 is 0 when no socket or bad state
                    // If we have never connected, doesn't matter
                    // If we HAVE connected, we could have a stale connection
//                    if ( msg.arg1 == 0 && mIsConnected) {
//                        // This means we have pairing, but the socket
//                        // died somehow; normally, we can reconnect from this,
//                        // but sometimes we cant
//                        Log.i(TAG, "ENABLE DISABLE");
//                        BluetoothAdapter.getDefaultAdapter().disable();
//                        BluetoothAdapter.getDefaultAdapter().enable();
//                    }
                    if (mIsConnected || mFirstTime) {
                        mFirstTime = false;
                        svc().showNotification("Printer Unavailable", "Issue connecting to printer");
                    }
                    mIsConnected = false;
                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = false;

                    sendPrinterStatusMessage();
                    break;

                case Opptransfer.RETRY_FOR_BT_SOCKET:
                    // If we have a socket open already,
                    // clear any pending RETY messages,
                    // then close the socket.
                    // If we dont, then we couldnt connect last time.
                    // Try again to open it.
                    // This will fire multiple times every minute,
                    // allowing us to move away/towards/etc

                    // remove Retry messages
                    // so we dont do this too many times
                    this.removeMessages(Opptransfer.RETRY_FOR_BT_SOCKET);
                    this.removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL);
                    this.removeMessages(Opptransfer.BLUETOOTH_SOCKET_CONNECTED);

                    // If we are currently Printing, we wait until
                    // next interval to try to grab the socket.
                    // if we are printing we are connected
                    if (!mIsPrinting){

                        // Destroy the connection if we have it
                        svc().destroyPatientLGThread();

                        Thread.sleep(150);
                        // Make a new connection
                        svc().mPatientLGFileTransfer =
                                new PatientBluetoothFileTransfer(this.svc()
                                        , this);
                    }

                    this.sendMessageDelayed(
                            this.obtainMessage(Opptransfer.RETRY_FOR_BT_SOCKET)
                    , RETRY_FOR_BT_SOCKET_INTERVAL_MS);
                    break;

                case Opptransfer.BLUETOOTH_SEND_TIMEOUT:
                    debug.addString("BLUETOOTH_SEND_TIMEOUT");
                    Log.i(TAG, "BLUETOOTH SEND TIMEOUT");
                    // We get here because printing took too long
                    // TODO: We can automatically retry?
                    // So mark failed
                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = false;
                    sendPrintJobStatus();
                    break;

                case Opptransfer.BLUETOOTH_SEND_FAIL:
                    debug.addString("BLUETOOTH_SEND_FAIL");
                    Log.i(TAG, "BLUETOOTH SEND FAIL");
                    this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);

                    mIsPrinting = false;
                    mIsLastPrintJobSuccessful = false;
                    sendPrintJobStatus();
                    break;

                // Complete to send image data
                case Opptransfer.BLUETOOTH_SEND_COMPLETE:
                    debug.addString("BLUETOOTH_SEND_COMPLETE");
                    Log.i(TAG, "BLUETOOTH SEND COMPLETE");
                    this.removeMessages(Opptransfer.BLUETOOTH_SEND_TIMEOUT);
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
