package main;

import configs.DispatcherConfig;
import configs.ServerConfig;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class SpringMVCInitializer implements WebApplicationInitializer
{
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException
    {
        // boy i sure love java class names
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ServerConfig.class);

        servletContext.addListener(new ContextLoaderListener(rootContext));

        // really love em
        AnnotationConfigWebApplicationContext mainContext = new AnnotationConfigWebApplicationContext();
        mainContext.register(DispatcherConfig.class);

        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(mainContext));

        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}
