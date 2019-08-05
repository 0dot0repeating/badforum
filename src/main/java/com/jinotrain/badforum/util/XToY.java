package com.jinotrain.badforum.util;

public final class XToY
{
    private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7',
                                            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();

        for (byte b: bytes)
        {
            // 0xFF turns byte into int and gets its unsigned value
            int leftHex  = (b & 0xFF) >> 4;
            int rightHex = b & 0xF;

            sb.append(hexChars[leftHex]);
            sb.append(hexChars[rightHex]);
        }

        return sb.toString();
    }
}
