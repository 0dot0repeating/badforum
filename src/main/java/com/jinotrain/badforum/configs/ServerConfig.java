package com.jinotrain.badforum.configs;

import com.jinotrain.badforum.beans.ServerData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ForumPropertySources
public class ServerConfig
{
    @Bean
    public ServerData serverData()
    {
        return new ServerData();
    }
}
