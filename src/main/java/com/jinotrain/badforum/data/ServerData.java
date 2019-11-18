package com.jinotrain.badforum.data;

import org.springframework.beans.factory.annotation.Value;

public class ServerData
{
    @Value("${badforum.port:8080}")
    private int port;

    @Value("${badforum.ssl.port:4443}")
    private int sslPort;

    @Value("${badforum.ssl.alias:badforum}")
    private String keyAlias;

    @Value("${badforum.ssl.password:}")
    private String sslPassword;

    public ServerData()
    {
        port        = -1;
        sslPort     = -1;
        keyAlias    = "";
        sslPassword = "";
    }

    public int    getPort()        { return port; }
    public int    getSSLPort()     { return sslPort; }
    public String getSSLPassword() { return sslPassword; }
    public String getKeyAlias()    { return keyAlias; }
}
