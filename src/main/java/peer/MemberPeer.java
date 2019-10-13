package peer;

import datastore.DataStore;
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    private LeafSet leafSet;
    private RoutingTable routingTable;
    private DataStore dataStore;
    private Map<String, Connection> ipConnectionMap = new ConcurrentHashMap<>();
    private ArrayList<String> travelRoute;
    private static final Logger logger = Logger.getLogger(MemberPeer.class.getName());
    private static FileHandler fh;


    public MemberPeer(InetAddress discoveryAddr, int discoveryPort, String storageDir, String id) throws Exception {
        this.dicoveryAddr = discoveryAddr;
        this.discoveryPort = discoveryPort;
        if (id == null)
            this.id = Util.getTimestampId();
        else
            this.id = id;
        leafSet = new LeafSet("", "", "", "");
        travelRoute = new ArrayList<>();
        dataStore = new DataStore(storageDir);
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
                discoveryConnection.getLocalPort(), false);
        discoveryConnection.sendMessage(dm);

        //Routing table
        routingTable = new RoutingTable(this);

        startConsole();
    }

    public static void main(String[] args) throws Exception {
        InetAddress discoveryPeerAddr = InetAddress.getByName(args[0]);
        if (args.length == 3)
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), null, args[2]);
        else {
            new MemberPeer(discoveryPeerAddr, Integer.parseInt(args[1]), args[2], args[3]);
        }
    }

    public void startConsole() {
        Scanner scn = new Scanner(System.in);
        while (true) {
            System.out.println("Please enter a command: ");
            String command = scn.nextLine();
            switch (command.toLowerCase()) {
                case "list-nodes":
                    for (Map.Entry<String, Connection> e : ipConnectionMap.entrySet()) {
                        System.out.println("Connected node: " + e.getValue().toString());
                    }
                    break;
                case "leaf-set":
                    System.out.println("Leafset is: \n" + leafSet);
                    break;
                case "routing-table":
                    System.out.println("Table is: \n" + routingTable);
                    break;
                case "pathway":
                    System.out.println("Took the path: " + this.travelRoute);
                    break;
                case "exit":
                    System.out.println("Exiting the overlay now.");
                    try {
                        exitOverlay();
                    } catch (IOException e) {
                        System.out.println("Error exiting overlay.");
                        e.printStackTrace();
                    }
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
    public void parseMessage(Message msg) throws IOException, NoSuchAlgorithmException {
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
            logger.log(Level.FINE, "Got response to forward request");
            parseForwardToMessage((ForwardToMessage) msg);
        } else if (msg instanceof UpdateLeafSetMessage) {
            logger.log(Level.FINE, "Got update leafset message");
            parseUpdateLeafSetMessage((UpdateLeafSetMessage) msg);
        } else if (msg instanceof FileStoreMessage) {
            logger.log(Level.FINE, "Got file store request, storing file here.");
            parseFileStoreMessage((FileStoreMessage) msg);
        } else if (msg instanceof ExitOverlayMessage) {
            logger.log(Level.FINE, "Peer sending exiting request.");
            parseExitOverlayMessage((ExitOverlayMessage) msg);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    public void addNewConnection(Connection c) {
        ipConnectionMap.put(c.getAddr() + "_" + c.getPort(), c);
    }

    private void parseRediscoverMessage(RediscoverMessage msg) throws IOException, NoSuchAlgorithmException {
        logger.log(Level.FINE, "Got rediscover (ID collision), sending with new ID");

        String randId = Util.getTimestampId();
        this.id = randId;
        System.out.println("My new ID is: " + this.id);
        DiscoverMessage dm = new DiscoverMessage(randId, discoveryConnection.getAddr(), serverThread.getPort(),
                discoveryConnection.getLocalPort(), false);
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

        int rowIndex = Util.getIdMatchingDigits(this.getId(), msg.getId());
        String closestByTable = routingTable.findClosest(msg.getId());
        int msgAndMeDiff = Math.abs(Util.getNumericalDifference(this.id, msg.getId()));
        int tableAndMsgDiff = Math.abs(Util.getNumericalDifference(closestByTable, msg.getId()));

        String closestId;
        if (msgAndMeDiff < tableAndMsgDiff)
            closestId = this.id;
        else
            closestId = routingTable.findClosest(msg.getId());
        String closestIp = routingTable.findClosestIp(closestId);

        String pitstop = this.id + "_"
                + joiningPeerConnection.getLocalAddr()
                + "_"
                + joiningPeerConnection.getLocalPort();

        if (msg.getLeafSet() != null)
            routingTable.insertNewPeer(msg.getId(), msg.getAddr(), msg.getHostPort());

        if (leafSet.isEmpty()) { // Second node in the system
            insertNewLeaf(msg);
            joiningPeerConnection.sendMessage(new ForwardToMessage(pitstop, "", "", rowIndex,
                    routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
        } else if (closestId.equals(this.id)) { //Arrived at closest node, check leafs
            logger.log(Level.FINE, "Closest ID in my table is me.");

            int loDiff = Math.abs(Util.getNumericalDifference(leafSet.getLo(), msg.getId()));
            int myDiff = Math.abs(Util.getNumericalDifference(this.id, msg.getId()));
            int hiDiff = Math.abs(Util.getNumericalDifference(leafSet.getHi(), msg.getId()));
            logger.log(Level.FINER, "My ID diff is: " + myDiff);
            logger.log(Level.FINER, "Lo ID diff is: " + loDiff);
            logger.log(Level.FINER, "Hi ID diff is: " + hiDiff);

            if (loDiff < myDiff && loDiff <= hiDiff) { //Lo node is closer
                sendToLoLeaf(joiningPeerConnection, rowIndex);
            } else if (hiDiff < myDiff) { //Hi node is closer
                sendToHiLeaf(joiningPeerConnection, rowIndex);
            } else { //I am the closest node
                logger.log(Level.FINE, "Destination reached: " + this.id);
                logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
                insertNewLeaf(msg);
                joiningPeerConnection.sendMessage(new ForwardToMessage(pitstop, "", "", rowIndex,
                        routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
            }

        } else { //Route by DHT
            logger.log(Level.FINE, "My routing table has a closer peer: " + closestId + " " + closestIp);
            logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
            joiningPeerConnection.sendMessage(new ForwardToMessage(pitstop, closestId, closestIp, rowIndex,
                    routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
        }
    }

    private synchronized void parseForwardToMessage(ForwardToMessage msg) throws IOException {
        logger.log(Level.FINE, "Adding row from " + msg.getPitstop() + " - row num is: " + msg.getRowIndex());
        logger.log(Level.FINE, "Row contents: " + msg.getTableRow());

        travelRoute.add(msg.getPitstop().split("_")[0]);
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

    private void parseFileStoreMessage(FileStoreMessage msg) throws IOException {
        logger.log(Level.FINE, "Writing file " + msg.getFile().getName());
        logger.log(Level.FINE, "File ID is: " + msg.getFileId());
        dataStore.writeFile(msg.getFile().getName(), msg.getContents());
    }

    private void parseUpdateLeafSetMessage(UpdateLeafSetMessage msg) throws IOException {
        logger.log(Level.FINE, "Changing leaf set to reflect updated version");
        logger.log(Level.FINE, "New leafset is: \n" + msg.getResponseLeaf());

        if (msg.getResponseLeaf().getHi().equals("current"))
            this.leafSet.setLo(msg.getResponseLeaf().getLo(), msg.getResponseLeaf().getFullLo());
        else if (msg.getResponseLeaf().getLo().equals("current"))
            this.leafSet.setHi(msg.getResponseLeaf().getHi(), msg.getResponseLeaf().getFullHi());
        else
            this.leafSet = msg.getResponseLeaf();

        //Make sure I am connected to my leaf set
        if (ipConnectionMap.get(leafSet.getLoAddr() + "_" + leafSet.getLoPort()) == null) {
            Socket s = new Socket(leafSet.getLoAddr(), leafSet.getLoHostPort());
            Connection c = new Connection(this, s);
        }
        if(ipConnectionMap.get(leafSet.getHiAddr() + "_" + leafSet.getHiPort()) == null){
            Socket s = new Socket(leafSet.getHiAddr(), leafSet.getHiHostPort());
            Connection c = new Connection(this, s);
        }
    }

    private void parseExitOverlayMessage(ExitOverlayMessage msg) {
        logger.log(Level.FINE, "IP exiting: " + msg.getExitingIp());
        logger.log(Level.FINE, "Port exiting: " + msg.getExitingPort());

        String addrPort = msg.getExitingIp() + "_" + msg.getExitingPort();
        Connection toBeClosed = ipConnectionMap.get(addrPort);
        toBeClosed.closeConnection();
        ipConnectionMap.remove(addrPort);

        logger.log(Level.FINE, "Peer successfully removed from connection list.");
    }

    private void sendToHiLeaf(Connection joiningPeerConnection, int rowIndex) throws IOException {
        logger.log(Level.FINE, "My hi leaf is closer at: " + leafSet.getFullHi());
        logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
        joiningPeerConnection.sendMessage(new ForwardToMessage(this.id, leafSet.getHi(), leafSet.getFullHi(), rowIndex,
                routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
    }

    private void sendToLoLeaf(Connection joiningPeerConnection, int rowIndex) throws IOException {
        logger.log(Level.FINE, "My low leaf is closer at: " + leafSet.getFullLo());
        logger.log(Level.FINE, "Sending row: " + routingTable.getRow(rowIndex));
        joiningPeerConnection.sendMessage(new ForwardToMessage(this.id, leafSet.getLo(), leafSet.getFullLo(), rowIndex,
                routingTable.getRow(rowIndex), routingTable.getIpFromRow(routingTable.getRow(rowIndex))));
    }

    private synchronized void insertNewLeaf(JoinPeerMessage msg) throws IOException {
        if (msg.getLeafSet() == null)
            return;

        Connection joiningPeerConnection = ipConnectionMap.get(msg.getAddr() + "_" + msg.getPort());

        LeafSet newSet;
        String msgAddrPort = msg.getAddr() + "_" + msg.getHostPort() + "_" + msg.getPort();
        String thisAddrPort = joiningPeerConnection.getLocalAddr() + "_" + serverThread.getPort()
                + "_" + joiningPeerConnection.getLocalPort();

        if (leafSet.isEmpty()) { // First join in
            newSet = new LeafSet(this.id, this.id, thisAddrPort, thisAddrPort);
            joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));

            leafSet.setHi(msg.getId(), msgAddrPort);
            leafSet.setLo(msg.getId(), msgAddrPort);
            logger.log(Level.FINE, "New leafset is:\n" + leafSet);
        } else {
            LeafSet otherLeafSet = null;
            int diff = Util.getNumericalDifference(this.id, msg.getId());
            if (diff < 0) { // Joining is hi
                newSet = new LeafSet(leafSet.getHi(), this.id, leafSet.getFullHi(), thisAddrPort);
                otherLeafSet = new LeafSet("current", msg.getId(), "current", msgAddrPort);

                Socket s = new Socket(InetAddress.getByName(leafSet.getHiAddr()), leafSet.getHiHostPort());
                Connection otherConnection = new Connection(this, s);
                otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                leafSet.setHi(msg.getId(), msgAddrPort);
            } else { // Joining is lo
                newSet = new LeafSet(this.id, leafSet.getLo(), thisAddrPort, leafSet.getFullLo());
                otherLeafSet = new LeafSet(msg.getId(), "current", msgAddrPort, "current");

                Socket s = new Socket(InetAddress.getByName(leafSet.getLoAddr()), leafSet.getLoHostPort());
                Connection otherConnection = new Connection(this, s);
                otherConnection.sendMessage(new UpdateLeafSetMessage(otherLeafSet));

                leafSet.setLo(msg.getId(), msgAddrPort);
            }
            joiningPeerConnection.sendMessage(new UpdateLeafSetMessage(newSet));
        }
    }

    private void exitOverlay() throws IOException {
        logger.log(Level.FINE, "Attempting to exit overlay now.");

        Connection hiConnect = ipConnectionMap.get(leafSet.getHiAddr() + "_" + leafSet.getHiPort());
        Connection loConnect = ipConnectionMap.get(leafSet.getLoAddr() + "_" + leafSet.getLoPort());

        LeafSet newHi = new LeafSet("current", leafSet.getLo(), "current", leafSet.getFullLo());
        LeafSet newLo = new LeafSet(leafSet.getHi(), "current", leafSet.getFullHi(), "current");

        hiConnect.sendMessage(new UpdateLeafSetMessage(newHi));
        loConnect.sendMessage(new UpdateLeafSetMessage(newLo));

        for (Map.Entry<String, Connection> e : ipConnectionMap.entrySet()) {
            Connection val = e.getValue();
            String addrPort = val.getAddr() + "_" + val.getPort();
            val.sendMessage(
                    new ExitOverlayMessage(val.getLocalAddr(), val.getLocalPort()));
            val.closeConnection();
            ipConnectionMap.remove(addrPort);
        }

        logger.log(Level.FINE, "Overlay exit successful");
    }

}