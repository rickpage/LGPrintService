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
 *  @version $Id: PasswordAuthentication.java 2532 2008-12-09 20:23:14Z skarzhevskyy $  
 */
package javax.obex.bluecove;

/**
 * This class holds user name and password combinations.
 * 
 */
public final class PasswordAuthentication {

    private byte[] mUserName;

    private final byte[] mPassword;

    /**
     * Creates a new <code>PasswordAuthentication</code> with the user name and
     * password provided.
     * 
     * @param userName
     *            the user name to include; this may be <code>null</code>
     * 
     * @param password
     *            the password to include in the response
     * 
     * @exception NullPointerException
     *                if <code>password</code> is <code>null</code>
     */
    public PasswordAuthentication(final byte[] userName, final byte[] password) {
        if (userName != null) {
            mUserName = new byte[userName.length];
            System.arraycopy(userName, 0, mUserName, 0, userName.length);
        }

        mPassword = new byte[password.length];
        System.arraycopy(password, 0, mPassword, 0, password.length);
    }

    /**
     * Retrieves the user name that was specified in the constructor. The user
     * name may be <code>null</code>.
     * 
     * @return the user name
     */
    public byte[] getUserName() {
        return mUserName;
    }

    /**
     * Retrieves the password.
     * 
     * @return the password
     */
    public byte[] getPassword() {
        return mPassword;
    }
}
