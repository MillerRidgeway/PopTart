package message;

import routing.LeafSet;

import java.io.Serializable;

public class JoinPeerMessage extends Message {
    private String id, addr;
    private int port;
    private LeafSet leafSet;

    public JoinPeerMessage(String id, String addr, int port, LeafSet leafSet) {
        this.id = id;
        this.port = port;
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

    public LeafSet getLeafSet() {
        return this.leafSet;
    }
}
