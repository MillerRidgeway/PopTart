package peer;

import datastore.DataStore;
import message.*;
import network.Connection;
import network.ServerThread;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class StoreClient implements Peer {
    private ServerThread serverThread;
    private Connection discoveryConnection;
    private DataStore dataStore;
    private String fileId;
    private static final Logger logger = Logger.getLogger(StoreClient.class.getName());
    private static FileHandler fh;

    public StoreClient(String discoveryAddr, int discoveryPort, String storageDir) throws IOException {
        //Server Thread
        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("Connections coming in on: " + ss.getLocalPort());

        //Logger
        fh = new FileHandler("StoreClient.log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINER);

        //Discovery connection
        Socket s = new Socket(discoveryAddr, discoveryPort);
        discoveryConnection = new Connection(this, s);
    }

    public static void main(String[] args) throws IOException {
        new StoreClient(args[0], Integer.parseInt(args[1]), args[2]);
    }

    @Override
    public void startConsole() {
        Scanner scn = new Scanner(System.in);
        while (true) {
            System.out.println("Please enter a filename to upload: ");
            String fName = scn.nextLine();
            System.out.println("Attempting to upload: " + fName);
            try {
                this.fileId = Util.getFilenameHash(fName);
                File f = new File(fName);
                DiscoverMessage dm = new DiscoverMessage(getId(), discoveryConnection.getAddr(), serverThread.getPort(),
                        discoveryConnection.getLocalPort());
                FileStoreMessage fsm = new FileStoreMessage(fileId, f, dm);
                discoveryConnection.sendMessage(fsm);
            } catch (Exception e) {
                System.out.println("Error in sending file.");
                e.printStackTrace();
            }

        }
    }

    @Override
    public void addNewConnection(Connection c) {
        System.out.println("New connection to: " + c);
    }

    @Override
    public String getId() {
        return "StoreCli";
    }

    @Override
    public void parseMessage(Message msg) throws IOException {
        if (msg instanceof JoinAckMessage) {
            logger.log(Level.FINE, "Got ack for file store request");
            parseJoinAckMessage((JoinAckMessage) msg);
        }
    }

    private void parseJoinAckMessage(JoinAckMessage msg) throws UnknownHostException, IOException {
        logger.log(Level.FINE, "Connecting to IP: " + msg.getRandPeer());
        logger.log(Level.FINE, "Connecting to port: " + msg.getHostPort());

        Socket s = new Socket(InetAddress.getByName(msg.getRandPeer()), msg.getHostPort());
        Connection c = new Connection(this, s);
        c.sendMessage(new JoinPeerMessage(this.fileId, c.getLocalAddr(), c.getLocalPort(),
                serverThread.getPort(), null));
    }
}
