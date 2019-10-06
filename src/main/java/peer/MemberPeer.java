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
        System.out.println("My node id is: " + this.id);

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
        while (true) {
            System.out.println("Please enter a command: ");
            String command = scn.nextLine();
            switch (command) {
                case "list-nodes":
                    for (Map.Entry<String, Connection> e : ipConnectionMap.entrySet()) {
                        System.out.println("Connected node: " + e.getValue().toString());
                    }
                    break;
                case "leaf-set":
                    System.out.println("Leafset is: " + leafSet);
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        }
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
        } else if (msg instanceof UpdateLeafSetMessage) {
            logger.log(Level.FINE, "Got forward request/response to join from existing peer.");
            parseUpdateLeafSetMessage((UpdateLeafSetMessage) msg);
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
        System.out.println("My new ID is: " + this.id);
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
        c.sendMessage(new JoinPeerMessage(this.id, c.getLocalAddr(), c.getLocalPort(),
                serverThread.getPort(), this.leafSet));
    }

    private synchronized void parseJoinPeerMessage(JoinPeerMessage msg) throws IOException {
        logger.log(Level.FINE, "Got join request from peer w/ ID: " + msg.getId());
        logger.log(Level.FINE, "Join request is from: " + msg.getAddr());
        logger.log(Level.FINE, "Join request is from port: " + msg.getPort());
        logger.log(Level.FINE, "Host port of joining peer is: " + msg.getHostPort());
        logger.log(Level.FINE, "Leafset of joining peer is currently: " + msg.getLeafSet());

        Connection joiningPeerConnection = ipConnectionMap.get(msg.getAddr() + "_" + msg.getPort());

        LeafSet newSet = null;
        String msgAddrPort = msg.getAddr() + "_" + msg.getHostPort();
        String thisAddrPort = joiningPeerConnection.getLocalAddr() + "_" + serverThread.getPort();
        if (leafSet.isEmpty()) {
            String addrPort = joiningPeerConnection.getLocalAddr() + "_" + joiningPeerConnection.getLocalPort();
            logger.log(Level.FINE, "New leafset is:\n" + leafSet);
            newSet = new LeafSet(this.id, this.id, addrPort, addrPort);
            joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));

            leafSet.setHi(msg.getId(), msgAddrPort);
            leafSet.setLo(msg.getId(), msgAddrPort);
        } else {
            LeafSet otherLeafSet = null;
            if (Util.getNumericalDifference(this.id, msg.getId()) > 0
                    && Util.getNumericalDifference(leafSet.getLo(), msg.getId()) > 0) { //Incoming is low

                if (Util.getNumericalDifference(this.id, leafSet.getHi()) < 0) { //"this" is middle node - wrapped
                    newSet = new LeafSet(this.id,
                            leafSet.getHi(),
                            thisAddrPort,
                            leafSet.getFullHi());

                    otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                } else { //"this" is high node
                    newSet = new LeafSet(leafSet.getLo(),
                            this.id,
                            leafSet.getFullLo(),
                            thisAddrPort);

                    otherLeafSet = new LeafSet("current", msg.getId(), "current", msg.getAddr());
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setHi(msg.getId(), msgAddrPort);
                }
                joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));

            } else if (Util.getNumericalDifference(this.id, msg.getId()) < 0
                    && Util.getNumericalDifference(leafSet.getHi(), msg.getId()) < 0) { //Incoming is high

                if (Util.getNumericalDifference(this.id, leafSet.getHi()) < 0) { //"this" is new lo - wrapped
                    newSet = new LeafSet(this.id,
                            leafSet.getHi(),
                            thisAddrPort,
                            leafSet.getFullHi());

                    otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                } else {//"this" is high node
                    newSet = new LeafSet(leafSet.getLo(),
                            this.id,
                            leafSet.getFullLo(),
                            thisAddrPort);

                    otherLeafSet = new LeafSet("current", msg.getId(), "current", msgAddrPort);
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setHi(msg.getId(), msgAddrPort);
                }
                joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));
            } else { //Incoming is middle
                if (Util.getNumericalDifference(this.id, msg.getId()) < 0) {
                    newSet = new LeafSet(leafSet.getHi(),
                            this.id,
                            leafSet.getFullHi(),
                            thisAddrPort);

                    otherLeafSet = new LeafSet("current", msg.getId(), "current", msgAddrPort);
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setHi(msg.getId(), msg.getAddr());
                } else {
                    newSet = new LeafSet(this.id,
                            leafSet.getLo(),
                            thisAddrPort,
                            leafSet.getFullLo());

                    otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                }
                joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));
            }
        }
    }

    private void parseUpdateLeafSetMessage(UpdateLeafSetMessage msg) {
        logger.log(Level.FINE, "New leafset is:\n" + msg.getResponseLeaf());
        logger.log(Level.FINE, "Changing leaf set to reflect updated version");

        if (msg.getResponseLeaf().getHi().equals("current"))
            this.leafSet.setLo(msg.getResponseLeaf().getLo(), msg.getResponseLeaf().getLoAddr());
        else if (msg.getResponseLeaf().getLo().equals("current"))
            this.leafSet.setHi(msg.getResponseLeaf().getHi(), msg.getResponseLeaf().getHiAddr());
        else
            this.leafSet = msg.getResponseLeaf();

        logger.log(Level.FINE, "New leafset is:\n" + leafSet);
    }

}




