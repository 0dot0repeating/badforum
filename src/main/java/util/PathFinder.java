package util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

public class PathFinder
{
    public static String getExecutablePath()
    {
        File f;
        String jarPath;

        try
        {
            f = new File(PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jarPath = f.getAbsolutePath();

            String[] jarPathParts = jarPath.split(File.separator);

            jarPath = String.join(File.separator, Arrays.copyOfRange(jarPathParts, 0, jarPathParts.length - 1));

        }
        catch (URISyntaxException e)
        {
            f = new File(".");
            jarPath = f.getAbsolutePath();
        }


        return jarPath;
    }
}
