package network;

import message.Message;
import peer.Peer;

import java.io.IOException;
import java.net.Socket;

public class Connection {
    private Peer p;
    private Socket s;
    private Recv recv;

    public Connection(Peer p, Socket s) throws IOException {
        this.p = p;
        this.s = s;
        this.recv = new Recv(s);
        System.out.println("New connection made to: " +
                s.getInetAddress().getHostAddress() + "_" + s.getPort());
    }

    public static Connection newConnection(Peer p, Socket s) throws IOException {
        return new Connection(p, s);
    }

    public void sendMessage(String msg) {
        System.out.println("Message send triggered.");
    }

}
