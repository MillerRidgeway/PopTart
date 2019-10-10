package peer;

import datastore.DataStore;
import message.Message;
import network.Connection;
import network.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class StoreClient implements Peer {
    private ServerThread serverThread;
    private Connection discoveryConnection;
    private DataStore dataStore;

    public StoreClient(String discoveryAddr, int discoveryPort, String storageDir) throws IOException{
        //Server Thread
        ServerSocket ss = new ServerSocket(0);
        serverThread = new ServerThread(this, ss);
        serverThread.start();
        System.out.println("Connections coming in on: " + ss.getLocalPort());

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
        }
    }

    @Override
    public void addNewConnection(Connection c) {

    }

    @Override
    public void parseMessage(Message msg) throws IOException {

    }

    @Override
    public String getId() {
        return "StoreCli";
    }
}
