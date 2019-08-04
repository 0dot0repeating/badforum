package com.jinotrain.badforum.util;

import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class LogHelper
{
    public static void dumpException(Logger logger, Exception e)
    {
        logger.error("Uncaught exception {} thrown ({})", e.getClass().getSimpleName(), e.getMessage());
        logger.error("Stack trace:");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        String eStr = sw.toString();
        Arrays.stream(eStr.split("\\r?\\n")).forEach((String l) -> logger.error(l));
    }
}
