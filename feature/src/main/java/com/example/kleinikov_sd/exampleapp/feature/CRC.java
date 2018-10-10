package com.example.kleinikov_sd.exampleapp.feature;

public class CRC {

    private CRC() {
    }

    private static class CRCHolder {
        private static final CRC instance = new CRC();
    }

    public static CRC getInstance() {
        return CRCHolder.instance;
    }

    private int calcCRC(byte[] dataBuffer) {
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

    public boolean checkCRC(byte[] toCheck) {
        if (toCheck.length < 2) {
            return false;
        }
        int x = toCheck[toCheck.length - 2];
        int y = toCheck[toCheck.length - 1];
        int crcToCheck = (x & 255) * 256 + (y & 255);
        int realCRC = calcCRC(toCheck);
        return realCRC == crcToCheck;
    }
}

