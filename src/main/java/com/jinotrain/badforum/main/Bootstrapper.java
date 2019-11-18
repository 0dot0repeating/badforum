package com.jinotrain.badforum.main;

import com.jinotrain.badforum.configs.JPAConfig;
import com.jinotrain.badforum.configs.MVCConfig;
import com.jinotrain.badforum.data.ServerData;
import com.jinotrain.badforum.configs.BootstrapConfig;
import com.jinotrain.badforum.util.LogHelper;
import com.jinotrain.badforum.util.PathFinder;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.util.Properties;

public class Bootstrapper
{
    private static Logger logger = LoggerFactory.getLogger(Bootstrapper.class);

    public static void main(String[] argv)
    {
        Bootstrapper main = new Bootstrapper();
        main.start();
    }


    private Bootstrapper() {}

    private void start()
    {
        try
        {
            Properties prop = System.getProperties();
            String jarPath = PathFinder.getJarDirectory();
            prop.setProperty("badforum.jarpath", jarPath);
            prop.setProperty("org.jboss.logging.provider", "slf4j");
            prop.setProperty("hsqldb.reconfig_logging", "false");

            logger.info("JAR directory is " + jarPath);

            String keyPath = PathFinder.getKeyPath();

            if (keyPath == null)
            {
                logger.warn("----------");
                logger.warn("No SSL key found, defaulting to HTTP-only");
                logSSLSetupMessage();
                logger.warn("----------");
            }
            else
            {
                logger.info("SSL key found at \"{}\"", keyPath);
            }

            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BootstrapConfig.class);
            ServerData sdata = (ServerData)context.getBean("serverData");
            int port           = sdata.getPort();
            int sslPort        = sdata.getSSLPort();
            String sslPassword = sdata.getSSLPassword();
            String keyAlias    = sdata.getKeyAlias();

            if (keyPath != null && (sslPassword == null || sslPassword.isEmpty()))
            {
                logger.warn("----------");
                logger.warn("No passphrase for the keystore found, defaulting to HTTP-only");
                logSSLSetupMessage();
                logger.warn("----------");
            }

            logger.info("Keystore passphrase found, and using key with alias \"{}\"", keyAlias);

            createAndRunServer(port, sslPort, keyPath, keyAlias, sslPassword);
        }
        catch (IOException e)
        {
            if (e.getCause() instanceof UnrecoverableKeyException)
            {
                logger.error("FATAL ERROR: Could not acquire SSL key from keystore");
                logger.error("Check badforum.ssl.password - it's either that, or your keystore is corrupted somehow");
                logger.error("Exiting with status 3");
                System.exit(3);
            }
            else
            {
                LogHelper.dumpException(logger, e);
                logger.error("Exiting with status 2");
                System.exit(2);
            }
        }
        catch (Exception e)
        {
            LogHelper.dumpException(logger, e);
            logger.error("Exiting with status 2");
            System.exit(2);
        }
    }


    // https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java
    // I'm mainly just reading off the above link, seeing what I need, and sticking that in here
    private void createAndRunServer(int port, int sslPort, String keyPath, String keyAlias, String sslPassword) throws Exception
    {
        Server server = new Server();
        server.setHandler(createSpringHandler());

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(sslPort);

        ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(port);
        httpConnector.setIdleTimeout(30000);
        server.addConnector(httpConnector);

        if (keyPath != null && !keyPath.isEmpty() && sslPassword != null && !sslPassword.isEmpty())
        {
            SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(keyPath);
            sslContextFactory.setKeyStorePassword(sslPassword);
            sslContextFactory.setKeyManagerPassword(sslPassword);
            sslContextFactory.setCertAlias(keyAlias);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            ServerConnector httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                                                                         new HttpConnectionFactory(httpsConfig));
            httpsConnector.setPort(sslPort);
            httpsConnector.setIdleTimeout(30000);
            server.addConnector(httpsConnector);

            logger.info("Forum starting on port {} (HTTPS: {})", port, sslPort);
        }
        else
        {
            logger.info("Forum starting on port {}", port);
        }

        server.start();
        server.join();
    }



    private ServletContextHandler createSpringHandler()
    {
        WebApplicationContext context = createContext();
        ServletContextHandler handler = new ServletContextHandler();

        handler.setErrorHandler(null);
        handler.setContextPath("/");
        handler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/*");
        handler.addEventListener(new ContextLoaderListener(context));

        return handler;
    }


    private WebApplicationContext createContext()
    {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(MVCConfig.class);
        context.register(JPAConfig.class);
        return context;
    }


    private void logSSLSetupMessage()
    {
        String keyToolPath = new File(new File(System.getProperty("java.home"), "bin"), "keytool.exe").getAbsolutePath();
        String keyPath     = new File("badforum_key.jks").getAbsolutePath();
        String configPath  = new File("badforum.properties").getAbsolutePath();

        logger.warn("To set up SSL support:");
        logger.warn("");

        logger.warn("If you already have a Java keystore, either copy it to \"{}\", or link to it from that path", keyPath);

        logger.warn("");
        logger.warn("If you don't have a Java keystore (or have no idea what that is), run this exact command:");
        logger.warn("  \"{}\" -keystore \"{}\" -alias badforum -genkey -keyalg RSA -sigalg SHA256withRSA", keyToolPath, keyPath);

        logger.warn("");
        logger.warn("Now take the password you're using for your SSL key (the one you just used, if you made a new key),");
        logger.warn("and add it to \"{}\" with the line below, creating the file if necessary:", configPath);
        logger.warn("  badforum.ssl.password=<password>");

        logger.warn("");
        logger.warn("If you already had a Java keystore set up and want to use a key with an alias that");
        logger.warn("isn't badforum, add the following line to the configuration file mentioned above:");
        logger.warn("  badforum.ssl.alias=<alias>");

        logger.warn("");
        logger.warn("If you run into a \"no cyphers overlap\" error, make sure the alias you're using in");
        logger.warn("the properties file is the same as the one you used for the key. You can check your");
        logger.warn("keystore with the following command:");
        logger.warn("  \"{}\" -keystore \"{}\" -list", keyToolPath, keyPath);

        logger.warn("");
        logger.warn("For all other issues, good luck - you'll need it.");
    }
}
