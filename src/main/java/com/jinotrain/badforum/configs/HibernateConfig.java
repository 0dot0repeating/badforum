package com.jinotrain.badforum.configs;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ForumPropertySources
@EnableTransactionManagement
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
        this.dbURL      = dbURL;
        this.dbDriver   = dbDriver;
        this.dialect    = dialect;
        this.username   = username;
        this.password   = password;
        this.autoschema = autoschema;
    }

    // I tried to use the standard entity manager stuff, but as far as I can tell,
    //  it's completely bugged out for reasons I don't understand and aren't my fault
    @Bean
    public LocalSessionFactoryBean sessionFactory()
    {
        LocalSessionFactoryBean sf = new LocalSessionFactoryBean();
        sf.setDataSource(jpaDataSource());
        sf.setPackagesToScan("com.jinotrain.badforum.beans");
        sf.setHibernateProperties(hibernateProperties());

        return sf;
    }


    @Bean
    public DataSource jpaDataSource()
    {
        logger.info("Creating JPA data source for URL \"{}\"", dbURL);
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(dbDriver);
        dataSource.setUrl(dbURL);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }


    @Bean
    public PlatformTransactionManager transactionManager()
    {
        HibernateTransactionManager manager = new HibernateTransactionManager();
        manager.setSessionFactory(sessionFactory().getObject());
        return manager;
    }


    private Properties hibernateProperties()
    {
        Properties outProps = new Properties();
        outProps.setProperty("hibernate.dialect", dialect);
        outProps.setProperty("hibernate.hbm2ddl.auto", autoschema ? "update" : "create-only");
        return outProps;
    }
}
