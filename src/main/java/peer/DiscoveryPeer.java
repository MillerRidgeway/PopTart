package peer;

import message.DiscoverMessage;
import message.Message;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryPeer implements Peer {
    private ServerThread serverThread;
    private int port;
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public DiscoveryPeer(int port) throws IOException {
        ServerSocket s = new ServerSocket(port);
        serverThread = new ServerThread(this, s);
        this.port = port;

        System.out.println("Discovery peer listening for connections on: " + port);
        serverThread.start();
    }

    public int getServerPort() {
        return serverThread.getPort();
    }

    @Override
    public void parseMessage(Message msg) {
        if (msg instanceof DiscoverMessage) {
            DiscoverMessage dm = (DiscoverMessage) msg;
            System.out.println("Got discovery message.");
            System.out.println("Host addr: " + dm.getHost());
            System.out.println("Host port: " + dm.getHostPort());
            System.out.println("ID of peer: " + dm.getId());

        }
    }

    public static void main(String[] args) throws IOException {
        new DiscoveryPeer(Integer.parseInt(args[0]));
    }

    @Override
    public void addNewConnection(Connection c) {
        connectionMap.put(c.getAddr(), c);
    }
}
