package com.example.kleinikov_sd.exampleapp.feature;

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
    public void byteTest(){
        byte first = (byte) 226;
        System.out.println(first);
        System.out.println(Integer.toHexString(first));
        int second = first & 0xff;
        System.out.println(second);
        System.out.println(Integer.toHexString(second));

        byte x = -30;
        System.out.println(Integer.toHexString(x&0xff));

    }
}