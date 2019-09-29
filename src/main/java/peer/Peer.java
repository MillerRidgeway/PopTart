package peer;

import message.Message;
import network.Connection;

import java.io.IOException;
import java.net.Socket;

public interface Peer {
    void addNewConnection(Connection c);
    int getServerPort();
    void parseMessage(Message msg) throws IOException;
}
