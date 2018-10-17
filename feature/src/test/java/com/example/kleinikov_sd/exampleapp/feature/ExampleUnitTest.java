package com.example.kleinikov_sd.exampleapp.feature;

import android.renderscript.Byte2;
import android.renderscript.Byte3;
import android.renderscript.Byte4;
import android.util.Log;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void swapTest() {

        byte[] x = new byte[]{0x40,0x49,0xe,0x56};
        byte[] y  = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};
        byte[] z  = new byte[]{0x34,0x56};


        System.out.println(CRC16.checkCRC(y));

        int res = CRC16.calcCRC(new byte[]{0x01,0x07});

        System.out.println(res);

        ByteBuffer buffer = ByteBuffer.wrap(z);

        int result = buffer.getShort();
        System.out.println(result);

    }
}