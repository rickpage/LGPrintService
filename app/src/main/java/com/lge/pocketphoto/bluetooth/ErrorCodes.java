package com.lge.pocketphoto.bluetooth;

/**
 * Was in lge.sample.MainActivity
 * We dont like that coupling!
 * Created by Pagga on 10/21/2015.
 */
public class ErrorCodes {

    public static final int BLUETOOTH_RESPONSE_TARGET_BUSY = 1;

    // Device Error - Paper Jam
    public static final int BLUETOOTH_RESPONSE_TARGET_JAM = 2;

    // Device Error -Paper Empty
    public static final int BLUETOOTH_RESPONSE_TARGET_EMPTY = 3;

    // Device Error - Wrong Paper
    public static final int BLUETOOTH_RESPONSE_TARGET_WRONG_PAPER = 4;

    // Device Error - Data Error
    public static final int BLUETOOTH_RESPONSE_TARGET_DATA_ERROR = 5;

    // Device Error - Cover Opened
    public static final int BLUETOOTH_RESPONSE_TARGET_COVER_OPEN = 6;

    // Device Error - System Error
    public static final int BLUETOOTH_RESPONSE_TARGET_SYSTEM_ERROR = 7;

    // Device Error - Low Battery
    public static final int BLUETOOTH_RESPONSE_TARGET_BATTERY_LOW = 8;

    // Device Error - High Temperature
    public static final int BLUETOOTH_RESPONSE_TARGET_HIGH_TEMPERATURE = 10;

    // Device Error - Low Temperature
    public static final int BLUETOOTH_RESPONSE_TARGET_LOW_TEMPERATURE = 11;

    // Device Error - Cooling Mode
    public static final int BLUETOOTH_RESPONSE_TARGET_COOLING_MODE = 22;


}
