package com.lge.pocketphoto.bluetooth;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.lge.pocketphoto.bluetooth.*;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PrintPairedSearcher extends AlertWorker {
	
	private Context mCtx;
	private Dialog mPairedDialog;
	private ListView mPaired_list;
	private ProgressBar mScan_progress;
	private TextView mNullListTxt;
	
	private List<PairedDevice> mPaired_item=new ArrayList<PairedDevice>();
	private Paired_ListAdapter mAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private int mSelect_item = -1;
	private boolean mFirstSearch = true;
	private boolean mSearchEnd = true;
	private boolean mSearchCanceled = false;
	private OnAlertListener mListerner;
	
	// BroadcastReceiver for searching device
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			Log.e("PrintPairedSearcher", "mReceiver action: "+action);
			
			// Whenever receiver find device, following message is received
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				Log.e("PrintPairedSearcher", "mReceiver ACTION_FOUND: "+device.getName());
				Log.e("PrintPairedSearcher", "mReceiver mFirstSearch: "+mFirstSearch);
				
				if(device.getName() != null)
				{
					if(mFirstSearch == false && (device.getName().contains("Printer") ||
							device.getName().contains("Pocket Photo") || device.getName().contains("PocketPhoto") || device.getName().contains("PD239")) )
					{
						PairedDevice d = new PairedDevice();
						d.name = device.getName();
						d.address = device.getAddress();
						
						// Double Check
						HashSet hs = new HashSet(mPaired_item);
						Iterator it = hs.iterator();
						while (it.hasNext()) {
							PairedDevice type = (PairedDevice) it.next();
							
							if(type.address.equals(d.address)) {
								return;
							}
						}
						
						mPaired_item.add(d);
						mAdapter.notifyDataSetChanged();
					}
				}
			}
			
			// It's done to search devices
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				if(mBluetoothAdapter.isDiscovering())
					mBluetoothAdapter.cancelDiscovery();
				if(mScan_progress != null)
					mScan_progress.setVisibility(View.INVISIBLE);
				mSearchEnd = true;
				
				if(mPaired_item == null || mPaired_item.size() < 1){
					mNullListTxt.setVisibility(View.VISIBLE);
				}else {
					mNullListTxt.setVisibility(View.INVISIBLE);
				}
			}
		}	
	};	
	
	public PrintPairedSearcher(Context context){
		mCtx = context;
	}
	
	public void PrintPairedSearchCancle(){
		if(mBluetoothAdapter.isDiscovering())
			mBluetoothAdapter.cancelDiscovery();
		
		mSelect_item = -1;
		if(null != mPairedDialog){
			mPairedDialog.dismiss();
			mPairedDialog = null;
		}
		mScan_progress = null;
		
		mCtx.unregisterReceiver(mReceiver);
		mListerner = null;
	}
	
	private void searchTask(){
		TimerTask myTask = new TimerTask() {
			
			TimerTask myTask2 = new TimerTask() {		
				@Override
				public void run() {
					
					if(mSearchEnd == false)
					{
						//	Second searching is finished
						mSearchEnd = true;
						if(mBluetoothAdapter.isDiscovering())
							mBluetoothAdapter.cancelDiscovery();
					}
				}
			};
			
			@Override
			public void run() {
				
				if(mSearchEnd == false)
				{
					// First searching is finished
					if(mBluetoothAdapter.isDiscovering())
						mBluetoothAdapter.cancelDiscovery();
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					mFirstSearch = false;
					// Second searching is started 
					mBluetoothAdapter.startDiscovery();
					Timer timer = new Timer();
					timer.schedule(myTask2, 10000);			
				}
			}
		};
		
		Timer timer = new Timer();
		timer.schedule(myTask, 5000);
	}

	/**
	 * BROKEN
	 * We are breakeing this function, as we require the devices to be paired already.
	 * We may change this but we dont ike the cpupling to R.xyz.etcetc...
	 *
	 * @param listerner
	 */
	public void search(final OnAlertListener listerner){

		throw new IllegalStateException("Cannot run DISCOVERY");
//		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//		mListerner = listerner;
//		mSelect_item = -1;
//		mFirstSearch = true;
//		mSearchEnd = false;
//		mSearchCanceled = false;
//		mPaired_item.clear();
//
//		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//		mCtx.registerReceiver(mReceiver, filter);
//
//    	filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//    	mCtx.registerReceiver(mReceiver, filter);
//
//		searchTask();
//
//		showAlert((Activity) mCtx, R.style.Theme_Dialog, R.layout.dialog_alert3,
//				new OnInitListener(listerner){
//
//					@Override
//					public void onInit(Dialog alert, View view) {
//						mPairedDialog = alert;
//
//						LayoutInflater factory = LayoutInflater.from(mCtx);
//				        View child_view = factory.inflate(R.layout.dialog_print_search_body, null);
//				        LinearLayout body_view = (LinearLayout)view.findViewById(R.id.idBody);
//						body_view.addView(child_view);
//
//						((TextView)view.findViewById(R.id.idCaption)).setText("Select PocketPhoto Mac Address.");
//						mPaired_list =(ListView)child_view.findViewById(R.id.idList);
//						mScan_progress = (ProgressBar) child_view.findViewById(R.id.scan_progress);
//						mNullListTxt = (TextView)child_view.findViewById(R.id.idNullListText);
//
//						mAdapter = new Paired_ListAdapter(mCtx,R.layout.dialog_print_search_body_row, mPaired_item);
//						mAdapter.setListView(mPaired_list);
//						mPaired_list.setAdapter(mAdapter);
//
//						Button retry = (Button) view.findViewById(R.id.idReSearch);
//						Button ok = (Button) child_view.findViewById(R.id.idOK);
//				    	Button cancel = (Button) child_view.findViewById(R.id.idCancel);
//
//				    	retry.setOnClickListener(new OnClickListener() {
//
//							@Override
//							public void onClick(View v) {
//								Log.e("PrintPairedSearcher", "retry: "+mSearchEnd);
//
//								if(mSearchEnd){
//									mSelect_item = -1;
//									mFirstSearch = true;
//									mSearchEnd = false;
//									mSearchCanceled = false;
//									mPaired_item.clear();
//									mAdapter.notifyDataSetChanged();
//
//									mNullListTxt.setVisibility(View.INVISIBLE);
//									mScan_progress.setVisibility(View.VISIBLE);
//
//									searchTask();
//								}
//
//							}
//						});
//
//
//				    	ok.setOnClickListener(new OnClickListener() {
//
//							@Override
//							public void onClick(View v) {
//								// If there is selection item
//								if(mSelect_item > -1){
//
//									if(mBluetoothAdapter.isDiscovering())
//										mBluetoothAdapter.cancelDiscovery();
//
//									PairedDevice device = mPaired_item.get(mSelect_item);
//
//									Bundle boundle=new Bundle();
//									boundle.putInt("workid", Activity.RESULT_OK);
//									boundle.putString("name", device.name);
//									boundle.putString("address", device.address);
//
//									mListerner = null;
//									mSelect_item = -1;
//									mPairedDialog.dismiss();
//									mPairedDialog = null;
//									mScan_progress = null;
//
//									mCtx.unregisterReceiver(mReceiver);
//
//									if (listerner!=null) listerner.onClick(boundle);
//								}
//							}
//						});
//
//				    	cancel.setOnClickListener(new OnClickListener() {
//
//							@Override
//							public void onClick(View v) {
//								// TODO Auto-generated method stub
//								if(mBluetoothAdapter.isDiscovering())
//									mBluetoothAdapter.cancelDiscovery();
//
//								mListerner = null;
//								mSelect_item = -1;
//								mPairedDialog.dismiss();
//								mPairedDialog = null;
//								mScan_progress = null;
//
//								mCtx.unregisterReceiver(mReceiver);
//
//								if (listerner!=null) listerner.onClick(null);
//							}
//						});
//
//					}
//
//					@Override
//					public void OnCancel() {
//						if(mBluetoothAdapter.isDiscovering())
//							mBluetoothAdapter.cancelDiscovery();
//
//						mPairedDialog.dismiss();
//						mPairedDialog = null;
//						mScan_progress = null;
//
//						mCtx.unregisterReceiver(mReceiver);
//
//						if (listerner!=null) listerner.onClick(null);
//					}
//
//		});
	}
	
	private class PairedDevice {
		public String name;
		public String address;
	}
	
	private class Paired_ListAdapter extends ArrayAdapter<PairedDevice> {
		private Context m_Context = null;
		private List<PairedDevice> m_item_list = null;
		private final int resId;
		private TextView itemName;
		private View mList;
		
		public Paired_ListAdapter(Context context, int textViewResourceId, List<PairedDevice> list) {
			super(context, textViewResourceId, list);
			m_Context = context;
			m_item_list = list;
			resId = textViewResourceId;
		}
		
		public void setListView(ListView list)
    	{
			mList = list;
    	}
//
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			Log.e("PrintPairedSearcher", "Paired_ListAdapter getView Call");
//
//			LayoutInflater inflator = (LayoutInflater) m_Context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//			convertView = inflator.inflate(resId, null);
//
//			convertView.setTag(position);
//
//			itemName = (TextView) convertView.findViewById(R.id.idText);
//			itemName.setText(m_item_list.get(position).name);
//
//			CheckBox cv = (CheckBox) convertView.findViewById(R.id.idCheck);
//
//			if(mSelect_item == position)
//				cv.setChecked(true);
//			else
//				cv.setChecked(false);
//
//			convertView.setOnClickListener(
//					new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							int pos = (Integer) v.getTag();
//
//							if (pos == mSelect_item) return;
//
//							((CheckBox)v.findViewById(R.id.idCheck)).setChecked(true);
//
//							View preView=((View)mList.findViewWithTag(mSelect_item));
//
//							if (preView!=null)
//								((CheckBox)preView.findViewById(R.id.idCheck)).setChecked(false);
//
//							mSelect_item = pos;
//						}
//					});
//
//			return convertView;
//		}
//
		@Override
		public void notifyDataSetChanged() {
			Log.e("PrintPairedSearcher", "Paired_ListAdapter notifyDataSetChanged");
			Log.e("PrintPairedSearcher", "Paired_ListAdapter m_item_list.size: "+m_item_list.size());
			super.notifyDataSetChanged();
		}
	}
}
