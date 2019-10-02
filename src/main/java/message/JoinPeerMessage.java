package message;

import java.io.Serializable;

public class JoinPeerMessage extends Message implements Serializable {
    private String id;
    private int hostPort;

    public JoinPeerMessage(String id, int hostPort) {
        this.id = id;
        this.hostPort = hostPort;
    }

    public String getId() {
        return this.id;
    }

    public int getHostPort() {
        return this.hostPort;
    }
}
