package biz.rpcodes.apps.lgprinter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Pagga on 11/23/2015.
 */
public class LGPrintHelper {
    // TODO: Check for external storage existance first!

    /**
     * Writes file to external storage and returns the filename.
     * Writes as a JPEG file.
     * If you want to convert to a URI you need to prepend file://
     * @param b
     * @param fileName
     * @return
     */
    static private String saveImageToExternal(Bitmap b, String fileName){ //}, int width, int height) {
        FileOutputStream fos;
        String mypath = Environment.getExternalStorageDirectory() +
                "/" + fileName + ".jpg";

        File file = new File(mypath);
        try {
            file.createNewFile();

            fos = new FileOutputStream(file);

            // Use the compress method on the BitMap object to write image to the OutputStream
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        };
        return file.toString();
    }

    /**
     * Print an existing file.
     * TODO: JPEG supported, PNG, GIF, other: untested
     * @param filepath
     * @param mService
     * @throws IOException
     */
    static public void sendPrintJob(String filepath, Messenger mService)
        throws IOException
    {
        if ( filepath != null && !filepath.isEmpty() && mService!=null ) {

            try {
                Message msg = Message.obtain(null
                        , PrintIntentConstants.MSG_REQUEST_PRINT_JOB
                );
                Bundle bun = new Bundle();
                bun.putString("filepath", filepath);
                msg.setData(bun);
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            throw new IOException("Cannot print: filepath = " + filepath
                    + ", messenger = " + mService);
        }
    }
        /**
         * Saves the bitmap, then sends print request.
         * @param b
         * @param mService
         */
        static public void sendPrintJob(Bitmap b, Messenger mService)
                throws IOException
        {
            String filepath = saveImageToExternal(b, "print");
            sendPrintJob(filepath, mService);
        }

    /**
     * Write with a custom temp filename
     * @param b
     * @param mService
     * @param tempFileName i.e. print will become /storage/0/emulated/print.jpg
     */
    static public void sendPrintJob(Bitmap b, Messenger mService, String tempFileName)
            throws IOException
    {
        String filepath = saveImageToExternal(b, tempFileName);
        sendPrintJob(filepath, mService);
    }


}
