package peer;

import message.Message;
import network.Connection;

import java.net.Socket;

public interface Peer {
    void addNewConnection(Connection c);
    int getServerPort();
    void parseMessage(Message msg);
}
