package peer;

import message.DiscoverMessage;
import message.Message;
import message.RediscoverMessage;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class MemberPeer implements Peer {
    private InetAddress dicoveryAddr;
    private int discoveryPort;

    Connection discoveryConnection;
    ServerThread serverThread;
    String id;
    Map<String, Connection> connectionMap = new ConcurrentHashMap<>();


    public MemberPeer(InetAddress discoveryAddr, int discoveryPort, String id) throws IOException {
        this.dicoveryAddr = discoveryAddr;
        this.discoveryPort = discoveryPort;
        if (id == null)
            id = getTimestampId();
        else
            this.id = id;

        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("New member node listening for connections on: " + ss.getLocalPort());

        Socket s = new Socket(discoveryAddr, discoveryPort);
        discoveryConnection = new Connection(this, s);

        DiscoverMessage dm = new DiscoverMessage(id, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());

        discoveryConnection.sendMessage(dm);
        run();
    }

    public static void main(String[] args) throws Exception {
        InetAddress discoveryPeerAddr = InetAddress.getByName(args[0]);
        if (args.length == 2)
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), null);
        else {
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), args[2]);
        }

    }

    public void run() {
        Scanner scn = new Scanner(System.in);
        System.out.print("Please enter a command: ");
        String command = scn.nextLine();
    }

    @Override
    public void parseMessage(Message msg) throws IOException {
        if (msg instanceof RediscoverMessage) {
            System.out.println("Got rediscover, trying again.");
            parseRediscoverMessage((RediscoverMessage) msg);
        }
    }

    public int getServerPort() {
        return serverThread.getPort();
    }


    public void addNewConnection(Connection c) {
        connectionMap.put(c.getAddr(), c);
    }

    private String getTimestampId() throws IOException {
        long timeStamp = System.currentTimeMillis();
        String hex = Long.toHexString(timeStamp);
        return hex.substring(hex.length() - 4);
    }

    public void parseRediscoverMessage(RediscoverMessage msg) throws IOException {
        String randId = getTimestampId();
        DiscoverMessage dm = new DiscoverMessage(randId, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());
        discoveryConnection.sendMessage(dm);
    }
}




