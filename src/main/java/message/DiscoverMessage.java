package message;

import java.io.*;

public class DiscoverMessage extends Message {
    private String id, host;
    private int hostPort, port;

    public DiscoverMessage(String id, String host, int hostPort, int port) {
        this.id = id;
        this.host = host;
        this.hostPort = hostPort;
        this.port = port;
    }

    public String getId() {
        return this.id;
    }

    public String getHost() {
        return this.host;
    }

    public int getHostPort() {
        return this.hostPort;
    }

    public int getPort() {
        return this.port;
    }
}
