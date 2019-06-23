package com.jinotrain.badforum.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring5.ISpringTemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
@EnableWebMvc
@PropertySources({
                         @PropertySource("classpath:badforum.properties"),
                         @PropertySource(value = "file:${badforum.jarpath}/badforum.properties", ignoreResourceNotFound = true),
                         @PropertySource(value = "file:badforum.properties", ignoreResourceNotFound = true),
                 })
@ComponentScan(basePackageClasses = {
        com.jinotrain.badforum.controllers.ControllerSearchDummy.class,
})
public class MVCConfig
{
    private Logger logger = LoggerFactory.getLogger(MVCConfig.class);

    private WebApplicationContext webContext;
    private Boolean cacheTemplates;

    @Autowired
    public MVCConfig(WebApplicationContext webContext,
                     @Value("${badforum.cachetemplates:false}") Boolean cacheTemplates)
    {
        this.cacheTemplates = cacheTemplates;
        this.webContext     = webContext;
    }


    private ISpringTemplateEngine templateEngine(ITemplateResolver resolver)
    {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }


    private ViewResolver makeViewResolver(String contentType, String extension, TemplateMode tmode)
    {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine(makeTemplateResolver(extension, tmode)));
        resolver.setContentType(contentType);
        resolver.setViewNames(new String[]{"*." + extension});
        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }


    private ITemplateResolver makeTemplateResolver(String extension, TemplateMode tmode)
    {
        logger.debug("creating template resolver for {} files", extension);
        logger.debug("- webContext is {}", webContext == null ? "null" : "not null");

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(webContext);
        resolver.setPrefix("classpath:/WEB-INF/" + extension + "/");
        resolver.setSuffix("." + extension);
        resolver.setTemplateMode(tmode);
        resolver.setCacheable(cacheTemplates);
        return resolver;
    }


    @Bean
    public ViewResolver htmlResolver()
    {
        return makeViewResolver("text/html", "html", TemplateMode.HTML);
    }
}
