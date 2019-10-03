package peer;

import message.*;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class DiscoveryPeer implements Peer {
    private ServerThread serverThread;
    private int port;
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> connectionHostMap = new ConcurrentHashMap<>();
    private final ArrayList<String> knownIds = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(DiscoveryPeer.class.getName());

    public DiscoveryPeer(int port) throws IOException {
        //Server thread
        ServerSocket s = new ServerSocket(port);
        serverThread = new ServerThread(this, s);
        this.port = port;
        System.out.println("Discovery peer listening for connections on: " + port);
        serverThread.start();

        //Logger
        Handler fh = new FileHandler("discoveryPeer.log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINE);

        startConsole();
    }


    public static void main(String[] args) throws IOException {
        new DiscoveryPeer(Integer.parseInt(args[0]));
    }

    private void startConsole() {
        Scanner scn = new Scanner(System.in);
        while (true) {
            System.out.println("Please enter a command: ");
            String command = scn.nextLine();
            switch (command) {
                case "list-nodes":
                    for (Map.Entry<String, Connection> e : connectionMap.entrySet()) {
                        System.out.println("Connected node: " + e.getValue().toString());
                        break;
                    }
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        }
    }

    @Override
    public void parseMessage(Message msg) throws IOException {
        if (msg instanceof DiscoverMessage) {
            parseDiscoverMessage((DiscoverMessage) msg);
        }
    }

    @Override
    public String getId() {
        return "Discovery Peer";
    }

    @Override
    public void addNewConnection(Connection c) {
        connectionMap.put(c.getAddr() + "_" + c.getPort(), c);
    }

    private void parseDiscoverMessage(DiscoverMessage msg) throws IOException {
        logger.log(Level.FINE, "Got discovery message.");
        logger.log(Level.FINE, "Host addr: " + msg.getHost());
        logger.log(Level.FINE, "Host port: " + msg.getHostPort());
        logger.log(Level.FINE, "Thread port: " + msg.getPort());
        logger.log(Level.FINE, "ID: " + msg.getId());
        Connection c = connectionMap.get(msg.getHost() + "_" + msg.getPort());

        if (knownIds.contains(msg.getId())) {
            logger.log(Level.FINE, "ID " + msg.getId() + " is already in use. Sending rediscover request");
            c.sendMessage(new RediscoverMessage());
        } else if (knownIds.size() == 0) {
            logger.log(Level.FINE, "First connected peer, sending first connect ack.");
            c.sendMessage(new FirstConnectAckMessage());
            knownIds.add(msg.getId());
            connectionHostMap.put(msg.getHost() + "_" + msg.getPort(), msg.getHostPort());
        } else {
            logger.log(Level.FINE, "Sending a random node for routing.");
            //Get a random peer from the active peer set
            Random generator = new Random();
            Map<String, Connection> tempMap = new ConcurrentHashMap<>(connectionMap);
            tempMap.remove(msg.getHost() + "_" + msg.getPort());
            Object[] vals = tempMap.keySet().toArray();
            String key = (String) vals[generator.nextInt(vals.length)];
            c.sendMessage(new JoinAckMessage(key, connectionHostMap.get(key)));

            knownIds.add(msg.getId());
            connectionHostMap.put(msg.getHost() + "_" + msg.getPort(), msg.getHostPort());
        }
    }
}
