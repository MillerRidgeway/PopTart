package network;

import peer.Peer;

import java.net.ServerSocket;

public class ServerThread extends Thread {
    private Peer p;
    private ServerSocket serv;

    public ServerThread(Peer p, ServerSocket serv) {
        this.serv = serv;
        this.p = p;
    }

    public void run() {
        while (serv != null) {
            try {
                Connection.newConnection(p, serv.accept());
            } catch (Exception e) {
                System.err.println("Error connecting to server socket.");
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        return serv.getLocalPort();
    }
}
