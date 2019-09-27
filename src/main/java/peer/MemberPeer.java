package peer;

import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemberPeer implements Peer {
    InetAddress dicoveryAddr;
    int port;
    Connection discoveryPeer;

    ServerThread serverThread;
    Map<String, Connection> connectionMap = new ConcurrentHashMap<>();


    public MemberPeer(InetAddress discoveryAddr, int port) throws IOException {
        this.dicoveryAddr = discoveryAddr;
        this.port = port;

        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("New member node listening for connections on: " + ss.getLocalPort());

        Socket s = new Socket(discoveryAddr, port);
        discoveryPeer = new Connection(this, s);

        discoveryPeer.sendMessage("testing message send");
    }

    public static void main(String[] args) throws Exception {
        InetAddress discoveryPeerAddr = InetAddress.getByName(args[0]);
        new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]));
    }

    public void addNewConnection(Connection c) {
        connectionMap.put(c.getAddr(), c);
    }
}
