package com.example.kleinikov_sd.exampleapp.feature;

import com.atomtex.modbus.util.BitConverter;
import com.atomtex.modbus.util.ByteSwapper;
import com.atomtex.modbus.util.ByteUtil;
import com.atomtex.modbus.util.CRC16;

import org.junit.Test;

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
        final byte[] MESSAGE_07 = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};

        byte[] x = new byte[]{(byte) 0xe2, 0x41};

        byte[] y = new byte[]{0x01, 0x07};

        int res = CRC16.calcCRC(x);
        int res2 = CRC16.calcCRC(y);

        int one = x[x.length - 2];
        int two = x[x.length - 1];
        int crcToCheck = (one & 255) * 256 + (two & 255);

        System.out.println(CRC16.calcCRC(y));
        System.out.println(crcToCheck);

        short num = (short) 57921;
        num = ByteSwapper.swap(num);
        byte[] bytes1 = BitConverter.getBytes(num);

        System.out.println(ByteUtil.getHexString(bytes1));

    }
}