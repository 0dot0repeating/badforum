package com.jinotrain.badforum.main;

import com.jinotrain.badforum.configs.JPAConfig;
import com.jinotrain.badforum.configs.MVCConfig;
import com.jinotrain.badforum.configs.ServerConfig;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

public class SpringWebInitializer implements WebApplicationInitializer
{
    @Override
    public void onStartup(ServletContext container)
    {
        // boy i sure love java class names
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ServerConfig.class);

        container.addListener(new ContextLoaderListener(rootContext));

        // really love em
        AnnotationConfigWebApplicationContext mainContext = new AnnotationConfigWebApplicationContext();
        mainContext.register(MVCConfig.class);
        mainContext.register(JPAConfig.class);

        ServletRegistration.Dynamic dispatcher =
                container.addServlet("dispatcher", new DispatcherServlet(mainContext));

        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}
