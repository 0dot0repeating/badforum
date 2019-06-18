package com.jinotrain.badforum.configs;

import com.jinotrain.badforum.beans.ServerData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
                 @PropertySource("classpath:badforum.properties"),
                 @PropertySource(value = "file:${badforum.jarpath}/badforum.properties", ignoreResourceNotFound = true),
                 @PropertySource(value = "file:badforum.properties", ignoreResourceNotFound = true),
                 })
public class ServerConfig
{
    @Bean
    public ServerData serverData()
    {
        return new ServerData();
    }
}
