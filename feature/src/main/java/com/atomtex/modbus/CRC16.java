package com.atomtex.modbus;

import java.util.Arrays;

/**
 * This class was designed to count check value by using the CRC-16-IBM algorithm.
 * <p>
 * A cyclic redundancy check (CRC) is an error-detecting code commonly used in digital networks
 * and storage devices to detect accidental changes to raw data.
 * Blocks of data get a short check value attached, based on the remainder of a polynomial division
 * of their contents. On retrieval, the calculation is repeated and, in the event the check
 * values do not match, corrective action can be taken against data corruption.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public class CRC16 {

    private CRC16() {
    }

    /**
     * Counts the check value.
     *
     * @param dataBuffer is the source buffer for counting
     * @return int value of a check value
     */
    public static int calcCRC(byte[] dataBuffer) {
        int sum = 0xffff;
        byte[] arr = dataBuffer;
        for (int i = 0; i < arr.length; i++) {
            sum = (sum ^ arr[i]);
            for (int j = 0; j < 8; j++) {
                if ((sum & 0x1) == 1) {
                    sum >>>= 1;
                    sum = (sum ^ 0xA001);
                } else {
                    sum >>>= 1;
                }
            }
        }
        return sum;
    }

    /**
     * Checks, whether the message is a proper one by using CRC-16-IBM algorithm.
     *
     * @param toCheck is the source data for check
     * @return whether the message is valid one
     */
    public static boolean checkCRC(byte[] toCheck) {
        if (toCheck.length < 2) {
            return false;
        }
        int x = toCheck[toCheck.length - 2];
        int y = toCheck[toCheck.length - 1];
        int crcToCheck = (x & 255) * 256 + (y & 255);
        int realCRC = calcCRC(Arrays.copyOf(toCheck, toCheck.length - 2));
        return realCRC == crcToCheck;
    }
}

