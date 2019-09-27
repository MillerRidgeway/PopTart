package peer;

import network.Connection;

import java.net.Socket;

public interface Peer {
    void addNewConnection(Connection c);
}
