package message;

import routing.LeafSet;

import java.io.Serializable;

public class JoinPeerMessage extends Message {
    private String id, addr;
    private int port, hostPort;
    private LeafSet leafSet;

    public JoinPeerMessage(String id, String addr, int port, int hostPort, LeafSet leafSet) {
        this.id = id;
        this.port = port;
        this.hostPort = hostPort;
        this.addr = addr;
        this.leafSet = leafSet;
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

    public int getHostPort() {
        return this.hostPort;
    }

    public LeafSet getLeafSet() {
        return this.leafSet;
    }
}
