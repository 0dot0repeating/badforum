package com.jinotrain.badforum.util;

public class MiscFuncs
{
    public static long clamp(long val, long low, long high)
    {
        if (val < low)  { return low; }
        if (val > high) { return high; }
        return val;
    }

    public static int clamp(int val, int low, int high)
    {
        if (val < low)  { return low; }
        if (val > high) { return high; }
        return val;
    }

    public static double clamp(double val, double low, double high)
    {
        if (val < low)  { return low; }
        if (val > high) { return high; }
        return val;
    }
}
