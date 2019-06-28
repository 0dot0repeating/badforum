package com.jinotrain.badforum.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class PathFinder
{
    private static Logger logger = LoggerFactory.getLogger(PathFinder.class);
    private static String exePath;

    static
    {
        File f;
        String jarPath;

        try
        {
            f = new File(PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jarPath = f.getAbsolutePath();

            String[] jarPathParts = jarPath.split(Pattern.quote(File.separator));

            jarPath = String.join(File.separator, Arrays.copyOfRange(jarPathParts, 0, jarPathParts.length - 1));

        }
        catch (URISyntaxException e)
        {
            f = new File(".");
            jarPath = f.getAbsolutePath();
        }

        exePath = jarPath;
    }

    public static String getExecutablePath()
    {
        return exePath;
    }
}
