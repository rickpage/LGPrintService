package biz.rpcodes.apps.lgprinter;

/**
 * <b>For LG Printer Model PD233 ... PD241<br />
 * Does not support multiple printers in range.</b><br/><i>
 * TODO: Add at least a warning, or the ability to set a specific...<br/>
 * TODO: ...MAC, name, etc, for the desired Printer.
 * </i>
 *<p>
 * A client registers, and then will recieve connectivity information
 * about the printer. The service repeatedly sends value indicating
 * if the printer test connection was successful.
 * When printing, the client writes a file and sends the URI in a
 * print request to the service. All registered clients will get information
 * about the job (start, finish), and may update their UI or
 * ignore these messages (and instead wait for the next handled message
 * i.e. the test connection)
 *</p>
 * <p>These constants do not include messages that are internal to the printing
 * process. The Service may use the same Handler for client/service
 * interaction as the LG printer uses to indicate print status, connection
 * , etc.
 *</p>
 * <p>
 *     The Service expects images TODO: in what formats are acceprtable?
 *     TODO: External storage required, document.
 * </p>
 *  Created by Pagga on 11/21/2015.
 */
public class PrintIntentConstants {
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * TODO: Test function and value. Remove.
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    public static final int MSG_SET_VALUE = 3;

    /**
     * Service sends to Clients periodically. Reports
     * true if device (phone, etc) could connect to (the first paired)
     * LG printer. False indicates printer was not in range, or off.
     */
    public static final int MSG_RESPONSE_STATUS = 4;

    /**
     * <p>
     * Client Activity sends to Service along with Uri. This Uri should
     * be on external storage or accessible to this Service
     * </p>
     * <p>Without content provider solution,
     * this currently requires EXTERNAL STORAGE to function,
     * so Client code
     * must save image to a public SD card path.
     * </p><br />
     * TODO: We would need a content reslover for this; for
     * TODO: now we pass a URI for a public stored file, on SD card
     */
    public static final int MSG_REQUEST_PRINT_JOB= 5;

    /**
     * <p>Service sends this back when the print job
     * requested by the Client either fails or succeeds.
     * A boolean inidcates true if success, false
     * if failure. The Client must decide if it wants
     * to retry the printing by sending
     * another request message</p>
     */
    public static final int MSG_RESPONSE_PRINT_JOB= 5;

    public static final int AVAILABLE = 1;
    public static final int UNAVAILABLE = 0;
    public static final int SUCCESS = 3;
    public static final int FAILURE = 4;


    public static final String package_name = "biz.rpcodes.apps.lgprinter";
    public static final String service_name = package_name + ".service.MessengerService";

}
