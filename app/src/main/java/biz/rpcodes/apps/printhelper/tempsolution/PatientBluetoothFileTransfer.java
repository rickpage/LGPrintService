package biz.rpcodes.apps.printhelper.tempsolution;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.androiod.bluetooth.opp.BluetoothOppBatch;
import com.androiod.bluetooth.opp.BluetoothOppShareInfo;
import com.androiod.bluetooth.opp.Constants;
import com.lge.pocketphoto.bluetooth.Opptransfer;
import com.lge.pocketphoto.bluetooth.PrintPairedSearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Like BTFT but opens a connection without having the URI
 *
 */
public class PatientBluetoothFileTransfer {
	private BluetoothAdapter mBtAdapter;
	private String mMac = null;
	private Context mContext;
	private ArrayList<BluetoothOppBatch> mBatchs;
	private int mBatchId = 1;

	private Handler mHandler;

	private OpptransferExistingSocket mtrans = null;

	private boolean bCanceled = false;

	private boolean _isSettingActivity = false;
	private BluetoothDevice mDevice;
	private volatile BluetoothSocket mSocket;

	public void stopTransfer()
	{
		if(mtrans != null) {
			mtrans.stopSession();
			mtrans.cancelTransfer();
		}
	}

	/**
	 * use this with a scheme Uri
	 * @param uri
	 */
	public void startPrintingURI(Uri uri) {
		String filepath = new String(uri.toString());
		startPrintingFilename(filepath);
	}

	/**
	 * Use this when you dont have file:// or scheme
	 * @param filename
	 */
	public void startPrintingFilename(String filename) {

		Uri imgUri = Uri.parse("file://" + filename);
		String converted =imgUri.toString();

		long ts = System.currentTimeMillis();

		BluetoothOppShareInfo info = new BluetoothOppShareInfo(
				0, //id
				converted, // uri
				null, //hint
				null, //_data ALSO the filename
				"image/*", //mime
				0, //direction
				mMac, //destination
				0, //visibility
				2, //user confirmation
				190, //status
				0, //total bytes
				0, //current bytes
				(int)ts, //time stamp
				false //media scanned
		);


		BluetoothOppBatch newBatch = new BluetoothOppBatch(mContext, info);
		newBatch.mId = mBatchId;
		mBatchId++;
		mBatchs.add(newBatch);

		// PROBLEM: Doing this will get us to re-connect
		// SOL'N: make opp transfer version with socket already known
		try {
			mtrans = new OpptransferExistingSocket(mContext
                    , null
                    , newBatch
                    , mHandler
                    , mSocket);
			mtrans.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private SocketConnectThread mConnectThread=null;

	public void initializeSocket(){
		mConnectThread = new SocketConnectThread(mDevice, -1);
		mConnectThread.start();
	}

	private void init(Context ctx, Handler handler)
	{

		mBatchs = new ArrayList<BluetoothOppBatch>();
		mContext = ctx;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;

		// DOES NOT START TRANSFER
		// But it does start the process to get socket
		getSocketFromPairedDevices();
	}

	public PatientBluetoothFileTransfer(Context ctx, Handler handler){
		init(ctx, handler);
	}



	private void getSocketFromPairedDevices()
	{
		boolean bFounded = false;
		int devCount = 0;
		ArrayList<String> devAddr = new ArrayList<String>() ;
		ArrayList<String> devName = new ArrayList<String>() ; 

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		if (pairedDevices.size() > 0)
		{	           
			for (BluetoothDevice device : pairedDevices) {
				if(device.getName() != null)
				{
					if(device.getName().contains("Printer") ||
							device.getName().contains("Pocket Photo") ||
							device.getName().contains("PocketPhoto") ||
							device.getName().contains("PD239") ||
							device.getName().contains("PD240") ||
							device.getName().contains("PD241")
							)
					{
						bFounded = true;
						if(devAddr.contains(device.getAddress()) == false)
						{
							devAddr.add(device.getAddress());
							devName.add(device.getName()); 
							devCount++;
							// save device, we use this to connect
							mDevice = device;
							break;
						}
					}
				}
			}

//			if(devCount == 0)
//			{
//				startDiscovery();
//			}else
			if ( devCount > 0){
				Log.d("BluetoothFileTx", "Start Transfer...");
				mMac = devAddr.get(0);
				initializeSocket();
				// Toast.makeText(mContext, "Transfer "+ mMac, Toast.LENGTH_LONG).show();
			}

		else
			{
				mHandler.obtainMessage(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL, 11111, 11111).sendToTarget();
			}
		} else {

			mHandler.obtainMessage(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL, 22222, 22222 ).sendToTarget();
		}
	}



	public void cancelBT_Connecting()
	{
		// TODO: Join if interrupt isnt enough
		try {
			mConnectThread.join();
			// Closes the BT socket
			mConnectThread.interrupt();
			mConnectThread = null;
		} catch (InterruptedException e) {
			Log.i("PatientTransfer", "Interrupted connection");
		//	e.printStackTrace();
		}

		// This doesnt seem to do it
//		if(mtrans != null)
//			mtrans.stopConnect();


	
	}
	
	public void cancelBT_Search()
	{
		if (mBtAdapter.isDiscovering())	mBtAdapter.cancelDiscovery();
		
		bCanceled = true;
	}


	private class SocketConnectThread extends Thread {
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
					Log.i("INTERRUPT","INTERRUPT");
					try {
						Thread.sleep(500);
						btSocket.close();
						Thread.sleep(500);
					} catch (Exception e) {

					}
				}
			}
			// RP
			super.interrupt();
		}

		int check_connect=0; // 0: Connecting, 1: Connect Success, -1: Connection Fail, -2: Timerover
//		private class ConnectTask extends AsyncTask<Void, Void, Void> {
//
//			@Override
//			protected Void doInBackground(Void... params) {
//				try {
//					Thread.sleep(20000);
//					// Thread.sleep(10000);
//					if (check_connect!=0) return null;
//					check_connect=-2;
////					BluetoothOppPreference.getInstance(mContext)
////					.removeChannel(device, OPUSH_UUID16);
//					// This is just BAD, why do they turn off adapter? RP
//					/// if(mAdapter != null)mAdapter.disable();
//
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				return null;
//			}
//		}
		@Override
		public void run() {

			timestamp = System.currentTimeMillis();

			/* Use BluetoothSocket to connect */

			try {
				btSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001105-0000-1000-8000-00805f9b34fb"));

				if(btSocket == null)
				{
//					BluetoothOppPreference.getInstance(mContext)
//					.removeChannel(device, OPUSH_UUID16);
					markConnectionFailed(btSocket);
					return;
				}

				check_connect=0;
				// might need to wait for socket to be formed
//				Thread.sleep(500);
//				try{
//					new ConnectTask().execute();
//				}
//				catch (Throwable e) { e.printStackTrace(); } // handling 'time over' manually
				// Looks like THIS is where we first connect to BT.
				// From here we connect to client thread?
				btSocket.connect(); // throws IOException
				check_connect=1;
				Thread.sleep(100);
			}
			catch (Exception e) {
				// e.printStackTrace();
				try {
					Thread.sleep(1000); //1 second delay
					btSocket.connect();
					Thread.sleep(200);

				} catch (Exception e1) {
					// e1.printStackTrace();
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

			//
			// We have success by this point
			// TODO: Should we send the socket out then back in when we want to print?
			// TODO: If so, make this a separate class and require socket to print
			//
			mSocket = btSocket;
			// Send success to UI
			mHandler.obtainMessage(Opptransfer.BLUETOOTH_SOCKET_CONNECTED)
					.sendToTarget();

			//
//			try {
//				// Do we print rigth after this or not?
//				// If so, just delay making this
//				// We can tear this down every X seconds
//				// We also need to be able to build BTFT as usual
//				BluetoothOppRfcommTransport transport;
//				transport = new BluetoothOppRfcommTransport(
//						btSocket);
//
////				BluetoothOppPreference.getInstance(mContext)
////				.setChannel(device, OPUSH_UUID16, channel);
////				BluetoothOppPreference.getInstance(mContext)
////				.setName(device, device.getName());
//
//
//			} catch (Exception e) {
//
////				BluetoothOppPreference.getInstance(mContext)
////				.removeChannel(device, OPUSH_UUID16);
//				markConnectionFailed(btSocket);
//				return;
//			}

		}

		private void markConnectionFailed(BluetoothSocket s) {
			try {
				if(s != null)s.close();
			} catch (IOException e) {

			}
			mHandler.obtainMessage(OpptransferExistingSocket.BLUETOOTH_SOCKET_CONNECT_FAIL).sendToTarget();
			return;
		}
	};
}

