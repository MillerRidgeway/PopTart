package message;

import network.Connection;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class JoinAckMessage extends Message {
    private String randPeer;
    private int hostPort;

    public JoinAckMessage(String randPeer, int hostPort) {
        this.randPeer = randPeer;
        this.hostPort = hostPort;
    }

    public String getRandPeer() {
        return this.randPeer.split("_")[0];
    }

    public int getHostPort() {
        return this.hostPort;
    }
}
