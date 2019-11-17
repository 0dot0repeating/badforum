package com.jinotrain.badforum.main;

import com.jinotrain.badforum.configs.JPAConfig;
import com.jinotrain.badforum.configs.MVCConfig;
import com.jinotrain.badforum.data.ServerData;
import com.jinotrain.badforum.configs.BootstrapConfig;
import com.jinotrain.badforum.util.LogHelper;
import com.jinotrain.badforum.util.PathFinder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

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
            String jarPath = PathFinder.getJarPath();
            prop.setProperty("badforum.jarpath", jarPath);
            logger.info("JAR directory is " + jarPath);

            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BootstrapConfig.class);
            ServerData sdata = (ServerData)context.getBean("serverData");
            int port = sdata.getPort();
            logger.info("Forum starting on port " + port);

            Server server = new Server(port);
            server.setHandler(createSpringHandler());
            server.start();
            server.join();

        }
        catch (Exception e)
        {
            LogHelper.dumpException(logger, e);
            logger.error("Exiting with status 2");
            System.exit(2);
        }
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
}
