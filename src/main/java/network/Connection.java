package network;

import message.Message;
import peer.Peer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class Connection {
    private Peer p;
    private Socket s;
    private Recv recv;
    private ObjectOutputStream out;

    public Connection(Peer p, Socket s) throws IOException {
        this.p = p;
        this.s = s;

        //Out must come before in (recv) or both sides block
        out = new ObjectOutputStream(s.getOutputStream());
        this.recv = new Recv(s, p);
        this.recv.start();

        p.addNewConnection(this);
    }

    public static Connection newConnection(Peer p, Socket s) throws IOException {
        return new Connection(p, s);
    }

    public void closeConnection() {
        try {
            recv.stopRunning();
            s.close();
        } catch (IOException e) {
            System.out.println("Failed to close connection.");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getAddr() + "_" + getPort();
    }

    public String getAddr() {
        return s.getInetAddress().getHostAddress();
    }

    public String getLocalAddr() {
        return s.getLocalAddress().getHostAddress();
    }

    public int getPort() {
        return s.getPort();
    }

    public int getLocalPort() {
        return s.getLocalPort();
    }

    public void sendMessage(Message msg) throws IOException {
        out.writeObject(msg);
    }

}
