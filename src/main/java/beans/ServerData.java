package beans;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.net.URISyntaxException;

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
