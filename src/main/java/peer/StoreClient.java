package peer;

import message.*;
import network.Connection;
import network.ServerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class StoreClient implements Peer {
    private ServerThread serverThread;
    private Connection discoveryConnection;
    private String fileId;
    private Map<String, Connection> ipConnectionMap = new ConcurrentHashMap<>();
    private FileStoreMessage fsm;
    private static final Logger logger = Logger.getLogger(StoreClient.class.getName());
    private static FileHandler fh;

    public StoreClient(String discoveryAddr, int discoveryPort) throws IOException {
        //Server Thread
        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("Connections coming in on: " + ss.getLocalPort());

        //Logger
        fh = new FileHandler("storeClient.log");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.FINER);

        //Discovery connection
        Socket s = new Socket(discoveryAddr, discoveryPort);
        discoveryConnection = new Connection(this, s);

        startConsole();
    }

    public static void main(String[] args) throws IOException {
        new StoreClient(args[0], Integer.parseInt(args[1]));
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
                System.out.println("File ID is: " + fileId);
                File f = new File(fName);
                Object contents = Files.readAllBytes(f.toPath());

                DiscoverMessage dm = new DiscoverMessage(getId(), discoveryConnection.getAddr(), serverThread.getPort(),
                        discoveryConnection.getLocalPort(), true);
                fsm = new FileStoreMessage(fileId, f, contents);
                discoveryConnection.sendMessage(dm);
            } catch (Exception e) {
                System.out.println("Error sending file: " + e);
            }

        }
    }

    @Override
    public void addNewConnection(Connection c) {
        ipConnectionMap.put(c.getAddr() + "_" + c.getPort(), c);
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
        } else if (msg instanceof ForwardToMessage) {
            logger.log(Level.FINE, "Got forward to, moving to next destination.");
            parseForwardToMessage((ForwardToMessage) msg);
        }
    }

    private void parseJoinAckMessage(JoinAckMessage msg) throws IOException {
        logger.log(Level.FINE, "Connecting to IP: " + msg.getRandPeer());
        logger.log(Level.FINE, "Connecting to port: " + msg.getHostPort());

        Socket s = new Socket(InetAddress.getByName(msg.getRandPeer()), msg.getHostPort());
        Connection c = new Connection(this, s);
        c.sendMessage(new JoinPeerMessage(this.fileId, c.getLocalAddr(), c.getLocalPort(),
                serverThread.getPort(), null));
    }

    private synchronized void parseForwardToMessage(ForwardToMessage msg) throws IOException {
        if (msg.getDestIp().isEmpty()) { //Final destination, send the file. (FileStoreMessage)
            logger.log(Level.FINE, "Found node to store file " + fileId + " at.");
            String ip = msg.getPitstop().split("_")[1];
            String port = msg.getPitstop().split("_")[2];
            Connection storageDestination = ipConnectionMap.get(ip + "_" + port);

            storageDestination.sendMessage(this.fsm);

            String discoveryInfo = discoveryConnection.getAddr() + "_" + discoveryConnection.getPort();
            for (Map.Entry<String, Connection> e : ipConnectionMap.entrySet()) {
                Connection val = e.getValue();
                String addrPort = val.getAddr() + "_" + val.getPort();
                if (!(addrPort.equals(discoveryInfo))) {
                    val.sendMessage(
                            new ExitOverlayMessage(this.getId(), val.getLocalAddr(), val.getLocalPort()));
                    val.closeConnection();
                    ipConnectionMap.remove(addrPort);
                }
            }
        } else { // Continue bouncing around the twork
            logger.log(Level.FINE, "There is a closer peer at: " + msg.getDestIp());
            logger.log(Level.FINE, "I am connecting to port: " + msg.getDestHostPort());
            logger.log(Level.FINE, "Forwarded from node " + msg.getPitstop());
            Socket s = new Socket(InetAddress.getByName(msg.getDestIp()), msg.getDestHostPort());
            Connection c = new Connection(this, s);
            c.sendMessage(new JoinPeerMessage(fileId, c.getLocalAddr(), c.getLocalPort(),
                    serverThread.getPort(), null));
        }
    }
}
