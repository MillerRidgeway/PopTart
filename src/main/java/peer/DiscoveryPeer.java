package peer;

import message.DiscoverMessage;
import message.Message;
import message.RediscoverMessage;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class DiscoveryPeer implements Peer {
    private ServerThread serverThread;
    private int port;
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private final ArrayList<String> knownIds = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(DiscoveryPeer.class.getName());

    public DiscoveryPeer(int port) throws IOException {
        ServerSocket s = new ServerSocket(port);
        serverThread = new ServerThread(this, s);
        this.port = port;

        System.out.println("Discovery peer listening for connections on: " + port);
        serverThread.start();

        Handler fh = new FileHandler("discoveryPeer.log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINE);

        //startConsole();
    }


    public static void main(String[] args) throws IOException {
        new DiscoveryPeer(Integer.parseInt(args[0]));
    }

    public void startConsole() {
        Scanner scn = new Scanner(System.in);
        System.out.print("Please enter a command: ");
        String command = scn.nextLine();
    }

    public int getServerPort() {
        return serverThread.getPort();
    }

    @Override
    public void parseMessage(Message msg) throws IOException {
        if (msg instanceof DiscoverMessage) {
            parseDiscoverMessage((DiscoverMessage) msg);
        }
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
        logger.log(Level.FINE, "ID of peer: " + msg.getId() + "\n");

        if (knownIds.contains(msg.getId())) {
            logger.log(Level.FINE, "ID " + msg.getId() + " is already in use. Sending rediscover request");
            Connection c = connectionMap.get(msg.getHost() + "_" + msg.getPort());
            c.sendMessage(new RediscoverMessage());
        } else {
            knownIds.add(msg.getId());
        }
    }
}
