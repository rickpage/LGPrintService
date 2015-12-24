package com.lge.pocketphoto.bluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * The Class ProgressAsyncTask
 */
public class ProgressDimmedAlert extends AsyncTask<Void, Integer, Void>{

	/**
	 * The m on task listener.
	 * @uml.property  name="mOnTaskListener"
	 * @uml.associationEnd  
	 */
	protected OnTaskListener mOnTaskListener=null;

	/**
	 * The listener interface for receiving onTask events.
	 * The class that is interested in processing a onTask
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnTaskListener<code> method. When
	 * the onTask event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see
	 */
	public abstract static class OnTaskListener {

		/**
		 * On data.
		 *
		 * @param data the data
		 */
		public abstract void onData(Intent data);

		/**
		 * On post.
		 *
		 * @param data the data
		 */
		public void onPost(Intent data) {}

		/**
		 * On prev.
		 *
		 * @param data the data
		 */
		public void onPrev(Intent data) {}

		/**
		 * On cancelled.
		 *
		 * @param data the data
		 */
		public void onCancelled(Intent data) {}
	}	

	/** The m dialog. */
	Dialog mDialog=null;

	/** The m msg. */
	String mMsg=null;

	/** The m data. */
	Intent mData=null;

	/** The m activity. */
	Activity mActivity=null;

	private RelativeLayout mView = null;

	private LinearLayout mLayout = null;

	/**
	 * Run.
	 *
	 * @param activity the activity
	 * @param sMsg the s msg
	 * @param l the l
	 */
	final public void run(Activity activity, String sMsg, OnTaskListener l) { 
		mMsg=sMsg;
		mActivity=activity;
		mOnTaskListener=l;
		execute();
	}

	final public void run(Activity activity, String sMsg, RelativeLayout view, OnTaskListener l) { 
		mMsg=sMsg;
		mActivity=activity;
		mOnTaskListener=l;
		mView = view;
		execute();
	}

	/**
	 * Run.
	 *
	 * @param activity the activity
	 * @param sMsg the s msg
	 * @param data the data
	 * @param l the l
	 */
	final public void run(Activity activity, String sMsg, Intent data, OnTaskListener l) { 
		mMsg=sMsg;
		mActivity=activity;
		mOnTaskListener=l;
		execute();
	}


	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Void doInBackground(Void... params) {
		// TODO Auto-generated method stub
		if (mOnTaskListener!=null)
			mOnTaskListener.onData(mData);
		return null;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onCancelled()
	 */
	@Override
	protected void onCancelled()
	{

	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	protected void onPostExecute(Void paramVoid)
	{
		if(mLayout != null)
			mView.removeView(mLayout);

		if (mDialog!=null)
		{
			mDialog.dismiss();
			mDialog=null;
		}

		if (mOnTaskListener!=null)
			mOnTaskListener.onPost(mData);

		super.onPostExecute(paramVoid);
	}

	OnCancelListener mCancel = new OnCancelListener() {

		@Override
		public void onCancel(DialogInterface arg0) {
			if (mOnTaskListener!=null)
				mOnTaskListener.onCancelled(mData);			
		}	
	};

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	protected void onPreExecute()
	{
		if (mOnTaskListener!=null)
			mOnTaskListener.onPrev(mData);
		super.onPreExecute(); 
	}
}
