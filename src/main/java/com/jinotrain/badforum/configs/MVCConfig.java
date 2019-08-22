package com.jinotrain.badforum.configs;

import com.jinotrain.badforum.controllers.ForumSessionInjector;
import com.jinotrain.badforum.data.PreAdminKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.thymeleaf.spring5.ISpringTemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Locale;

@Configuration
@EnableWebMvc
@ForumPropertySources
@EnableJpaRepositories(basePackageClasses = {
    com.jinotrain.badforum.db.repositories.ForumPostRepository.class
})
@ComponentScan(basePackageClasses = {
        com.jinotrain.badforum.controllers.ControllerSearchDummy.class,
        com.jinotrain.badforum.components.ComponentSearchDummy.class
})
public class MVCConfig implements WebMvcConfigurer
{
    private Logger logger = LoggerFactory.getLogger(MVCConfig.class);

    @Autowired
    private WebApplicationContext webContext;

    @Autowired
    private Environment environment;


    public MVCConfig()
    {
    }


    private ISpringTemplateEngine templateEngine(ITemplateResolver resolver)
    {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messageSource());
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

        boolean cacheTemplates = Boolean.parseBoolean(environment.getProperty("badforum.cachetemplates", "true"));
        resolver.setCacheable(cacheTemplates);

        return resolver;
    }


    @Bean
    public ViewResolver viewResolver()
    {
        return makeViewResolver("text/html", "html", TemplateMode.HTML);
    }


    @Bean
    public MessageSource messageSource()
    {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("WEB-INF/messages");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultEncoding("UTF-8");

        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver()
    {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.US);
        resolver.setCookieName("forumLocale");
        resolver.setCookieMaxAge(4800);
        return resolver;
    }


    @Bean
    public ForumSessionInjector forumRequestInterceptor()
    {
        return new ForumSessionInjector();
    }


    @Bean
    public PreAdminKey preAdminKey()
    {
        return new PreAdminKey();
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(forumRequestInterceptor());
    }
}
