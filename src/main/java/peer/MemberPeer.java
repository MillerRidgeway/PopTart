package peer;

import message.*;
import network.Connection;
import network.ServerThread;
import routing.LeafSet;
import routing.RoutingTable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private Map<String, Connection> ipConnectionMap = new ConcurrentHashMap<>();
    public LeafSet leafSet;
    private RoutingTable routingTable;
    private static final Logger logger = Logger.getLogger(DiscoveryPeer.class.getName());
    private static FileHandler fh;


    public MemberPeer(InetAddress discoveryAddr, int discoveryPort, String id) throws IOException {
        this.dicoveryAddr = discoveryAddr;
        this.discoveryPort = discoveryPort;
        if (id == null)
            this.id = getTimestampId();
        else
            this.id = id;
        leafSet = new LeafSet("", "", "", "");

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

        //startConsole();
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
        if (msg instanceof RediscoverMessage) { //Collision within the ID space
            logger.log(Level.FINE, "Got rediscover, trying again.");
            parseRediscoverMessage((RediscoverMessage) msg);
        } else if (msg instanceof FirstConnectAckMessage) { //First connected peer
            logger.log(Level.FINE, "Got first connect ack");
            parseFirstConnectAckMessage((FirstConnectAckMessage) msg);
        } else if (msg instanceof JoinAckMessage) { //Acknowledge join request, attempt to contact given peer
            logger.log(Level.FINE, "Got join ack.");
            parseJoinAckMessage((JoinAckMessage) msg);
        } else if (msg instanceof JoinPeerMessage) { // Being contacted by another peer to join
            logger.log(Level.FINE, "Got peer join.");
            parseJoinPeerMessage((JoinPeerMessage) msg);
        } else if (msg instanceof JoinPeerAckMessage) { //Got response for join req, forward or stop if closest.
            logger.log(Level.FINE, "Got forward/response to join.");
            parseJoinPeerAckMessage((JoinPeerAckMessage) msg);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }


    public void addNewConnection(Connection c) {
        ipConnectionMap.put(c.getAddr() + "_" + c.getPort(), c);
    }

    private String getTimestampId() {
        long timeStamp = System.currentTimeMillis();
        String hex = Long.toHexString(timeStamp);
        return hex.substring(hex.length() - 4);
    }

    private void parseRediscoverMessage(RediscoverMessage msg) throws IOException {
        logger.log(Level.FINE, "Got rediscover (ID collision), sending with new ID");

        String randId = getTimestampId();
        this.id = randId;
        DiscoverMessage dm = new DiscoverMessage(randId, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());
        discoveryConnection.sendMessage(dm);
    }

    private void parseFirstConnectAckMessage(FirstConnectAckMessage msg) {
        logger.log(Level.FINE, "First connect acknowledged by discovery node...waiting");
    }

    private void parseJoinAckMessage(JoinAckMessage msg) throws IOException {
        logger.log(Level.FINE, "JoinPeer ack, opening connection to new peer");
        logger.log(Level.FINE, "Connecting to IP: " + msg.getRandPeer());
        logger.log(Level.FINE, "Connecting to port: " + msg.getHostPort());

        Socket s = new Socket(InetAddress.getByName(msg.getRandPeer()), msg.getHostPort());
        Connection c = new Connection(this, s);
        c.sendMessage(new JoinPeerMessage(this.id, c.getLocalAddr(), c.getLocalPort()));
    }

    private void parseJoinPeerMessage(JoinPeerMessage msg) throws IOException {
        logger.log(Level.FINE, "Got join request from peer w/ ID: " + msg.getId());
        logger.log(Level.FINE, "Join request is from: " + msg.getAddr());
        logger.log(Level.FINE, "Join request is from port: " + msg.getPort());

        Connection c = ipConnectionMap.get(msg.getAddr() + "_" + msg.getPort());


        if (leafSet.isEmpty()) {
            this.leafSet.setHi(msg.getId(), msg.getAddr() + "_" + msg.getPort());
            this.leafSet.setLo(msg.getId(), msg.getAddr() + "_" + msg.getPort());
            c.sendMessage(new JoinPeerAckMessage());
        }

        if (leafSet.contains(msg.getId())) {
            c.sendMessage(new DestinationFoundMessage());
        }
        //routingTable.findClosestIp(routingTable.findClosest(msg.getId()));


    }

    private void parseJoinPeerAckMessage(JoinPeerAckMessage msg) {
    }

}




