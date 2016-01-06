package biz.rpcodes.apps.printhelper.tempsolution;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

import javax.obex.*;

import com.androiod.bluetooth.opp.*;

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
import android.util.Log;

public class OppTransferForConnectOnly extends AsyncTask<Void, Integer, Void> implements BluetoothOppBatch.BluetoothOppBatchListener {
	
	private boolean bCancelConnect = false;
	BluetoothOppBatch mBatch;
	public static BluetoothSocket btSocket; 
	EventHandler mSessionHandler;
	private HandlerThread mHandlerThread;
	BluetoothOppObexSession mSession = null;
	private Context mContext;
	BluetoothOppShareInfo mCurrentShare;
	private BluetoothAdapter mAdapter;
	private SocketConnectThread mConnectThread=null;

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

	public static final int BLUETOOTH_RETRY_FOR_CONNECTION = 100;
	// RP
	public static final int BLUETOOTH_CONNECTION_INTERRUPTED = 116;

	public OppTransferForConnectOnly(Context context,
									 PowerManager powerManager,
									 BluetoothOppBatch batch,
									 Handler handler,
									 boolean bPair) throws Exception{

		mBatch = batch;
		mContext = context;

        if (context == null) {
            throw new NullPointerException("Context may not be null");
        }
        mBatch.registerListern(this );

		mAdapter = BluetoothAdapter.getDefaultAdapter();	

		mManagerHandler = handler;	

		// NOTE: This argument is 100% useless fo rhtis code
		bPaired = bPair;

        // preventRealTransfer();
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

		mSessionHandler.obtainMessage(SDP_RESULT, -1, -1,
				mBatch.mDestination).sendToTarget();
	}	
	
	private void processCurrentShare() {
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
			case SDP_RESULT:              
				if (!((BluetoothDevice) msg.obj)
						.equals(mBatch.mDestination)) {
					return;
				}				
				if(mConnectThread == null)
				{
					mConnectThread = new SocketConnectThread(
							mBatch.mDestination, msg.arg1);
					mConnectThread.start();
				}
				break;

				/*
				 * RFCOMM connect fail is for outbound share only! Mark batch
				 * failed, and all shares in batch failed
				 */
			case RFCOMM_ERROR:
				mConnectThread = null;
				markBatchFailed(BluetoothShare.STATUS_CONNECTION_ERROR);
				mBatch.mStatus = Constants.BATCH_STATUS_FAILED;

				notifyToManager(msg);

				break;
				/*
				 * RFCOMM connected is for outbound share only! Create
				 * BluetoothOppObexClientSession and start it
				 */
			case RFCOMM_CONNECTED:				
				mConnectThread = null;
				// different class for "transport" in all the files...
				mTransport = (ObexTransport) msg.obj;
				
				// This sets the file info and triggers boolean in run() for BTOppObexClientSession
				startObexSession();

				notifyToManager(msg);

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

			// RP TODO THis doesnt transfer anything!
			mSession = new ConnectFirstBTOBEXClientSession(mContext,
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
		// BUT if using ConnextFirstBTOBEX then we do not doSendFile
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

	private class SocketConnectThread extends Thread {
		private static final String TAG = "SocketConnect";
		private final String host;

		private final BluetoothDevice device;

		private final int channel;

		private boolean isConnected;

		private long timestamp;

		private BluetoothSocket btSocket = null;

		/* create a TCP socket */
		public SocketConnectThread(String host, int port, int dummy) {
			super ("Socket Connect Thread");
			this .host = host;
			this .channel = port;
			this .device = null;
			isConnected = false;
		}

		/* create a Rfcomm Socket */
		public SocketConnectThread(BluetoothDevice device, int channel) {
			super ("Socket Connect Thread");
			this .device = device;
			this .host = null;
			this .channel = channel;
			isConnected = false;
		}

		public void interrupt() {
			if (!Constants.USE_TCP_DEBUG) {
				if (btSocket != null) {
					try {
						Thread.sleep(500);
						btSocket.close();
						Thread.sleep(500);
					} catch (Exception e) {

					}
				}
			}
		}

		int check_connect=0; // 0: Connecting, 1: Connect Success, -1: Connection Fail, -2: Timerover
	  	private class ConnectTask extends AsyncTask<Void, Void, Void> {   	

			@Override
			protected Void doInBackground(Void... params) {
				try {
					//Thread.sleep(20000);
					 Thread.sleep(5000);
					if (check_connect!=0) return null;
					check_connect=-2;
//					BluetoothOppPreference.getInstance(mContext)
//					.removeChannel(device, OPUSH_UUID16);
					// This is just BAD, why do they turn off adapter? RP
					/// if(mAdapter != null)mAdapter.disable();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}    	
	    }
	 		@Override
		public void run() {

			timestamp = System.currentTimeMillis();

			/* Use BluetoothSocket to connect */

			try {			
				btSocket = mBatch.mDestination.createRfcommSocketToServiceRecord(UUID.fromString("00001105-0000-1000-8000-00805f9b34fb"));
				
				if(btSocket == null)
				{
//					BluetoothOppPreference.getInstance(mContext)
//					.removeChannel(device, OPUSH_UUID16);
					markConnectionFailed(btSocket);
					return;	
				}

				check_connect=0;
				
				Thread.sleep(500);
				try{ new ConnectTask().execute(); } catch (Throwable e) { e.printStackTrace(); } // handling 'time over' manually
				// Looks like THIS is where we first connect to BT.
				// From here we connect to client thread?
				btSocket.connect();
				check_connect=1;
				Thread.sleep(200);
			} 
			catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(1000); //1 second delay
					btSocket.connect();
					Thread.sleep(200);
					
				} catch (Exception e1) {
					e1.printStackTrace();
					check_connect=-1;
//					BluetoothOppPreference.getInstance(mContext)
//					.removeChannel(device, OPUSH_UUID16);
					markConnectionFailed(btSocket);
					return;
				}
			}			

			if (check_connect==0)
			{
				check_connect=-1;
//				BluetoothOppPreference.getInstance(mContext)
//				.removeChannel(device, OPUSH_UUID16);
				markConnectionFailed(btSocket);
				return;	
			}
			
			try {
				BluetoothOppRfcommTransport transport;
				transport = new BluetoothOppRfcommTransport(
						btSocket);

//				BluetoothOppPreference.getInstance(mContext)
//				.setChannel(device, OPUSH_UUID16, channel);
//				BluetoothOppPreference.getInstance(mContext)
//				.setName(device, device.getName());
				mSessionHandler.obtainMessage(RFCOMM_CONNECTED,
						transport).sendToTarget();

			} catch (Exception e) {

//				BluetoothOppPreference.getInstance(mContext)
//				.removeChannel(device, OPUSH_UUID16);
				markConnectionFailed(btSocket);
				return;
			}

		}

		private void markConnectionFailed(Socket s) {
			try {
				s.close();
			} catch (IOException e) {

			}
			mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
		}

		private void markConnectionFailed(BluetoothSocket s) {
			try {
				if(s != null)s.close();
			} catch (IOException e) {

			}

			try {
				mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
			}
			catch (IllegalStateException e){
				Log.e(TAG, e.getMessage());
			}
			return;
		}
	};


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
		case OppTransferForConnectOnly.OBEX_SEND_FIRST_PACKET:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_FIRST_PACKET, arg1, arg2).sendToTarget();
			break;
		case com.lge.pocketphoto.bluetooth.Opptransfer.OBEX_SEND_PACKET:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_PACKET, arg1, arg2).sendToTarget();
			break;
		case BluetoothOppObexSession.MSG_SHARE_COMPLETE:
			mManagerHandler.obtainMessage(BLUETOOTH_SEND_COMPLETE).sendToTarget();
			break;
		case BluetoothOppObexSession.MSG_SESSION_ERROR: // RP
			mManagerHandler.obtainMessage(BLUETOOTH_RETRY_FOR_CONNECTION, mBatch.mErrStatus, 0).sendToTarget();
			break;
		case BluetoothOppObexSession.MSG_SHARE_INTERRUPTED:		
			mManagerHandler.obtainMessage(BLUETOOTH_CONNECTION_INTERRUPTED, mBatch.mErrStatus, 0).sendToTarget();
			break;
		case RFCOMM_CONNECTED:
			mManagerHandler.obtainMessage(BLUETOOTH_SOCKET_CONNECTED).sendToTarget();
			break;
		case RFCOMM_ERROR:
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

		if ( mHandlerThread != null) {

			if ( mSessionHandler != null)
				mSessionHandler.removeCallbacksAndMessages(null);
			mHandlerThread.quit();
			mHandlerThread = null;
		}
	}

	@Override
	protected Void doInBackground(Void... params) {

		try {
			if(mHandlerThread != null)
				mSessionHandler = new EventHandler(mHandlerThread.getLooper());
			startConnectSession();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
