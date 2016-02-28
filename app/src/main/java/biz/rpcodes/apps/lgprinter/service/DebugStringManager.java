package biz.rpcodes.apps.lgprinter.service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles adding strings to a collection so
 * we can send debug strings as replies
 *
 * When the string is called, it saves its value off,
 * and sends back the old + new
 * Created by page on 2/26/16.
 */
public class DebugStringManager {
    int composeCount = 0;
    final String OLD_HEADER= "\nLAST MESSAGE\n";
    final String NEW_HEADER="\nNEW MESSAGE\n";
    final String DEFAULT_MESSAGE = "\nNothing yet\n";

    String oldMessage = DEFAULT_MESSAGE;
    String newMessage = DEFAULT_MESSAGE;
    private int MAX_AMOUNT_MESSAGES = 1024;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String getNowString(){
        return dateFormatter.format(new Date());
    }
    /**
     *
     * @return If null, this means we do not want to send messages, so we dont clutter server
     */
    public String getDebugMessage(){
        String s = "\n";
        if ( composeCount <= MAX_AMOUNT_MESSAGES ) {
            composeCount++;
        } else {
            return null;
        }

        if ( composeCount >= MAX_AMOUNT_MESSAGES ){
            s += "!!!! LIMIT REACHED, an idle application should not flood our server !!!!!";
        } else {
            s += composeCount + " messages have been composed by LG Print Service.\n";
        }
        s = OLD_HEADER + oldMessage + NEW_HEADER + newMessage + s;

        oldMessage = newMessage;
        newMessage = "";

        return s;
    }

    /**
     * Adds date + : + s + newline to newMessage
     * @param s
     */
    public void addString(String s){

        newMessage += "\n" + getNowString() + " : " + s;
    }
}
