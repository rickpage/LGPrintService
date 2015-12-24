/**
 *  BlueCove - Java library for Bluetooth
 * 
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @version $Id: HeaderSet.java 2531 2008-12-09 19:43:45Z skarzhevskyy $  
 */
package javax.obex.bluecove;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.security.SecureRandom;

import com.lge.pocketphoto.bluetooth.*;

/**
 * The <code>HeaderSet</code> interface defines the methods that set and get
 * the values of OBEX headers.
 * <P>
 * The following table describes how the headers specified in this interface are
 * represented in OBEX and in Java. The Java types are used with the
 * <code>setHeader()</code> and <code>getHeader()</code> methods and specify
 * the type of object that must be provided and will be returned from these
 * methods, respectively. <TABLE BORDER>
 * <TR>
 * <TH>Header Values</TH>
 * <TH>OBEX Representation</TH>
 * <TH>Java Type</TH>
 * </TR>
 * <TR>
 * <TD>COUNT</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * <TR>
 * <TD>NAME</TD>
 * <TD>Unicode string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>TYPE</TD>
 * <TD>ASCII string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>LENGTH</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * <TR>
 * <TD>TIME_ISO_8601</TD>
 * <TD>ASCII string of the form YYYYMMDDTHHMMSS[Z] where [Z] specifies Zulu
 * time</TD>
 * <TD><code>java.util.Calendar</code></TD>
 * </TR>
 * <TR>
 * <TD>TIME_4_BYTE</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.util.Calendar</code></TD>
 * </TR>
 * <TR>
 * <TD>DESCRIPTION</TD>
 * <TD>Unicode string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>TARGET</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>HTTP</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>WHO</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>OBJECT_CLASS</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>APPLICATION_PARAMETER</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * </TABLE>
 * <P>
 * The <code>APPLICATION_PARAMETER</code> header requires some additional
 * explanation. The byte array provided with the
 * <code>APPLICATION_PARAMETER</code> should be of the form Tag-Length-Value
 * according to the OBEX specification where Tag is a byte long, Length is a
 * byte long, and Value is up to 255 bytes long. Multiple Tag-Length-Value
 * triples are allowed within a single <code>APPLICATION_PARAMETER</code>
 * header. The implementation will NOT check this condition. It is mentioned
 * only to allow for interoperability between OBEX implementations.
 * <P>
 * <STRONG>User Defined Headers</STRONG>
 * <P>
 * OBEX allows 64 user-defined header values. Depending on the header identifier
 * provided, headers have different types. The table below defines the ranges
 * and their types. <TABLE BORDER>
 * <TR>
 * <TH>Header Identifier</TH>
 * <TH>Decimal Range</TH>
 * <TH>OBEX Type</TH>
 * <TH>Java Type</TH>
 * </TR>
 * <TR>
 * <TD>0x30 to 0x3F</TD>
 * <TD>48 to 63</TD>
 * <TD>Unicode String</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>0x70 to 0x7F</TD>
 * <TD>112 to 127</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>0xB0 to 0xBF</TD>
 * <TD>176 to 191</TD>
 * <TD>1 byte</TD>
 * <TD><code>java.lang.Byte</code></TD>
 * </TR>
 * <TR>
 * <TD>0xF0 to 0xFF</TD>
 * <TD>240 to 255</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * </TABLE>
 * 
 */
public final class HeaderSet {

    /**
     * Represents the OBEX Count header. This allows the connection statement to
     * tell the server how many objects it plans to send or retrieve.
     * <P>
     * The value of <code>COUNT</code> is 0xC0 (192).
     */
    public static final int COUNT = 0xC0;

    /**
     * Represents the OBEX Name header. This specifies the name of the object.
     * <P>
     * The value of <code>NAME</code> is 0x01 (1).
     */
    public static final int NAME = 0x01;

    /**
     * Represents the OBEX Type header. This allows a request to specify the
     * type of the object (e.g. text, html, binary, etc.).
     * <P>
     * The value of <code>TYPE</code> is 0x42 (66).
     */
    public static final int TYPE = 0x42;

    /**
     * Represents the OBEX Length header. This is the length of the object in
     * bytes.
     * <P>
     * The value of <code>LENGTH</code> is 0xC3 (195).
     */
    public static final int LENGTH = 0xC3;

    /**
     * Represents the OBEX Time header using the ISO 8601 standards. This is the
     * preferred time header.
     * <P>
     * The value of <code>TIME_ISO_8601</code> is 0x44 (68).
     */
    public static final int TIME_ISO_8601 = 0x44;

    /**
     * Represents the OBEX Time header using the 4 byte representation. This is
     * only included for backwards compatibility. It represents the number of
     * seconds since January 1, 1970.
     * <P>
     * The value of <code>TIME_4_BYTE</code> is 0xC4 (196).
     */
    public static final int TIME_4_BYTE = 0xC4;

    /**
     * Represents the OBEX Description header. This is a text description of the
     * object.
     * <P>
     * The value of <code>DESCRIPTION</code> is 0x05 (5).
     */
    public static final int DESCRIPTION = 0x05;

    /**
     * Represents the OBEX Target header. This is the name of the service an
     * operation is targeted to.
     * <P>
     * The value of <code>TARGET</code> is 0x46 (70).
     */
    public static final int TARGET = 0x46;

    /**
     * Represents the OBEX HTTP header. This allows an HTTP 1.X header to be
     * included in a request or reply.
     * <P>
     * The value of <code>HTTP</code> is 0x47 (71).
     */
    public static final int HTTP = 0x47;

    /**
     * Represents the OBEX BODY header.
     * <P>
     * The value of <code>BODY</code> is 0x48 (72).
     */
    public static final int BODY = 0x48;

    /**
     * Represents the OBEX End of BODY header.
     * <P>
     * The value of <code>BODY</code> is 0x49 (73).
     */
    public static final int END_OF_BODY = 0x49;

    /**
     * Represents the OBEX Who header. Identifies the OBEX application to
     * determine if the two peers are talking to each other.
     * <P>
     * The value of <code>WHO</code> is 0x4A (74).
     */
    public static final int WHO = 0x4A;

    /**
     * Represents the OBEX Connection ID header. Identifies used for OBEX
     * connection multiplexing.
     * <P>
     * The value of <code>CONNECTION_ID</code> is 0xCB (203).
     */

    public static final int CONNECTION_ID = 0xCB;

    /**
     * Represents the OBEX Application Parameter header. This header specifies
     * additional application request and response information.
     * <P>
     * The value of <code>APPLICATION_PARAMETER</code> is 0x4C (76).
     */
    public static final int APPLICATION_PARAMETER = 0x4C;

    /**
     * Represents the OBEX authentication digest-challenge.
     * <P>
     * The value of <code>AUTH_CHALLENGE</code> is 0x4D (77).
     */
    public static final int AUTH_CHALLENGE = 0x4D;

    /**
     * Represents the OBEX authentication digest-response.
     * <P>
     * The value of <code>AUTH_RESPONSE</code> is 0x4E (78).
     */
    public static final int AUTH_RESPONSE = 0x4E;

    /**
     * Represents the OBEX Object Class header. This header specifies the OBEX
     * object class of the object.
     * <P>
     * The value of <code>OBJECT_CLASS</code> is 0x4F (79).
     */
    public static final int OBJECT_CLASS = 0x4F;

    private Long mCount; // 4 byte unsigned integer

    private String mName; // null terminated Unicode text string

    private String mType; // null terminated ASCII text string

    private Long mLength; // 4 byte unsigend integer

    private Calendar mIsoTime; // String of the form YYYYMMDDTHHMMSSZ

    private Calendar mByteTime; // 4 byte unsigned integer

    private String mDescription; // null terminated Unicode text String

    private byte[] mTarget; // byte sequence

    private byte[] mHttpHeader; // byte sequence

    private byte[] mWho; // length prefixed byte sequence

    private byte[] mAppParam; // byte sequence of the form tag length value

    private byte[] mObjectClass; // byte sequence

    private String[] mUnicodeUserDefined; //null terminated unicode string

    private byte[][] mSequenceUserDefined; // byte sequence user defined

    private Byte[] mByteUserDefined; // 1 byte

    private Long[] mIntegerUserDefined; // 4 byte unsigned integer

    private final SecureRandom mRandom;

    public byte[] nonce;

    public byte[] mAuthChall; // The authentication challenge header

    public byte[] mAuthResp; // The authentication response header

    public byte[] mConnectionID; // THe connection ID

    /**
	 * @uml.property  name="responseCode"
	 */
    public int responseCode;

    /**
     * Creates new <code>HeaderSet</code> object.
     * @param size the max packet size for this connection
     */
    public HeaderSet() {
        mUnicodeUserDefined = new String[16];
        mSequenceUserDefined = new byte[16][];
        mByteUserDefined = new Byte[16];
        mIntegerUserDefined = new Long[16];
        responseCode = -1;
        mRandom = new SecureRandom();
    }

    /**
	 * Sets the value of the header identifier to the value provided. The type
	 * of object must correspond to the Java type defined in the description of
	 * this interface. If <code>null</code> is passed as the
	 * <code>headerValue</code> then the header will be removed from the set
	 * of headers to include in the next request.
	 * 
	 * @param headerID
	 *            the identifier to include in the message
	 * 
	 * @param headerValue
	 *            the value of the header identifier
	 * 
	 * @exception IllegalArgumentException
	 *                if the header identifier provided is not one defined in
	 *                this interface or a user-defined header; if the type of
	 *                <code>headerValue</code> is not the correct Java type as
	 *                defined in the description of this interface
	 */
    public void setHeader(int headerID, Object headerValue) {
        long temp = -1;

        switch (headerID) {
            case COUNT:
                if (!(headerValue instanceof Long)) {
                    if (headerValue == null) {
                        mCount = null;
                        break;
                    }
                    throw new IllegalArgumentException("Count must be a Long");
                }
                temp = ((Long)headerValue).longValue();
                if ((temp < 0L) || (temp > 0xFFFFFFFFL)) {
                    throw new IllegalArgumentException("Count must be between 0 and 0xFFFFFFFF");
                }
                mCount = (Long)headerValue;
                break;
            case NAME:
                if ((headerValue != null) && (!(headerValue instanceof String))) {
                    throw new IllegalArgumentException("Name must be a String");
                }
              
                break;
            case TYPE:
                if ((headerValue != null) && (!(headerValue instanceof String))) {
                    throw new IllegalArgumentException("Type must be a String");
                }
                mType = (String)headerValue;
                break;
            case LENGTH:
                if (!(headerValue instanceof Long)) {
                    if (headerValue == null) {
                        mLength = null;
                        break;
                    }
                    throw new IllegalArgumentException("Length must be a Long");
                }
                temp = ((Long)headerValue).longValue();
                if ((temp < 0L) || (temp > 0xFFFFFFFFL)) {
                    throw new IllegalArgumentException("Length must be between 0 and 0xFFFFFFFF");
                }
                mLength = (Long)headerValue;
                break;
            case TIME_ISO_8601:
                if ((headerValue != null) && (!(headerValue instanceof Calendar))) {
                    throw new IllegalArgumentException("Time ISO 8601 must be a Calendar");
                }
                mIsoTime = (Calendar)headerValue;
                break;
            case TIME_4_BYTE:
                if ((headerValue != null) && (!(headerValue instanceof Calendar))) {
                    throw new IllegalArgumentException("Time 4 Byte must be a Calendar");
                }
                mByteTime = (Calendar)headerValue;
                break;
            case DESCRIPTION:
                if ((headerValue != null) && (!(headerValue instanceof String))) {
                    throw new IllegalArgumentException("Description must be a String");
                }
                mDescription = (String)headerValue;
                break;
            case TARGET:
                if (headerValue == null) {
                    mTarget = null;
                } else {
                    if (!(headerValue instanceof byte[])) {
                        throw new IllegalArgumentException("Target must be a byte array");
                    } else {
                        mTarget = new byte[((byte[])headerValue).length];
                        System.arraycopy(headerValue, 0, mTarget, 0, mTarget.length);
                    }
                }
                break;
            case HTTP:
                if (headerValue == null) {
                    mHttpHeader = null;
                } else {
                    if (!(headerValue instanceof byte[])) {
                        throw new IllegalArgumentException("HTTP must be a byte array");
                    } else {
                        mHttpHeader = new byte[((byte[])headerValue).length];
                        System.arraycopy(headerValue, 0, mHttpHeader, 0, mHttpHeader.length);
                    }
                }
                break;
            case WHO:
                if (headerValue == null) {
                    mWho = null;
                } else {
                    if (!(headerValue instanceof byte[])) {
                        throw new IllegalArgumentException("WHO must be a byte array");
                    } else {
                        mWho = new byte[((byte[])headerValue).length];
                        System.arraycopy(headerValue, 0, mWho, 0, mWho.length);
                    }
                }
                break;
            case OBJECT_CLASS:
                if (headerValue == null) {
                    mObjectClass = null;
                } else {
                    if (!(headerValue instanceof byte[])) {
                        throw new IllegalArgumentException("Object Class must be a byte array");
                    } else {
                        mObjectClass = new byte[((byte[])headerValue).length];
                        System.arraycopy(headerValue, 0, mObjectClass, 0, mObjectClass.length);
                    }
                }
                break;
            case APPLICATION_PARAMETER:
                if (headerValue == null) {
                    mAppParam = null;
                } else {
                    if (!(headerValue instanceof byte[])) {
                        throw new IllegalArgumentException(
                                "Application Parameter must be a byte array");
                    } else {
                        mAppParam = new byte[((byte[])headerValue).length];
                        System.arraycopy(headerValue, 0, mAppParam, 0, mAppParam.length);
                    }
                }
                break;
            default:
                // Verify that it was not a Unicode String user Defined
                if ((headerID >= 0x30) && (headerID <= 0x3F)) {
                    if ((headerValue != null) && (!(headerValue instanceof String))) {
                        throw new IllegalArgumentException(
                                "Unicode String User Defined must be a String");
                    }
                    mUnicodeUserDefined[headerID - 0x30] = (String)headerValue;

                    break;
                }
                // Verify that it was not a byte sequence user defined value
                if ((headerID >= 0x70) && (headerID <= 0x7F)) {

                    if (headerValue == null) {
                        mSequenceUserDefined[headerID - 0x70] = null;
                    } else {
                        if (!(headerValue instanceof byte[])) {
                            throw new IllegalArgumentException(
                                    "Byte Sequence User Defined must be a byte array");
                        } else {
                            mSequenceUserDefined[headerID - 0x70] = new byte[((byte[])headerValue).length];
                            System.arraycopy(headerValue, 0, mSequenceUserDefined[headerID - 0x70],
                                    0, mSequenceUserDefined[headerID - 0x70].length);
                        }
                    }
                    break;
                }
                // Verify that it was not a Byte user Defined
                if ((headerID >= 0xB0) && (headerID <= 0xBF)) {
                    if ((headerValue != null) && (!(headerValue instanceof Byte))) {
                        throw new IllegalArgumentException("ByteUser Defined must be a Byte");
                    }
                    mByteUserDefined[headerID - 0xB0] = (Byte)headerValue;

                    break;
                }
                // Verify that is was not the 4 byte unsigned integer user
                // defined header
                if ((headerID >= 0xF0) && (headerID <= 0xFF)) {
                    if (!(headerValue instanceof Long)) {
                        if (headerValue == null) {
                            mIntegerUserDefined[headerID - 0xF0] = null;
                            break;
                        }
                        throw new IllegalArgumentException("Integer User Defined must be a Long");
                    }
                    temp = ((Long)headerValue).longValue();
                    if ((temp < 0L) || (temp > 0xFFFFFFFFL)) {
                        throw new IllegalArgumentException(
                                "Integer User Defined must be between 0 and 0xFFFFFFFF");
                    }
                    mIntegerUserDefined[headerID - 0xF0] = (Long)headerValue;
                    break;
                }
                throw new IllegalArgumentException("Invalid Header Identifier");
        }
    }

    /**
	 * Retrieves the value of the header identifier provided. The type of the
	 * Object returned is defined in the description of this interface.
	 * 
	 * @param headerID
	 *            the header identifier whose value is to be returned
	 * 
	 * @return the value of the header provided or <code>null</code> if the
	 *         header identifier specified is not part of this
	 *         <code>HeaderSet</code> object
	 * 
	 * @exception IllegalArgumentException
	 *                if the <code>headerID</code> is not one defined in this
	 *                interface or any of the user-defined headers
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                operation or if the connection has been closed
	 */
    public Object getHeader(int headerID) throws IOException {

        switch (headerID) {
            case COUNT:
                return mCount;
            case NAME:
                return mName;
            case TYPE:
                return mType;
            case LENGTH:
                return mLength;
            case TIME_ISO_8601:
                return mIsoTime;
            case TIME_4_BYTE:
                return mByteTime;
            case DESCRIPTION:
                return mDescription;
            case TARGET:
                return mTarget;
            case HTTP:
                return mHttpHeader;
            case WHO:
                return mWho;
            case OBJECT_CLASS:
                return mObjectClass;
            case APPLICATION_PARAMETER:
                return mAppParam;
            default:
                // Verify that it was not a Unicode String user Defined
                if ((headerID >= 0x30) && (headerID <= 0x3F)) {
                    return mUnicodeUserDefined[headerID - 0x30];
                }
                // Verify that it was not a byte sequence user defined header
                if ((headerID >= 0x70) && (headerID <= 0x7F)) {
                    return mSequenceUserDefined[headerID - 0x70];
                }
                // Verify that it was not a byte user defined header
                if ((headerID >= 0xB0) && (headerID <= 0xBF)) {
                    return mByteUserDefined[headerID - 0xB0];
                }
                // Verify that it was not a integer user defined header
                if ((headerID >= 0xF0) && (headerID <= 0xFF)) {
                    return mIntegerUserDefined[headerID - 0xF0];
                }
                throw new IllegalArgumentException("Invalid Header Identifier");
        }
    }

    /**
	 * Retrieves the list of headers that may be retrieved via the
	 * <code>getHeader</code> method that will not return <code>null</code>.
	 * In other words, this method returns all the headers that are available in
	 * this object.
	 * 
	 * @see #getHeader
	 * 
	 * @return the array of headers that are set in this object or
	 *         <code>null</code> if no headers are available
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                operation or the connection has been closed
	 */
    public int[] getHeaderList() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (mCount != null) {
            out.write(COUNT);
        }
        if (mName != null) {
            out.write(NAME);
        }
        if (mType != null) {
            out.write(TYPE);
        }
        if (mLength != null) {
            out.write(LENGTH);
        }
        if (mIsoTime != null) {
            out.write(TIME_ISO_8601);
        }
        if (mByteTime != null) {
            out.write(TIME_4_BYTE);
        }
        if (mDescription != null) {
            out.write(DESCRIPTION);
        }
        if (mTarget != null) {
            out.write(TARGET);
        }
        if (mHttpHeader != null) {
            out.write(HTTP);
        }
        if (mWho != null) {
            out.write(WHO);
        }
        if (mAppParam != null) {
            out.write(APPLICATION_PARAMETER);
        }
        if (mObjectClass != null) {
            out.write(OBJECT_CLASS);
        }

        for (int i = 0x30; i < 0x40; i++) {
            if (mUnicodeUserDefined[i - 0x30] != null) {
                out.write(i);
            }
        }

        for (int i = 0x70; i < 0x80; i++) {
            if (mSequenceUserDefined[i - 0x70] != null) {
                out.write(i);
            }
        }

        for (int i = 0xB0; i < 0xC0; i++) {
            if (mByteUserDefined[i - 0xB0] != null) {
                out.write(i);
            }
        }

        for (int i = 0xF0; i < 0x100; i++) {
            if (mIntegerUserDefined[i - 0xF0] != null) {
                out.write(i);
            }
        }

        byte[] headers = out.toByteArray();
        out.close();

        if ((headers == null) || (headers.length == 0)) {
            return null;
        }

        int[] result = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            // Convert the byte to a positive integer.  That is, an integer
            // between 0 and 256.
            result[i] = headers[i] & 0xFF;
        }

        return result;
    }

    /**
	 * Sets the authentication challenge header. The <code>realm</code> will
	 * be encoded based upon the default encoding scheme used by the
	 * implementation to encode strings. Therefore, the encoding scheme used to
	 * encode the <code>realm</code> is application dependent.
	 * 
	 * @param realm
	 *            a short description that describes what password to use; if
	 *            <code>null</code> no realm will be sent in the
	 *            authentication challenge header
	 * 
	 * @param userID
	 *            if <code>true</code>, a user ID is required in the reply;
	 *            if <code>false</code>, no user ID is required
	 * 
	 * @param access
	 *            if <code>true</code> then full access will be granted if
	 *            successful; if <code>false</code> then read-only access will
	 *            be granted if successful
	 */
    public void createAuthenticationChallenge(String realm, boolean userID, boolean access)
            throws IOException {

//        nonce = new byte[16];
//        for (int i = 0; i < 16; i++) {
//            nonce[i] = (byte)mRandom.nextInt();
//        }
//
//        mAuthChall = ObexHelper.computeAuthenticationChallenge(nonce, realm, access, userID);
    }

    /**
	 * Returns the response code received from the server. Response codes are
	 * defined in the <code>ResponseCodes</code> class.
	 * 
	 * @see ResponseCodes
	 * 
	 * @return the response code retrieved from the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                transaction; if this method is called on a
	 *                <code>HeaderSet</code> object created by calling
	 *                <code>createHeaderSet()</code> in a
	 *                <code>ClientSession</code> object; if an OBEX server
	 *                created this object
	 */
    public int getResponseCode() throws IOException {
        if (responseCode == -1) {
            throw new IOException("May not be called on a server");
        } else {
            return responseCode;
        }
    }
}
