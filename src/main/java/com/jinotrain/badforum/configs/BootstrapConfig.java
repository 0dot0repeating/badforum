package com.jinotrain.badforum.configs;

import com.jinotrain.badforum.data.ServerData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ForumPropertySources
public class BootstrapConfig
{
    @Bean
    public ServerData serverData()
    {
        return new ServerData();
    }
}
