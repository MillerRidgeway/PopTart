package message;

import java.io.Serializable;

public class JoinPeerMessage extends Message implements Serializable {
    private String id, addr;
    private int port;

    public JoinPeerMessage(String id, String addr, int port) {
        this.id = id;
        this.port = port;
        this.addr = addr;
    }

    public String getId() {
        return this.id;
    }

    public String getAddr() {
        return this.addr;
    }

    public int getPort() {
        return this.port;
    }
}
