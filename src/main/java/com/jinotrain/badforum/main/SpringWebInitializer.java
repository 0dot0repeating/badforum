package com.jinotrain.badforum.main;

import com.jinotrain.badforum.configs.ServerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = {
                    com.jinotrain.badforum.controllers.ControllerSearchDummy.class,
               })
public class SpringWebInitializer implements WebApplicationInitializer
{
    private static Logger log = LoggerFactory.getLogger(SpringWebInitializer.class);

    @Override
    public void onStartup(ServletContext container) throws ServletException
    {
        log.info("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        // boy i sure love java class names
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ServerConfig.class);

        container.addListener(new ContextLoaderListener(rootContext));

        // really love em
        AnnotationConfigWebApplicationContext mainContext = new AnnotationConfigWebApplicationContext();
        mainContext.register(getClass());

        ServletRegistration.Dynamic dispatcher =
                container.addServlet("dispatcher", new DispatcherServlet(mainContext));

        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}
