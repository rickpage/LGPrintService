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

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;

/**
 * Like BTFT but opens a connection without having the URI
 *
 */
public class PatientBluetoothFileTransfer {
	private BluetoothAdapter mBtAdapter;
	private PrintPairedSearcher mPrintPairedSearcher;
	private static Uri mUri;
	private String mMac = null;
	private Context mContext;
	private ArrayList<BluetoothOppBatch> mBatchs;
	private int mBatchId = 1;

	private Handler mHandler;

	private Opptransfer mtrans = null;

	private boolean bCanceled = false;

	private boolean _isSettingActivity = false;

	public void stopTransfer()
	{
		if(mtrans != null) {
			mtrans.stopSession();
			mtrans.cancelTransfer();
		}
	}

	public void startPrintingURI(Uri uri){
		mUri = uri;
		String filepath = new String(mUri.toString());

		long ts = System.currentTimeMillis();

		BluetoothOppShareInfo info = new BluetoothOppShareInfo(
				0, //id
				filepath, // uri
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
			mtrans = new Opptransfer(mContext
                    , null
                    , newBatch
                    , mHandler
                    , false);
			mtrans.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startTransfer(boolean bPaired) {
		long ts = System.currentTimeMillis();

		BluetoothOppShareInfo info = new BluetoothOppShareInfo(
				0, //id
				null, // uri
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

			mtrans = new Opptransfer(mContext
					, null
					, newBatch
					, mHandler
					, bPaired);
			mtrans.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init(Context ctx, Handler handler)
	{

		mBatchs = new ArrayList<BluetoothOppBatch>();
		mContext = ctx;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;

	}

	public PatientBluetoothFileTransfer(Context ctx, Uri uri, Handler handler)
	{
		init(ctx, handler);
	}

	public PatientBluetoothFileTransfer(Context ctx, String mac, Uri uri, Handler handler)
	{
		mMac = mac;

		init(ctx, handler);
	}
	public PatientBluetoothFileTransfer(Context ctx, String mac, Uri uri, Handler handler, boolean isSettingActivity)
	{	
		mMac = mac;
		_isSettingActivity = isSettingActivity;
		mContext = ctx;
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mPrintPairedSearcher = new PrintPairedSearcher(mContext);
	}

	public void allowTransfer(){
		if (mtrans == null)
			throw new IllegalStateException("OppTransfer missing: cannot transfer");
		mtrans.processCurrentShare();
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
				Log.d("BluetoothFileTx", "Start Transfer...");
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

}

