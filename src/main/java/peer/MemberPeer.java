package peer;

import com.sun.security.ntlm.Server;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class MemberPeer implements Peer {
    InetAddress dicoveryAddr;
    int port;
    ServerThread serverThread;
    Connection discoveryPeer;

    public MemberPeer(InetAddress discoveryAddr, int port) throws IOException {
        this.dicoveryAddr = discoveryAddr;
        this.port = port;

        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("New member node listening for connections on: " + ss.getLocalPort());

        Socket s = new Socket(discoveryAddr, port);
        discoveryPeer = new Connection(this, s);
    }

    public static void main(String[] args) throws Exception {
        InetAddress discoveryPeerAddr = InetAddress.getByName(args[0]);
        new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]));
    }
}
