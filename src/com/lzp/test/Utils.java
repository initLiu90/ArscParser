package com.lzp.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Utils {
    public static byte[] copyByte(byte[] src, int start, int len) {
        if (src == null) return null;

        if (start > src.length) return null;

        if ((start + len) > src.length) return null;

        if (start < 0) return null;

        if (len <= 0) return null;

        byte[] resultByte = new byte[len];
        for (int i = 0; i < len; i++) {
            resultByte[i] = src[i + start];
        }
        return resultByte;
    }

    public static short byte2Short(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    public static int byte2int(byte[] b) {
        int targets = (b[0] & 0xff) | ((b[1] << 8) & 0xff00)
                | ((b[2] << 24) >>> 8) | (b[3] << 24);
        return targets;
    }

    public static String filterStringNull(String str) {
        if (TextUtils.isEmpty(str)) return str;

        byte[] strBytes = str.getBytes();
        ArrayList<Byte> newByte = new ArrayList<>();
        for (int i = 0; i < strBytes.length; i++) {
            if (strBytes[i] != 0) {
                newByte.add(strBytes[i]);
            }
        }
        byte[] newByteArray = new byte[newByte.size()];
        for (int i = 0; i < newByteArray.length; i++) {
            newByteArray[i] = newByte.get(i);
        }
        return new String(newByteArray);
    }

    /**
     * int to byte array
     * 网络字节序
     *
     * @param value
     * @return
     */
    public static byte[] int2ByteArray(int value) {
        byte[] result = new byte[4];

        result[0] = (byte) (value);
        result[1] = (byte) (value >> 8);
        result[2] = (byte) (value >> 16);
        result[3] = (byte) (value >> 24);

        return result;
    }

    public static byte[] short2ByteArray(short value) {
        byte[] result = new byte[2];
        result[1] = (byte) ((value >> 8) & 0xFF);
        result[0] = (byte) (value & 0xFF);
        return result;
    }
}
