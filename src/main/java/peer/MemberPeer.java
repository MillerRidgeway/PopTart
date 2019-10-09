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
    private LeafSet leafSet;
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

        //Logger
        fh = new FileHandler("memberPeer" + "_" + this.id + ".log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINER);

        //Server thread
        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("New member node listening for connections on: " + ss.getLocalPort());

        //Discovery peer connection
        Socket s = new Socket(discoveryAddr, discoveryPort);
        discoveryConnection = new Connection(this, s);
        DiscoverMessage dm = new DiscoverMessage(this.id, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort());
        discoveryConnection.sendMessage(dm);

        //Routing table
        routingTable = new RoutingTable(this);

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
                case "routing-table":
                    System.out.println("Table is: \n" + routingTable);
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        }
    }

    public String getConnectionInfo() {
        return discoveryConnection.getLocalAddr() + "_" + serverThread.getPort();
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
        } else if (msg instanceof ForwardToMessage) {
            logger.log(Level.FINE, "Got response to forward");
            parseForwardToMessage((ForwardToMessage) msg);
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

        Connection joiningPeerConnection = ipConnectionMap.get(msg.getAddr() + "_" + msg.getPort());

        int rowIndex = Util.getIdDifference(this.getId(), msg.getId());
        String closestId = routingTable.findClosest(msg.getId());
        String closestIp = routingTable.findClosestIp(closestId);

        routingTable.insertNewPeer(msg.getId(), msg.getAddr(), msg.getHostPort());

        if (leafSet.isEmpty()) { // Second node in the system
            insertNewPeer(msg);
            joiningPeerConnection.sendMessage(new ForwardToMessage("", "", rowIndex,
                    routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
        } else if (closestId.equals(this.id)) { //Arrived at closest node, check leafs
            logger.log(Level.FINE, "Closest ID in my table is me.");
            int hiMatch = Util.getIdDifference(leafSet.getHi(), msg.getId());
            int loMatch = Util.getIdDifference(leafSet.getLo(), msg.getId());
            int myMatch = Util.getIdDifference(this.getId(), msg.getId());

            logger.log(Level.FINER, "My diff is: " + myMatch);
            logger.log(Level.FINER, "logDiff is: " + loMatch);
            logger.log(Level.FINER, "hiDiff is: " + hiMatch);

            if (loMatch > myMatch) {
                logger.log(Level.FINE, "My low leaf is closer at: "
                        + leafSet.getFullLo() + " " + leafSet.getFullLo());
                logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                joiningPeerConnection.sendMessage(new ForwardToMessage(leafSet.getLo(), leafSet.getFullLo(), rowIndex,
                        routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
            } else if (hiMatch > myMatch) {
                logger.log(Level.FINE, "My hi leaf is closer at: " + leafSet.getFullHi());
                logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                joiningPeerConnection.sendMessage(new ForwardToMessage(leafSet.getHi(), leafSet.getFullHi(), rowIndex,
                        routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
            } else if (loMatch == myMatch) {
                int loDiff = Util.getDigitDifference(leafSet.getLo(), msg.getId(), loMatch);
                int myDiff = Util.getDigitDifference(this.id, msg.getId(), myMatch);

                if (loDiff < myDiff) {
                    logger.log(Level.FINE, "My low leaf is closer at: "
                            + leafSet.getFullLo() + " " + leafSet.getFullLo());
                    logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                    joiningPeerConnection.sendMessage(new ForwardToMessage(leafSet.getLo(), leafSet.getFullLo(), rowIndex,
                            routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
                } else {
                    logger.log(Level.FINE, "Destination reached: " + this.id);
                    logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                    insertNewPeer(msg);
                    joiningPeerConnection.sendMessage(new ForwardToMessage("", "", rowIndex,
                            routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
                }
            } else {
                logger.log(Level.FINE, "Destination reached: " + this.id);
                logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                insertNewPeer(msg);
                joiningPeerConnection.sendMessage(new ForwardToMessage("", "", rowIndex,
                        routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
            }

        } else { //Route by DHT
            logger.log(Level.FINE, "My routing table has a closer peer: " + closestId + " " + closestIp);
            logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
            joiningPeerConnection.sendMessage(new ForwardToMessage(closestId, closestIp, rowIndex,
                    routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
        }
    }

    private synchronized void parseForwardToMessage(ForwardToMessage msg) throws IOException {
        logger.log(Level.FINE, "Adding row from previous peer - row num is: " + msg.getRowIndex());
        logger.log(Level.FINE, "Row contents: " + msg.getTableRow());

        routingTable.setRow(msg.getRowIndex(), msg.getTableRow());
        routingTable.putIps(msg.getTableRow(), msg.getIps());

        if (!msg.getDestIp().isEmpty()) {
            logger.log(Level.FINE, "There is a closer peer at: " + msg.getDestIp());
            logger.log(Level.FINE, "I am connecting to port: " + msg.getDestHostPort());
            Socket s = new Socket(InetAddress.getByName(msg.getDestIp()), msg.getDestHostPort());
            Connection c = new Connection(this, s);
            c.sendMessage(new JoinPeerMessage(this.id, c.getLocalAddr(), c.getLocalPort(),
                    serverThread.getPort(), this.leafSet));
        } else {
            logger.log(Level.FINE, "Reached final destination.");
        }
    }

    private void parseUpdateLeafSetMessage(UpdateLeafSetMessage msg) {
        logger.log(Level.FINE, "Changing leaf set to reflect updated version");

        if (msg.getResponseLeaf().getHi().equals("current"))
            this.leafSet.setLo(msg.getResponseLeaf().getLo(), msg.getResponseLeaf().getLoAddr());
        else if (msg.getResponseLeaf().getLo().equals("current"))
            this.leafSet.setHi(msg.getResponseLeaf().getHi(), msg.getResponseLeaf().getHiAddr());
        else
            this.leafSet = msg.getResponseLeaf();
    }

    private synchronized void insertNewPeer(JoinPeerMessage msg) throws IOException {
        Connection joiningPeerConnection = ipConnectionMap.get(msg.getAddr() + "_" + msg.getPort());

        LeafSet newSet;
        String msgAddrPort = msg.getAddr() + "_" + msg.getHostPort() + "_" + msg.getPort();
        String thisAddrPort = joiningPeerConnection.getLocalAddr() + "_" + serverThread.getPort()
                + "_" + joiningPeerConnection.getLocalPort();
        if (leafSet.isEmpty()) {
            newSet = new LeafSet(this.id, this.id, thisAddrPort, thisAddrPort);
            joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));

            leafSet.setHi(msg.getId(), msgAddrPort);
            leafSet.setLo(msg.getId(), msgAddrPort);
            logger.log(Level.FINE, "New leafset is:\n" + leafSet);
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
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiHostPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                } else { //"this" is high node
                    newSet = new LeafSet(leafSet.getLo(),
                            this.id,
                            leafSet.getFullLo(),
                            thisAddrPort);

                    otherLeafSet = new LeafSet("current", msg.getId(), "current", msg.getAddr());
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoHostPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setHi(msg.getId(), msgAddrPort);
                }
                joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));

            } else if (Util.getNumericalDifference(this.id, msg.getId()) < 0
                    && Util.getNumericalDifference(leafSet.getHi(), msg.getId()) < 0) { //Incoming is high

                if (Util.getNumericalDifference(this.id, leafSet.getHi()) < 0) { //"this" is new lo
                    newSet = new LeafSet(this.id,
                            leafSet.getHi(),
                            thisAddrPort,
                            leafSet.getFullHi());

                    otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiHostPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                } else {//"this" is high node
                    newSet = new LeafSet(leafSet.getLo(),
                            this.id,
                            leafSet.getFullLo(),
                            thisAddrPort);

                    otherLeafSet = new LeafSet("current", msg.getId(), "current", msgAddrPort);
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoHostPort());
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
                    Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiHostPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setHi(msg.getId(), msg.getAddr());
                } else {
                    newSet = new LeafSet(this.id,
                            leafSet.getLo(),
                            thisAddrPort,
                            leafSet.getFullLo());

                    otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");
                    Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoHostPort());
                    Connection otherConnection = new Connection(this, s);
                    otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                    leafSet.setLo(msg.getId(), msgAddrPort);
                }
                joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));
            }
        }
    }

}