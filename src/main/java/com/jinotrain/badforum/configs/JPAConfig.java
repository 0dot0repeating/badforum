package com.jinotrain.badforum.configs;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ForumPropertySources
@EnableTransactionManagement
public class JPAConfig
{
    private static Logger logger = LoggerFactory.getLogger(JPAConfig.class);

    @Autowired
    private ApplicationContext context;

    private String  dbURL;
    private String  dbDriver;
    private String  dialect;
    private String  username;
    private String  password;
    private Boolean autoschema;

    @Autowired
    public JPAConfig(@Value("${badforum.db.url}")        String dbURL,
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

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory()
    {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(jpaDataSource());
        em.setPackagesToScan("com.jinotrain.badforum.db.entities");

        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(jpaProperties());

        return em;
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
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf)
    {
        JpaTransactionManager manager = new JpaTransactionManager();
        manager.setEntityManagerFactory(emf);
        return manager;
    }


    private Properties jpaProperties()
    {
        Properties outProps = new Properties();
        outProps.setProperty("hibernate.hbm2ddl.auto", autoschema ? "update" : "create-only");
        outProps.setProperty("hibernate.dialect", dialect);
        return outProps;
    }
}
