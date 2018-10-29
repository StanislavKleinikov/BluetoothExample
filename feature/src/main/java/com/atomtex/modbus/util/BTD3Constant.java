package com.atomtex.feature.util;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class BTD3Constant {

    public static final byte ADDRESS = 0x01;

    /**
     * To consider the status of the binary signals
     */
    public static final byte READ_BS = 0x02;

    /**
     * Consider the state of the control registers
     */
    public static final byte READ_CR = 0x03;

    /**
     * Read state of data registers
     */
    public static final byte READ_DR = 0x04;

    /**
     * Send control signal
     */
    public static final byte SEND_CS = 0x05;

    /**
     * Change the state of the control register
     */
    public static final byte CHANGE_CR = 0x06;

    /**
     * Read status word
     */
    public static final byte READ_SW = 0x07;

    /**
     * Diagnostics
     */
    public static final byte DIAGNOSTCICS = 0x08;

    /**
     * Consider a sample spectrum from the non-volatile memory
     */
    public static final byte READ_SPECTR_NV = 0x09;

    /**
     * Record the sample spectrum in the non-volatile memory
     */
    public static final byte WRITE_SPECTR_NV = 0x10;

    /**
     * To consider the accumulated sample spectrum
     */
    public static final byte READ_SPECTR_SP = 0x11;

    /**
     * Change the state of control registers
     */
    public static final byte CHANGE_MULTIPLY_CR = 0x16;

    /**
     * Read device identification code
     */
    public static final byte READ_ID = 0x17;

    /**
     * Read calibration data sample
     */
    public static final byte READ_CDS = 0x18;

    /**
     * Record calibration data sample
     */
    public static final byte WRITE_CDS = 0x19;
}
