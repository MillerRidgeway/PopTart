package peer;

import network.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;

public class DiscoveryPeer implements Peer {
    private ServerThread serverThread;
    private int port;

    public DiscoveryPeer(int port) throws IOException {
        ServerSocket s = new ServerSocket(port);
        serverThread = new ServerThread(this, s);
        this.port = port;

        System.out.println("Discovery peer listening for connections on: " + port);
        serverThread.start();
    }

    public int getServerPort() {
        return this.port;
    }

    public static void main(String[] args) throws IOException {
        new DiscoveryPeer(Integer.parseInt(args[0]));
    }
}
