package com.lzp.test;

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
}
