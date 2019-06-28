package com.jinotrain.badforum.configs;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PropertySources({
                         @PropertySource("classpath:badforum.properties"),
                         @PropertySource(value = "file:${badforum.jarpath}/badforum.properties", ignoreResourceNotFound = true),
                         @PropertySource(value = "file:badforum.properties", ignoreResourceNotFound = true),
                 })
public @interface ForumPropertySources
{
}
