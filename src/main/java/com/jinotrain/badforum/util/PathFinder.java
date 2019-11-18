package com.jinotrain.badforum.util;

import java.io.File;
import java.net.URISyntaxException;

public final class PathFinder
{
    private final static String jarDirectory;
    private final static String keyPath;

    static
    {
        File jar, key;
        String _jarPath, _keyPath;

        try
        {
            jar = new File(PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            _jarPath = jar.getAbsoluteFile().getParent();
        }
        catch (URISyntaxException e)
        {
            jar = new File(".");
            _jarPath = jar.getAbsolutePath();
        }

        key = new File(jar, "badforum_key.jks");

        if (!key.isFile())
        {
            key = new File("badforum_key.jks");
        }

        jarDirectory = _jarPath;
        keyPath      = key.isFile() ? key.getAbsolutePath() : null;
    }

    public static String getJarDirectory()
    {
        return jarDirectory;
    }

    public static String getKeyPath()
    {
        return keyPath;
    }
}
