package com.jinotrain.badforum.util;

import java.io.File;
import java.net.URISyntaxException;

public final class PathFinder
{
    private final static String exePath;

    static
    {
        File f;
        String jarPath;

        try
        {
            f = new File(PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jarPath = f.getAbsoluteFile().getParent();
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
