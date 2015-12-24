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
 *  @version $Id: ResponseCodes.java 2532 2008-12-09 20:23:14Z skarzhevskyy $  
 */
package javax.obex.bluecove;

/**
 * The <code>ResponseCodes</code> class contains the list of valid response
 * codes a server may send to a client.
 * <P>
 * <STRONG>IMPORTANT NOTE</STRONG>
 * <P>
 * It is important to note that these values are different then those defined in
 * <code>javax.microedition.io.HttpConnection</code>. The values in this
 * interface represent the values defined in the IrOBEX specification. The
 * values in <code>javax.microedition.io.HttpConnection</code> represent values
 * defined in the HTTP specification.
 * <P>
 * <code>OBEX_DATABASE_FULL</code> and <code>OBEX_DATABASE_LOCKED</code> require
 * further description since they are not defined in HTTP. The server will send
 * an <code>OBEX_DATABASE_FULL</code> message when the client requests that
 * something be placed into a database but the database is full (cannot take
 * more data). <code>OBEX_DATABASE_LOCKED</code> will be returned when the
 * client wishes to access a database, database table, or database record that
 * has been locked.
 * 
 */
public final class ResponseCodes {   
	/**
     * Defines the OBEX OBEX_HTTP_CONTINUE response code.
     * <P>
     * The value of <code>OBEX_HTTP_CONTINUE</code> is 0x90 (160).
     */	
	public static final int OBEX_HTTP_CONTINUE = 0x90;
	
	/**
     * Defines the OBEX SUCCESS response code.
     * <P>
     * The value of <code>OBEX_HTTP_OK</code> is 0xA0 (160).
     */
    public static final int OBEX_HTTP_OK = 0xA0;

    /**
     * Defines the OBEX CREATED response code.
     * <P>
     * The value of <code>OBEX_HTTP_CREATED</code> is 0xA1 (161).
     */
    public static final int OBEX_HTTP_CREATED = 0xA1;

    /**
     * Defines the OBEX ACCEPTED response code.
     * <P>
     * The value of <code>OBEX_HTTP_ACCEPTED</code> is 0xA2 (162).
     */
    public static final int OBEX_HTTP_ACCEPTED = 0xA2;

    /**
     * Defines the OBEX NON-AUTHORITATIVE INFORMATION response code.
     * <P>
     * The value of <code>OBEX_HTTP_NOT_AUTHORITATIVE</code> is 0xA3 (163).
     */
    public static final int OBEX_HTTP_NOT_AUTHORITATIVE = 0xA3;

    /**
     * Defines the OBEX NO CONTENT response code.
     * <P>
     * The value of <code>OBEX_HTTP_NO_CONTENT</code> is 0xA4 (164).
     */
    public static final int OBEX_HTTP_NO_CONTENT = 0xA4;

    /**
     * Defines the OBEX RESET CONTENT response code.
     * <P>
     * The value of <code>OBEX_HTTP_RESET</code> is 0xA5 (165).
     */
    public static final int OBEX_HTTP_RESET = 0xA5;

    /**
     * Defines the OBEX PARTIAL CONTENT response code.
     * <P>
     * The value of <code>OBEX_HTTP_PARTIAL</code> is 0xA6 (166).
     */
    public static final int OBEX_HTTP_PARTIAL = 0xA6;

    /**
     * Defines the OBEX MULTIPLE_CHOICES response code.
     * <P>
     * The value of <code>OBEX_HTTP_MULT_CHOICE</code> is 0xB0 (176).
     */
    public static final int OBEX_HTTP_MULT_CHOICE = 0xB0;

    /**
     * Defines the OBEX MOVED PERMANENTLY response code.
     * <P>
     * The value of <code>OBEX_HTTP_MOVED_PERM</code> is 0xB1 (177).
     */
    public static final int OBEX_HTTP_MOVED_PERM = 0xB1;

    /**
     * Defines the OBEX MOVED TEMPORARILY response code.
     * <P>
     * The value of <code>OBEX_HTTP_MOVED_TEMP</code> is 0xB2 (178).
     */
    public static final int OBEX_HTTP_MOVED_TEMP = 0xB2;

    /**
     * Defines the OBEX SEE OTHER response code.
     * <P>
     * The value of <code>OBEX_HTTP_SEE_OTHER</code> is 0xB3 (179).
     */
    public static final int OBEX_HTTP_SEE_OTHER = 0xB3;

    /**
     * Defines the OBEX NOT MODIFIED response code.
     * <P>
     * The value of <code>OBEX_HTTP_NOT_MODIFIED</code> is 0xB4 (180).
     */
    public static final int OBEX_HTTP_NOT_MODIFIED = 0xB4;

    /**
     * Defines the OBEX USE PROXY response code.
     * <P>
     * The value of <code>OBEX_HTTP_USE_PROXY</code> is 0xB5 (181).
     */
    public static final int OBEX_HTTP_USE_PROXY = 0xB5;

    /**
     * Defines the OBEX BAD REQUEST response code.
     * <P>
     * The value of <code>OBEX_HTTP_BAD_REQUEST</code> is 0xC0 (192).
     */
    public static final int OBEX_HTTP_BAD_REQUEST = 0xC0;

    /**
     * Defines the OBEX UNAUTHORIZED response code.
     * <P>
     * The value of <code>OBEX_HTTP_UNAUTHORIZED</code> is 0xC1 (193).
     */
    public static final int OBEX_HTTP_UNAUTHORIZED = 0xC1;

    /**
     * Defines the OBEX PAYMENT REQUIRED response code.
     * <P>
     * The value of <code>OBEX_HTTP_PAYMENT_REQUIRED</code> is 0xC2 (194).
     */
    public static final int OBEX_HTTP_PAYMENT_REQUIRED = 0xC2;

    /**
     * Defines the OBEX FORBIDDEN response code.
     * <P>
     * The value of <code>OBEX_HTTP_FORBIDDEN</code> is 0xC3 (195).
     */
    public static final int OBEX_HTTP_FORBIDDEN = 0xC3;

    /**
     * Defines the OBEX NOT FOUND response code.
     * <P>
     * The value of <code>OBEX_HTTP_NOT_FOUND</code> is 0xC4 (196).
     */
    public static final int OBEX_HTTP_NOT_FOUND = 0xC4;

    /**
     * Defines the OBEX METHOD NOT ALLOWED response code.
     * <P>
     * The value of <code>OBEX_HTTP_BAD_METHOD</code> is 0xC5 (197).
     */
    public static final int OBEX_HTTP_BAD_METHOD = 0xC5;

    /**
     * Defines the OBEX NOT ACCEPTABLE response code.
     * <P>
     * The value of <code>OBEX_HTTP_NOT_ACCEPTABLE</code> is 0xC6 (198).
     */
    public static final int OBEX_HTTP_NOT_ACCEPTABLE = 0xC6;

    /**
     * Defines the OBEX PROXY AUTHENTICATION REQUIRED response code.
     * <P>
     * The value of <code>OBEX_HTTP_PROXY_AUTH</code> is 0xC7 (199).
     */
    public static final int OBEX_HTTP_PROXY_AUTH = 0xC7;

    /**
     * Defines the OBEX REQUEST TIME OUT response code.
     * <P>
     * The value of <code>OBEX_HTTP_TIMEOUT</code> is 0xC8 (200).
     */
    public static final int OBEX_HTTP_TIMEOUT = 0xC8;

    /**
     * Defines the OBEX METHOD CONFLICT response code.
     * <P>
     * The value of <code>OBEX_HTTP_CONFLICT</code> is 0xC9 (201).
     */
    public static final int OBEX_HTTP_CONFLICT = 0xC9;

    /**
     * Defines the OBEX METHOD GONE response code.
     * <P>
     * The value of <code>OBEX_HTTP_GONE</code> is 0xCA (202).
     */
    public static final int OBEX_HTTP_GONE = 0xCA;

    /**
     * Defines the OBEX METHOD LENGTH REQUIRED response code.
     * <P>
     * The value of <code>OBEX_HTTP_LENGTH_REQUIRED</code> is 0xCB (203).
     */
    public static final int OBEX_HTTP_LENGTH_REQUIRED = 0xCB;

    /**
     * Defines the OBEX PRECONDITION FAILED response code.
     * <P>
     * The value of <code>OBEX_HTTP_PRECON_FAILED</code> is 0xCC (204).
     */
    public static final int OBEX_HTTP_PRECON_FAILED = 0xCC;

    /**
     * Defines the OBEX REQUESTED ENTITY TOO LARGE response code.
     * <P>
     * The value of <code>OBEX_HTTP_ENTITY_TOO_LARGE</code> is 0xCD (205).
     */
    public static final int OBEX_HTTP_ENTITY_TOO_LARGE = 0xCD;

    /**
     * Defines the OBEX REQUESTED URL TOO LARGE response code.
     * <P>
     * The value of <code>OBEX_HTTP_REQ_TOO_LARGE</code> is 0xCE (206).
     */
    public static final int OBEX_HTTP_REQ_TOO_LARGE = 0xCE;

    /**
     * Defines the OBEX UNSUPPORTED MEDIA TYPE response code.
     * <P>
     * The value of <code>OBEX_HTTP_UNSUPPORTED_TYPE</code> is 0xCF (207).
     */
    public static final int OBEX_HTTP_UNSUPPORTED_TYPE = 0xCF;

    /**
     * Defines the OBEX INTERNAL SERVER ERROR response code.
     * <P>
     * The value of <code>OBEX_HTTP_INTERNAL_ERROR</code> is 0xD0 (208).
     */
    public static final int OBEX_HTTP_INTERNAL_ERROR = 0xD0;

    /**
     * Defines the OBEX NOT IMPLEMENTED response code.
     * <P>
     * The value of <code>OBEX_HTTP_NOT_IMPLEMENTED</code> is 0xD1 (209).
     */
    public static final int OBEX_HTTP_NOT_IMPLEMENTED = 0xD1;

    /**
     * Defines the OBEX BAD GATEWAY response code.
     * <P>
     * The value of <code>OBEX_HTTP_BAD_GATEWAY</code> is 0xD2 (210).
     */
    public static final int OBEX_HTTP_BAD_GATEWAY = 0xD2;

    /**
     * Defines the OBEX SERVICE UNAVAILABLE response code.
     * <P>
     * The value of <code>OBEX_HTTP_UNAVAILABLE</code> is 0xD3 (211).
     */
    public static final int OBEX_HTTP_UNAVAILABLE = 0xD3;

    /**
     * Defines the OBEX GATEWAY TIMEOUT response code.
     * <P>
     * The value of <code>OBEX_HTTP_GATEWAY_TIMEOUT</code> is 0xD4 (212).
     */
    public static final int OBEX_HTTP_GATEWAY_TIMEOUT = 0xD4;

    /**
     * Defines the OBEX HTTP VERSION NOT SUPPORTED response code.
     * <P>
     * The value of <code>OBEX_HTTP_VERSION</code> is 0xD5 (213).
     */
    public static final int OBEX_HTTP_VERSION = 0xD5;

    /**
     * Defines the OBEX DATABASE FULL response code.
     * <P>
     * The value of <code>OBEX_DATABASE_FULL</code> is 0xE0 (224).
     */
    public static final int OBEX_DATABASE_FULL = 0xE0;

    /**
     * Defines the OBEX DATABASE LOCKED response code.
     * <P>
     * The value of <code>OBEX_DATABASE_LOCKED</code> is 0xE1 (225).
     */
    public static final int OBEX_DATABASE_LOCKED = 0xE1;

    /**
     * Constructor does nothing.
     */
    private ResponseCodes() {
        throw new RuntimeException("Not Implemented! Used to compile Code");
    }
} 

