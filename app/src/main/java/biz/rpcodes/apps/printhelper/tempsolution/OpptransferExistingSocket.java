package biz.rpcodes.apps.printhelper.tempsolution;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;

import com.androiod.bluetooth.opp.BluetoothOppBatch;
import com.androiod.bluetooth.opp.BluetoothOppObexClientSession;
import com.androiod.bluetooth.opp.BluetoothOppObexSession;
import com.androiod.bluetooth.opp.BluetoothOppRfcommTransport;
import com.androiod.bluetooth.opp.BluetoothOppShareInfo;
import com.androiod.bluetooth.opp.BluetoothShare;
import com.androiod.bluetooth.opp.Constants;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

import javax.obex.ObexTransport;

public class OpptransferExistingSocket extends AsyncTask<Void, Integer, Void> implements BluetoothOppBatch.BluetoothOppBatchListener {

	private final BluetoothSocket mSocket;
	private boolean bCancelConnect = false;
	BluetoothOppBatch mBatch;
	EventHandler mSessionHandler;
	private HandlerThread mHandlerThread;
	BluetoothOppObexSession mSession = null;
	private Context mContext;
	BluetoothOppShareInfo mCurrentShare;
	private BluetoothAdapter mAdapter;

    private boolean cancelTransferToKeepPrinterOn;

	Handler mManagerHandler;

	private ObexTransport mTransport;

	protected boolean bPaired = false;

	// MESSAGE IDs 10 through 14, 50 through 63
	public static final int RFCOMM_ERROR = 10;

	public static final int RFCOMM_CONNECTED = 11;

	public static final int SDP_RESULT = 12;

	public static final int OBEX_SEND_PACKET = 13;

	public static final int OBEX_SEND_FIRST_PACKET = 14;

	private static final int CONNECT_WAIT_TIMEOUT = 45000;

	private static final int CONNECT_RETRY_TIME = 100;

	private static final short OPUSH_UUID16 = 0x1105;
	// When first connected
	public static final int BLUETOOTH_SOCKET_CONNECTED = 50;

	public static final int BLUETOOTH_SOCKET_CONNECT_FAIL = 51;

	public static final int BLUETOOTH_SEND_PACKET = 60;

	public static final int BLUETOOTH_SEND_FIRST_PACKET = 61;

	public static final int BLUETOOTH_SEND_COMPLETE = 62;

	public static final int BLUETOOTH_SEND_FAIL = 63;

	// RP
	// This means we were successful, but we would like to retry
	// Not used when using Opptransfer class
	public static final int CHECK_BT_RETRY_FOR_CONNECT_STATUS = 100;
	// RP
	public static final int BLUETOOTH_CONNECTION_INTERRUPTED = 116;
	// RP
	// When we print, set this
	public static final int BLUETOOTH_SEND_TIMEOUT = 120;

	public OpptransferExistingSocket(Context context,
									 PowerManager powerManager,
									 BluetoothOppBatch batch,
									 Handler handler,
									 BluetoothSocket socket) throws Exception{

		mBatch = batch;
		mContext = context;

		mSocket = socket;

        if (context == null) {
            throw new NullPointerException("Context may not be null");
        }
        mBatch.registerListern(this );

		mAdapter = BluetoothAdapter.getDefaultAdapter();	

		mManagerHandler = handler;	

        // preventRealTransfer();
	}

	/**
	 * Make transfer objects from socket
	 * @throws Exception
	 */
	private void makeConnectionObjects(){
		if ( mSocket == null) {
			// Don do this, because we should have a socket already
			//mSessionHandler.obtainMessage(RFCOMM_ERROR)
			//		.sendToTarget();
			return;
		}
		BluetoothOppRfcommTransport transport;
		transport = new BluetoothOppRfcommTransport(
				mSocket);

//				BluetoothOppPreference.getInstance(mContext)
//				.setChannel(device, OPUSH_UUID16, channel);
//				BluetoothOppPreference.getInstance(mContext)
//				.setName(device, device.getName());

		mSessionHandler.obtainMessage(RFCOMM_CONNECTED,
				transport).sendToTarget();
	}
//    public void startRealTransfer(){
//        cancelTransferToKeepPrinterOn = false;
//    }
//    public void preventRealTransfer(){
//        cancelTransferToKeepPrinterOn = true;
//    }

    public void start() throws Exception {

		if (mHandlerThread == null) {

			mHandlerThread = new HandlerThread("BtOpp Transfer Handler", 5);
			mHandlerThread.start();
			execute();
		}
	}

	private void startConnectSession() throws Exception {

		//mSessionHandler.obtainMessage(SDP_RESULT, -1, -1,
		//		mBatch.mDestination).sendToTarget();
	}	
	
	public void processCurrentShare() {
		/* This transfer need user confirm */		
		mSession.addShare(mCurrentShare);
	}

	@Override
	public void onShareAdded(int id) {


	}


	@Override
	public void onShareDeleted(int id) {


	}


	@Override
	public void onBatchCanceled() {


	}

	public void stopSession()
	{

		if(mSession != null)
			mSession.stop();
		if ( mHandlerThread != null) {

			if ( mSessionHandler != null)
				mSessionHandler.removeCallbacksAndMessages(null);
			mHandlerThread.quit();
			mHandlerThread = null;
		}
	}

	private class EventHandler extends Handler {

		public EventHandler(Looper looper) {
			super (looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case OBEX_SEND_FIRST_PACKET:

				// It seems that sometimes this comes too late, unless that was the debugger?
//                if (cancelTransferToKeepPrinterOn){
//                    try {
//                        mTransport.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    notifyToManager(mManagerHandler.obtainMessage(RFCOMM_ERROR));
//                    break;
//                }

                if(bCancelConnect == true)
				{
					bCancelConnect = false;
					notifyToManager(OBEX_SEND_FIRST_PACKET, 1, 0);	// 1: Cancel	
					break;
				}
			case OBEX_SEND_PACKET:

//                if (cancelTransferToKeepPrinterOn){
//                    try {
//                        mTransport.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    notifyToManager(mManagerHandler.obtainMessage(RFCOMM_ERROR));
//                    break;
//                }
                notifyToManager(msg);
				break;
//			case SDP_RESULT:
//				if (!((BluetoothDevice) msg.obj)
//						.equals(mBatch.mDestination)) {
//					return;
//				}
//				if(mConnectThread == null)
//				{
//					mConnectThread = new SocketConnectThread(
//							mBatch.mDestination, msg.arg1);
//					mConnectThread.start();
//				}
//				break;

				/*
				 * RFCOMM connect fail is for outbound share only! Mark batch
				 * failed, and all shares in batch failed
				 */
			case RFCOMM_ERROR:
//				mConnectThread = null;

				markBatchFailed(BluetoothShare.STATUS_CONNECTION_ERROR);
				mBatch.mStatus = Constants.BATCH_STATUS_FAILED;

				// We can notify here in case we lost something
				notifyToManager(msg);

				break;
				/*
				 * RFCOMM connected is for outbound share only! Create
				 * BluetoothOppObexClientSession and start it
				 */
			case RFCOMM_CONNECTED:				
				// mConnectThread = null;
				// different class for "transport" in all the files...
				mTransport = (ObexTransport) msg.obj;
				// RP: Up until HERE, we are connected, but havent sent data
				// This sets the file info and triggers boolean in run() for BTOppObexClientSession
				startObexSession();
//  NO! We already know
				// notifyToManager(msg);

				break;
				/*
				 * Put next share if available,or finish the transfer.
				 * For outbound session, call session.addShare() to send next file,
				 * or call session.stop().
				 * For inbounds session, do nothing. If there is next file to receive,it
				 * will be notified through onShareAdded()
				 */
			case BluetoothOppObexSession.MSG_SHARE_COMPLETE:
				BluetoothOppShareInfo info = (BluetoothOppShareInfo) msg.obj;

				if (mBatch.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
					mCurrentShare = mBatch.getPendingShare();
					mSession.stop();
				}

				notifyToManager(msg);
				break;
				/*
				 * Handle session completed status Set batch status to
				 * finished
				 */
			case BluetoothOppObexSession.MSG_SESSION_COMPLETE:				
				BluetoothOppShareInfo info1 = (BluetoothOppShareInfo) msg.obj;

				mBatch.mStatus = Constants.BATCH_STATUS_FINISHED;
				/*
				 * trigger content provider again to know batch status change
				 */			
				tickShareStatus(info1);				
				break;

				/* Handle the error state of an Obex session */
			case BluetoothOppObexSession.MSG_SESSION_ERROR:				
				BluetoothOppShareInfo info2 = (BluetoothOppShareInfo) msg.obj;
				mSession.stop();
				mBatch.mStatus = Constants.BATCH_STATUS_FAILED;
				mBatch.mErrStatus = info2.mErrStatus;
				markBatchFailed(info2.mStatus);
				tickShareStatus(mCurrentShare);
				notifyToManager(msg);
				break;

			case BluetoothOppObexSession.MSG_SHARE_INTERRUPTED:			
				BluetoothOppShareInfo info3 = (BluetoothOppShareInfo) msg.obj;
				if (mBatch.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
					try {
						if (mTransport == null) {

						} else {
							mTransport.close();
						}
					} catch (IOException e) {

					}

					mBatch.mStatus = Constants.BATCH_STATUS_FAILED;
					if (info3 != null) {
						markBatchFailed(info3.mStatus);
					} else {
						markBatchFailed();
					}
					tickShareStatus(mCurrentShare);
				}
				notifyToManager(msg);
				break;

			case BluetoothOppObexSession.MSG_CONNECT_TIMEOUT:

				/* for outbound transfer, the block point is BluetoothSocket.write()
				 * The only way to unblock is to tear down lower transport
				 * */
				if (mBatch.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {
					try {
						if (mTransport == null) {

						} else {
							mTransport.close();
						}
					} catch (IOException e) {

					}

				} else {
					/*
					 * For inbound transfer, the block point is waiting for
					 * user confirmation we can interrupt it nicely
					 */

					// Remove incoming file confirm notification
					NotificationManager nm = (NotificationManager) mContext
							.getSystemService(Context.NOTIFICATION_SERVICE);
					nm.cancel(mCurrentShare.mId);
					// Send intent to UI for timeout handling
					Intent in = new Intent(
							BluetoothShare.USER_CONFIRMATION_TIMEOUT_ACTION);
					mContext.sendBroadcast(in);

					markShareTimeout(mCurrentShare);
				}
				break;
			}
		}
	}
	private void startObexSession() {
		mBatch.mStatus = Constants.BATCH_STATUS_RUNNING;

		mCurrentShare = mBatch.getPendingShare();
		if (mCurrentShare == null) {
			return;
		}

		if (mBatch.mDirection == BluetoothShare.DIRECTION_OUTBOUND) {

			mSession = new BluetoothOppObexClientSession(mContext,
					mTransport);
		} else if (mBatch.mDirection == BluetoothShare.DIRECTION_INBOUND) {
			/*
			 * For inbounds transfer, a server session should already exists
			 * before BluetoothOppTransfer is initialized. We should pass in a
			 * mSession instance.
			 */
			if (mSession == null) {
				/** set current share as error */

				markBatchFailed();
				mBatch.mStatus = Constants.BATCH_STATUS_FAILED;
				return;
			}
		}
		// This starts the thread,
		// but the flag might be marked true, so we must wait
		mSession.start(mSessionHandler);
		// THIS here marks our flag false, so
		// calling this function makes it print ultimately
		// We should stay connected if we never call this because
		// the loop waits until interupted or the flfag is on
		// TODO put this if needed:
		processCurrentShare();
	}

	public void allowTransfer(){
		processCurrentShare();
	}
    /**
     * Tear down transport
     * Do this when we start transferring and we
     * just want to keep the printer turned ON
     */
    public void cancelTransfer(){
        if (mTransport != null){
            try{
                mTransport.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }



	private void markBatchFailed(int failReason) {
		synchronized (this ) {
			try {
				wait(1000);
			} catch (InterruptedException e) {

			}
		}


		if (mCurrentShare != null) {

			if (BluetoothShare.isStatusError(mCurrentShare.mStatus)) {
				failReason = mCurrentShare.mStatus;
			}
			if (mCurrentShare.mDirection == BluetoothShare.DIRECTION_INBOUND
					&& mCurrentShare.mFilename != null) {
				new File(mCurrentShare.mFilename).delete();
			}
		}

		BluetoothOppShareInfo info = mBatch.getPendingShare();
		while (info != null) {
			if (info.mStatus < 200) {
				info.mStatus = failReason;
			}
			info = mBatch.getPendingShare();
		}



	}

	private void markBatchFailed() {
		markBatchFailed(BluetoothShare.STATUS_UNKNOWN_ERROR);
	}
	
	private void notifyToManager(int what, int arg1, int arg2)
	{
		switch(what)
		{
		case OpptransferExistingSocket.OBEX_SEND_FIRST_PACKET:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_FIRST_PACKET, arg1, arg2).sendToTarget();
			break;
		case OpptransferExistingSocket.OBEX_SEND_PACKET:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_PACKET, arg1, arg2).sendToTarget();
			break;
		case BluetoothOppObexSession.MSG_SHARE_COMPLETE:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_COMPLETE).sendToTarget();
			break;
		case BluetoothOppObexSession.MSG_SESSION_ERROR:
		case BluetoothOppObexSession.MSG_SHARE_INTERRUPTED:		
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_FAIL, mBatch.mErrStatus, 0).sendToTarget();
			break;
		case RFCOMM_CONNECTED:
			//  NO! We already know!
			// mManagerHandler.obtainMessage(BLUETOOTH_SOCKET_CONNECTED).sendToTarget();
			break;
		case RFCOMM_ERROR:
			// We notify here because we needed a socket, so we will record error if we dont have it etc
			mManagerHandler.obtainMessage(BLUETOOTH_SOCKET_CONNECT_FAIL, arg1, arg2).sendToTarget();
			break; 
		}		
	}

	private void notifyToManager(Message msg)
	{
		notifyToManager(msg.what, msg.arg1, msg.arg2);
	}

	private void tickShareStatus(BluetoothOppShareInfo share) {

	}

	private void markShareTimeout(BluetoothOppShareInfo share) {

	}	

	public void stopConnect() {
		bCancelConnect = true;
	}

	@Override
	protected Void doInBackground(Void... params) {
		mSessionHandler = new EventHandler(mHandlerThread.getLooper());
		
		try {
			makeConnectionObjects();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
