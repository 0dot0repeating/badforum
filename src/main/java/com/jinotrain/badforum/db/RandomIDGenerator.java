package com.jinotrain.badforum.db;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

public class RandomIDGenerator implements IdentifierGenerator, Configurable
{
    private int idBytes = 32;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException
    {
        Random rnd = new SecureRandom();
        byte[] randBytes = new byte[idBytes];

        rnd.nextBytes(randBytes);

        return null;
    }


    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
    {
        idBytes = ConfigurationHelper.getInt("bytes", params, idBytes);
    }
}
