package network;

import message.Message;
import peer.Peer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

public class Connection {
    private Peer p;
    private Socket s;
    private Recv recv;
    private DataOutputStream out;

    public Connection(Peer p, Socket s) throws IOException {
        this.p = p;
        this.s = s;
        this.recv = new Recv(s, p);
        this.recv.start();

        out = new DataOutputStream(s.getOutputStream());

        p.addNewConnection(this);
        System.out.println("New connection made to: " +
                s.getInetAddress().getHostAddress() + "_" + s.getPort());
    }

    public static Connection newConnection(Peer p, Socket s) throws IOException {
        return new Connection(p, s);
    }

    public String getAddr() {
        return s.getInetAddress().getHostAddress();
    }

    @Override
    public String toString(){
        return p.getId() + "-" + getAddr() + "_" + getPort();
    }

    public int getPort() {
        return s.getPort();
    }

    public int getLocalPort() {
        return s.getLocalPort();
    }

    public void sendMessage(Message msg) throws IOException {
        byte[] messageInBytes = msg.toBytes();
        out.writeInt(messageInBytes.length);
        out.write(messageInBytes);
        out.flush();
    }

}
