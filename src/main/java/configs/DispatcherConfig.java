package configs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import servlets.ServletSearchDummy;

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = {ServletSearchDummy.class})
public class DispatcherConfig
{
}
