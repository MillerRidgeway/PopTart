package peer;

import message.*;
import network.Connection;
import network.ServerThread;
import routing.LeafSet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class MemberPeer implements Peer {
    private InetAddress dicoveryAddr;
    private int discoveryPort;
    private Connection discoveryConnection;

    private ServerThread serverThread;
    private String id;
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private LeafSet leafSet;
    private static final Logger logger = Logger.getLogger(DiscoveryPeer.class.getName());
    private static FileHandler fh;


    public MemberPeer(InetAddress discoveryAddr, int discoveryPort, String id) throws IOException {
        this.dicoveryAddr = discoveryAddr;
        this.discoveryPort = discoveryPort;
        if (id == null)
            id = getTimestampId();
        else
            this.id = id;
        leafSet = null;

        //Server thread
        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("New member node listening for connections on: " + ss.getLocalPort());

        //Discovery peer connection
        Socket s = new Socket(discoveryAddr, discoveryPort);
        discoveryConnection = new Connection(this, s);
        DiscoverMessage dm = new DiscoverMessage(id, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());
        discoveryConnection.sendMessage(dm);

        //Logger
        fh = new FileHandler("memberPeer" + "_" + ss.getLocalPort() + ".log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINE);

        startConsole();
    }

    public static void main(String[] args) throws Exception {
        InetAddress discoveryPeerAddr = InetAddress.getByName(args[0]);
        if (args.length == 2)
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), null);
        else {
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), args[2]);
        }
    }

    private void startConsole() {
        Scanner scn = new Scanner(System.in);
        System.out.println("Please enter a command: ");
        String command = scn.nextLine();
        System.out.println("The command was: " + command);
    }

    @Override
    public void parseMessage(Message msg) throws IOException {
        if (msg instanceof RediscoverMessage) {
            logger.log(Level.FINE, "Got rediscover, trying again.");
            parseRediscoverMessage((RediscoverMessage) msg);
        } else if (msg instanceof FirstConnectAckMessage) {
            logger.log(Level.FINE, "Got first connect ack");
            parseFirstConnectAckMessage((FirstConnectAckMessage) msg);
        } else if (msg instanceof JoinAckMessage) {
            logger.log(Level.FINE, "Got join ack.");
            parseJoinAckMessage((JoinAckMessage) msg);
        }
    }


    public void addNewConnection(Connection c) {
        connectionMap.put(c.getAddr(), c);
    }

    private String getTimestampId() throws IOException {
        long timeStamp = System.currentTimeMillis();
        String hex = Long.toHexString(timeStamp);
        return hex.substring(hex.length() - 4);
    }

    private void parseRediscoverMessage(RediscoverMessage msg) throws IOException {
        String randId = getTimestampId();
        DiscoverMessage dm = new DiscoverMessage(randId, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());
        discoveryConnection.sendMessage(dm);
    }

    private void parseFirstConnectAckMessage(FirstConnectAckMessage msg) {
        logger.log(Level.FINE, "First connect: Setting leafset to itself");
        leafSet = new LeafSet(this, this);
        logger.log(Level.FINE, "New leafset is: " + leafSet.toString());
    }

    private void parseJoinAckMessage(JoinAckMessage msg) {
        logger.log(Level.FINE, "Join request recieved, opening connection to new peer");
        logger.log(Level.FINE, "Peer IP: " + msg.getRandPeer());
        logger.log(Level.FINE, "Peer host port: " + msg.getHostPort());
    }

}




