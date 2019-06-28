package com.jinotrain.badforum.configs;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ForumPropertySources
public class HibernateConfig
{
    private static Logger logger = LoggerFactory.getLogger(HibernateConfig.class);

    @Autowired
    private ApplicationContext context;

    private String  dbURL;
    private String  dbDriver;
    private String  dialect;
    private String  username;
    private String  password;
    private Boolean autoschema;

    @Autowired
    public HibernateConfig(@Value("${badforum.db.url}")        String dbURL,
                           @Value("${badforum.db.driver}")     String dbDriver,
                           @Value("${badforum.db.dialect}")    String dialect,
                           @Value("${badforum.db.username}")   String username,
                           @Value("${badforum.db.password}")   String password,
                           @Value("${badforum.db.autoschema}") Boolean autoschema)
    {
        logger.debug("!@#$% yes the hibernate config loaded %$#@!");

        this.dbURL      = dbURL;
        this.dbDriver   = dbDriver;
        this.dialect    = dialect;
        this.username   = username;
        this.password   = password;
        this.autoschema = autoschema;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() throws IOException
    {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(jpaDataSource());
        sessionFactory.setPackagesToScan("com.jinotrain.badforum.beans.db");
        sessionFactory.setHibernateProperties(hibernateProperties());

        return sessionFactory;
    }


    @Bean
    public DataSource jpaDataSource()
    {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(dbDriver);
        dataSource.setUrl(dbURL);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }


    private Properties hibernateProperties() throws IOException
    {
        Properties outProps = new Properties();

        outProps.setProperty("hibernate.hbm2ddl.auto", autoschema ? "update" : "create-only");
        outProps.setProperty("hibernate.dialect", dialect);

        return outProps;
    }
}
