package biz.rpcodes.apps.lgprinter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import biz.rpcodes.apps.lgprinter.service.MessengerService;

public class InstalledNotify extends ActionBarActivity {

    private Messenger mService;
    private boolean mIsAttached;
    private Messenger mMessenger = new Messenger(new Handler());
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            mIsAttached = true;
            Log.i("OK", "OK");
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        PrintIntentConstants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsAttached = false;
            Log.i("NOTOK", "NOTOK");
        }
    };

    public void doQuit(View v){
        unbindMonitorPrinterService();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installed_notify);

        // This is how we bind using external
        Intent i = new Intent();//PrintIntentConstants.start_service);
        //i.setPackage(PrintIntentConstants.package_name);
        i.setClassName(PrintIntentConstants.package_name, PrintIntentConstants.service_name);
        if (getPackageManager().resolveService(i, PackageManager.GET_INTENT_FILTERS) != null) {
            bindService(i, mConnection, Context.BIND_AUTO_CREATE);
            // mIsBound = true;
        } else {
            Toast.makeText(this.getApplicationContext()
                    , "LG Print Service Not Installed!"
                    , Toast.LENGTH_SHORT);
        }
    }


    /**
     * Stop listening for printer.
     */
    void unbindMonitorPrinterService() {
       // if (mIsBound) {

            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            PrintIntentConstants.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            if ( mConnection != null && mIsAttached) {
                // Detach our existing connection.
                unbindService(mConnection);
            }
           // mIsBound = false;
            // mCallbackText.setText("Unbinding.");
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        // unbindMonitorPrinterService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_installed_notify, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
