package com.jinotrain.badforum.data;

import org.springframework.beans.factory.annotation.Value;

public class ServerData
{
    @Value("${badforum.port:8080}")
    private int port;

    public ServerData()
    {
        port = -1;
    }

    public int getPort() { return port; }
}
