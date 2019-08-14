package com.jinotrain.badforum.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DurationFormat
{
    public static String format(Duration floodWindow)
    {
        long totalSeconds = floodWindow.getSeconds();

        long days    =  totalSeconds / (60*60*24);
        long hours   = (totalSeconds / (60*60)) % 24;
        long minutes = (totalSeconds /  60) % 60;
        long seconds =  totalSeconds %  60;

        List<String> strs = new ArrayList<>();

        if (days    > 0)                   { strs.add(days    + " day"    + (days    == 1 ? "" : "s")); }
        if (hours   > 0)                   { strs.add(hours   + " hour"   + (hours   == 1 ? "" : "s")); }
        if (minutes > 0)                   { strs.add(minutes + " minute" + (minutes == 1 ? "" : "s")); }
        if (seconds > 0 || strs.isEmpty()) { strs.add(seconds + " second" + (seconds == 1 ? "" : "s")); }

        int strCount = strs.size();

        if (strCount == 1)
        {
            String ret = strs.get(0);

            if (ret.startsWith("1 ")) { return ret.substring(2); }
            return ret;
        }

        if (strCount == 2)
        {
            return strs.get(0) + " and " + strs.get(1);
        }

        String leftPart = String.join(", ", strs.subList(0, strCount-1));
        return leftPart + ", and " + strs.get(strCount-1);
    }
}
