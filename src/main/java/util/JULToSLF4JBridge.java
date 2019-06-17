package util;

import org.apache.juli.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JULToSLF4JBridge implements Log
{
    private final Logger logger;

    public JULToSLF4JBridge()
    {
        logger = null;
    }

    public JULToSLF4JBridge(String name)
    {
        System.err.println("this is getting used, right");
        logger = LoggerFactory.getLogger(name);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }


    @Override
    public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }


    @Override
    public boolean isFatalEnabled()
    {
        return logger.isErrorEnabled();
    }


    @Override
    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }


    @Override
    public boolean isTraceEnabled()
    {
        return logger.isTraceEnabled();
    }


    @Override
    public boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }


    @Override
    public void trace(Object message)
    {
        logger.debug(String.valueOf(message));
    }


    @Override
    public void trace(Object message, Throwable t)
    {
        logger.debug(String.valueOf(message), t);
    }


    @Override
    public void debug(Object message)
    {
        logger.debug(String.valueOf(message));
    }


    @Override
    public void debug(Object message, Throwable t)
    {
        logger.debug(String.valueOf(message), t);
    }


    @Override
    public void info(Object message)
    {
        logger.info(String.valueOf(message));
    }


    @Override
    public void info(Object message, Throwable t)
    {
        logger.info(String.valueOf(message), t);
    }


    @Override
    public void warn(Object message)
    {
        logger.warn(String.valueOf(message));
    }


    @Override
    public void warn(Object message, Throwable t)
    {
        logger.warn(String.valueOf(message), t);
    }


    @Override
    public void error(Object message)
    {
        logger.error(String.valueOf(message));
    }


    @Override
    public void error(Object message, Throwable t)
    {
        logger.error(String.valueOf(message), t);
    }


    @Override
    public void fatal(Object message)
    {
        logger.error(String.valueOf(message));
    }


    @Override
    public void fatal(Object message, Throwable t)
    {
        logger.error(String.valueOf(message), t);
    }
}
