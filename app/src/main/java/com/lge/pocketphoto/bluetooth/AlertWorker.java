package com.lge.pocketphoto.bluetooth;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

/**
 * The Class AlertWorker.
 */
public class AlertWorker {

	/**
	 * The listener interface for receiving onInit events.
	 * The class that is interested in processing a onInit
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnInitListener<code> method. When
	 * the onInit event occurs, that object's appropriate
	 * method is invoked.
	 *
	 */
	public abstract static class OnInitListener {
		  
		 /** The m click listener. */
 		protected View.OnClickListener mClickListener=null;
		 
 		/** The m alert. */
 		protected Dialog mAlert = null;
		  
 		DatePickerDialog.OnDateSetListener mDateSetListener = null; 

		
		  /**
  		 * Instantiates a new on init listener.
  		 *
  		 * @param listerner the listerner
  		 */
  		public OnInitListener(final OnAlertListener listerner)
		  {
			  mClickListener =
	        		new View.OnClickListener() {
	    				@Override
	    				public void onClick(View v) {
	    					Log.d("----", "<--alert dismiss");
	    					mAlert.dismiss();
	    					Log.d("----", "-->alert dismiss");
	    					
	    					final Object tag = v.getTag();
	    					(new Handler()).post(new Runnable() {
								@Override
								public void run() {
						        	if (listerner!=null) listerner.onClick(tag);
								}});
	    				}
	        	};
		  }

		/**
  		 * Sets the alert.
  		 *
  		 * @param alert the new alert
  		 */
  		void setAlert(Dialog alert) {mAlert=alert;}
		  
		  /**
  		 * On init.
  		 *
  		 * @param alert the alert
  		 * @param view the view
  		 */
  		public abstract void onInit(Dialog alert, View view);
		  
  		/**
  		 * On cancel.
  		 */
  		public abstract void OnCancel();
		  
  		/**
  		 * On outside touch.
  		 */
  		public void OnOutsideTouch() {} 
	}	
	
	/**
	 * The Class AlertDlg.
	 */
	public static class AlertDlg extends Dialog
	{
		
		/**
		 * The m listener.
		 * @uml.property  name="mListener"
		 * @uml.associationEnd  
		 */
		OnInitListener mListener;
		
		/**
		 * Instantiates a new alert dlg.
		 *
		 * @param context the context
		 * @param theme the theme
		 */
		public AlertDlg(Context context, int theme)
		{
			super(context,theme);
		}
		
		/**
		 * Sets the listerner.
		 *
		 * @param linstener the new listerner
		 */
		public void setListerner(OnInitListener linstener)
		{
			mListener=linstener;
			
		}
		
	    /* (non-Javadoc)
    	 * @see android.app.Dialog#onKeyDown(int, android.view.KeyEvent)
    	 */
    	public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent)
	    {
	      if (paramInt == KeyEvent.KEYCODE_BACK)
	      {
	    	  if(mListener!=null) { 
				  Log.d("----", "<--alert dismiss");
	    	  	  this.dismiss();
				  Log.d("----", "-->alert dismiss");
	    		  mListener.OnCancel();
	    	  	  return true;
	    	  }
	      }
	      
	      return super.onKeyDown(paramInt, paramKeyEvent); 
	    }

		/* (non-Javadoc)
		 * @see android.app.Dialog#onTouchEvent(android.view.MotionEvent)
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event)
	    {
			 if(mListener!=null) 
				 mListener.OnOutsideTouch();
			return super.onTouchEvent(event);
	    }
	};
	
	/**
	 * Show alert.
	 *
	 * @param activity the activity
	 * @param dialog_resId the dialog_res id
	 * @param linstener the linstener
	 */
	protected static void showAlert(Activity activity, int dialog_theme,int dialog_resId, final OnInitListener linstener)
	{
		LayoutInflater factory = LayoutInflater.from(activity);
    	final View view = factory.inflate(dialog_resId, null);     
    	final AlertDlg alert=new AlertDlg(activity, dialog_theme);
    	
    	alert.setListerner(linstener);
    	if (linstener!=null)
    	{
    		linstener.setAlert(alert);
    		linstener.onInit(alert, view);
    	}
    	alert.setContentView(view);
    	
    	view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){}
		});
    	
    	alert.show();
	}
	
	/**
	 * The listener interface for receiving onAlert events.
	 * The class that is interested in processing a onAlert
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnAlertListener<code> method. When
	 * the onAlert event occurs, that object's appropriate
	 * method is invoked.
	 *
	 */
	public abstract static class OnAlertListener {
		  
  		/**
  		 * On click.
  		 *
  		 * @param tag the tag
  		 */
  		public abstract void onClick(Object tag);
	}	
}
