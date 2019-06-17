package main;

import beans.ServerData;
import configs.ServerConfig;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import util.PathFinder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

public class Bootstrapper
{
    private Logger logger;

    public static void main(String[] argv)
    {
        Bootstrapper main = new Bootstrapper();
        main.run();
    }

    public Bootstrapper()
    {
        logger = LoggerFactory.getLogger(Bootstrapper.class);
    }

    public void run()
    {
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
                tomcat.addContext("", ".");

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
            logger.error("Uncaught exception {} thrown ({})", e.getClass().getSimpleName(), e.getMessage());
            logger.error("Stack trace:");

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            String eStr = sw.toString();
            Arrays.stream(eStr.split("\\r?\\n")).forEach((String l) -> logger.error(l));

            logger.error("Exiting with status 2");
            System.exit(2);
        }
    }
}
