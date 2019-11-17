package com.jinotrain.badforum.util;

import java.io.File;
import java.net.URISyntaxException;

public final class PathFinder
{
    private final static String jarPath;

    static
    {
        File f;
        String _jarPath;

        try
        {
            f = new File(PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            _jarPath = f.getAbsoluteFile().getParent();
        }
        catch (URISyntaxException e)
        {
            f = new File(".");
            _jarPath = f.getAbsolutePath();
        }

        jarPath = _jarPath;
    }

    public static String getJarPath()
    {
        return jarPath;
    }
}
