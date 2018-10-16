package com.example.kleinikov_sd.exampleapp.feature;

/**
 * This class was designed to count check value by using the CRC-16-IBM algorithm.
 * <p>
 * A cyclic redundancy check (CRC) is an error-detecting code commonly used in digital networks
 * and storage devices to detect accidental changes to raw data.
 * Blocks of data get a short check value attached, based on the remainder of a polynomial division
 * of their contents. On retrieval, the calculation is repeated and, in the event the check
 * values do not match, corrective action can be taken against data corruption.
 */
public class CRC16 {

    private CRC16() {
    }

    /**
     * The nested class for implementation of lazy initialization
     */
    private static class CRC16Holder {
        private static final CRC16 instance = new CRC16();
    }

    public static CRC16 getInstance() {
        return CRC16Holder.instance;
    }

    /**
     * Counts the check value.
     *
     * @param dataBuffer is the source buffer for counting
     * @return int value of a check value
     */
    private int calcCRC16(byte[] dataBuffer) {
        int sum = 0xffff;
        byte[] arr = dataBuffer;
        for (int i = 0; i < arr.length - 2; i++) {
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
    public boolean checkCRC16(byte[] toCheck) {
        if (toCheck.length < 2) {
            return false;
        }
        int x = toCheck[toCheck.length - 2];
        int y = toCheck[toCheck.length - 1];
        int crcToCheck = (x & 255) * 256 + (y & 255);
        int realCRC = calcCRC16(toCheck);
        return realCRC == crcToCheck;
    }
}

