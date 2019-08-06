package com.jinotrain.badforum.main;

import com.jinotrain.badforum.data.ServerData;
import com.jinotrain.badforum.configs.ServerConfig;
import com.jinotrain.badforum.util.LogHelper;
import com.jinotrain.badforum.util.PathFinder;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Bootstrapper
{
    private static Logger logger = LoggerFactory.getLogger(Bootstrapper.class);

    public static void main(String[] argv)
    {
        Bootstrapper main = new Bootstrapper();
        main.run();
    }

    public Bootstrapper() {}

    public void run()
    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        try
        {
            Properties prop = System.getProperties();
            prop.setProperty("badforum.jarpath", PathFinder.getExecutablePath());
            logger.info("JAR directory is " + prop.getProperty("badforum.jarpath", ""));

            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ServerConfig.class);
            ServerData sdata = (ServerData)context.getBean("serverData");
            int port = sdata.getPort();
            logger.info("Forum starting on port " + port);

            String basetmp = System.getProperty("java.io.tmpdir");

            try
            {
                File tmpdir = File.createTempFile("forum", "", new File(basetmp));
                tmpdir.delete();
                tmpdir.mkdir();
                tmpdir.deleteOnExit();

                String tmppath = tmpdir.getAbsolutePath();

                logger.info("Temporary files at \"{}\"", tmppath);

                Tomcat tomcat = new Tomcat();
                tomcat.setPort(port);
                tomcat.setBaseDir(tmppath);

                tomcat.getHost().setAppBase(".");
                Context ctx = tomcat.addWebapp("", ".");

                ctx.getJarScanner().setJarScanFilter((jarScanType, jarName) -> jarName.startsWith("badforum"));

                // necessary for the server to actually bind to anything
                tomcat.getConnector();

                tomcat.start();
                tomcat.getServer().await();
            }
            catch (IOException e)
            {
                logger.error("Could not create temporary directory in {} ({})", basetmp, e.getLocalizedMessage());
                logger.error("Exiting with status 1");
                System.exit(1);
            }
            finally
            {
                context.close();
            }
        }
        catch (Exception e)
        {
            LogHelper.dumpException(logger, e);
            logger.error("Exiting with status 2");
            System.exit(2);
        }
    }
}
