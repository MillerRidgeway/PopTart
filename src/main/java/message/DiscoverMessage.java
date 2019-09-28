package message;

import java.io.*;

public class DiscoverMessage extends Message implements Serializable {
    private String id, host;
    private int hostPort;

    public DiscoverMessage(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.hostPort = port;
    }

    public String getId(){
        return this.id;
    }

    public String getHost(){
        return this.host;
    }

    public int getHostPort(){
        return this.hostPort;
    }
}
