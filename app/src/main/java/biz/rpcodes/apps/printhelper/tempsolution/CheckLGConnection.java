package biz.rpcodes.apps.printhelper.tempsolution;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.androiod.bluetooth.opp.BluetoothOppBatch;
import com.androiod.bluetooth.opp.BluetoothOppShareInfo;
import com.lge.pocketphoto.bluetooth.AlertWorker;
import com.lge.pocketphoto.bluetooth.Opptransfer;
import com.lge.pocketphoto.bluetooth.PrintPairedSearcher;
import com.lge.pocketphoto.bluetooth.ProgressDimmedAlert;

import java.util.ArrayList;
import java.util.Set;

/**
 * Copied from BluetiithFileTransfer in lge.pockehpoto.bliuetooth
 *
 * This could be much cleaner, all it does it connect in the same
 * way Motorola wrote for us, and then we let it disconnect
 *
 * We expect messages about connect / disconnect to reach the handler
 * We can discard them and ignore them when we really want to print
 *
 */
public class CheckLGConnection {

	boolean bFirstSearch = true;

	private PowerManager mPowerManager;
	private BluetoothAdapter mBtAdapter;
	private PrintPairedSearcher mPrintPairedSearcher;
	private static Uri mUri;
	private String mFilepath;
	private String mMac = null;
	private Context mContext;
	private ArrayList<BluetoothOppBatch> mBatchs;
	private int mBatchId = 1;

	private Handler mHandler;

	private OppTransferForConnectOnly mtrans = null;

	private boolean bCanceled = false;

	private boolean _isSettingActivity = false;

	public void stopTransfer()
	{
		if(mtrans != null) {
			mtrans.stopSession();
			mtrans.cancelTransfer();
		}
	}

	public void startTransfer(boolean bPaired) {

//		if (mBtAdapter.isDiscovering())
//			mBtAdapter.cancelDiscovery();

		long ts = System.currentTimeMillis();

		BluetoothOppShareInfo info = new BluetoothOppShareInfo(
				0, //id
				mFilepath, // uri
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

		try {
			if ( mtrans != null ){
				mtrans.stopSession();
				mtrans.cancelTransfer();
				mtrans = null;
			}

			mtrans = new OppTransferForConnectOnly(mContext, mPowerManager, newBatch, mHandler, bPaired);

			mtrans.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init(Context ctx, Handler handler)
	{
		mFilepath = ""; //new String(mUri.toString());
		mBatchs = new ArrayList<BluetoothOppBatch>();
		mContext = ctx;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		mPrintPairedSearcher = new PrintPairedSearcher(mContext);

	}

	public CheckLGConnection(Context ctx, Handler handler)
	{
		init(ctx, handler);
	}

	public CheckLGConnection(Context ctx, String mac, Handler handler)
	{
		mMac = mac;

		init(ctx, handler);
	}
	public CheckLGConnection(Context ctx, String mac, Handler handler, boolean isSettingActivity)
	{	
		mMac = mac;
		_isSettingActivity = isSettingActivity;
		mContext = ctx;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mPrintPairedSearcher = new PrintPairedSearcher(mContext);
	}

	
	public void getPairedDevices()
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
				Log.d("CheckLGConnection", "Start Transfer...");
				mMac = devAddr.get(0);
				startTransfer(false);
				// Toast.makeText(mContext, "Transfer "+ mMac, Toast.LENGTH_LONG).show();
			}

			else
			{
				mHandler.obtainMessage(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL).sendToTarget();
			}
		} else {

			mHandler.obtainMessage(Opptransfer.BLUETOOTH_SOCKET_CONNECT_FAIL).sendToTarget();
		}
	}

	/** We do NOT want discovery, we require it to be paired first **/
	public void checkBluetooth()
	{
		int state = mBtAdapter.getState();
		// TODO: Only change this and the condition below when we implement our OWN PrintPairedSearcher
		boolean tryToDoSearch = false;
		//turn ON if BluetoothAdapter is OFF		
		if(state == BluetoothAdapter.STATE_OFF && tryToDoSearch)
		{
			

			(new ProgressDimmedAlert()).run((Activity)mContext, "",
					new ProgressDimmedAlert.OnTaskListener() {

				private boolean bEnd = false;			

				private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String action = intent.getAction();			

						if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

							if(mBtAdapter.getState() == BluetoothAdapter.STATE_ON)
							{
								bEnd = true;	
							}		        
						}	
					}	
				};	


				public void onPrev(Intent data) {

					IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
					mContext.registerReceiver(mStateReceiver, filter);

					mBtAdapter.enable();							
				}

				@Override
				public void onData(Intent data) {
					while(bEnd == false)
					{
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				@Override
				public void onCancelled(Intent data) {
					mContext.unregisterReceiver(mStateReceiver);	
					//((PrintActivity)mContext).finish();
				}

				public void onPost(Intent data) {
					mContext.unregisterReceiver(mStateReceiver);
						getPairedDevices();	
				}
			});		
		}
		else if(state == BluetoothAdapter.STATE_ON && mMac != null)
		{
			
		}
		else if(state == BluetoothAdapter.STATE_ON && mMac == null)			
		{
			getPairedDevices();	
		}
	}	

	public void startDiscovery() {
		mPrintPairedSearcher.search(new AlertWorker.OnAlertListener(){

			@Override
			public void onClick(Object tag) {
				if (mContext instanceof Activity)
				{
					
				}
				
				if(bCanceled == true) return;
				
			
				if (tag==null) {
					return;
				} 
				Bundle resultValue=(Bundle)tag;
				if (resultValue.getInt("workid")!=Activity.RESULT_OK) {
					return;
				}
				
				mMac = resultValue.getString("address");
				startTransfer(false);
				Toast.makeText(mContext, "Transfer "+ mMac, Toast.LENGTH_LONG).show(); 
				
			}
			
		});
	}

	public void cancelBT_Connecting()
	{		
		if(mtrans != null)
			mtrans.stopConnect();
	
	}
	
	public void cancelBT_Search()
	{
		if (mBtAdapter.isDiscovering())	mBtAdapter.cancelDiscovery();
		
		bCanceled = true;
	}
	
	public void setMacAddr(String mac)
	{
		mMac = mac;
	}
	public String getMacAddr()
	{
		return mMac;
	}
}

