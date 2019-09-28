package network;

import message.Message;
import peer.Peer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Recv extends Thread {
    private Socket s;
    private DataInputStream input;
    private Peer p;

    public Recv(Socket s, Peer p) throws IOException {
        this.s = s;
        this.p = p;
        input = new DataInputStream(s.getInputStream());
    }

    public void run() {
        while (s != null) {
            try {
                p.parseMessage(readMessage());
            } catch (Exception e) {
                System.out.println("Failure reading message");
                e.printStackTrace();
            }
        }
    }

    public Message readMessage() throws IOException, ClassNotFoundException {
        int len = input.readInt();
        byte[] contents = new byte[len];
        input.read(contents);

        ByteArrayInputStream in = new ByteArrayInputStream(contents);
        ObjectInputStream is = new ObjectInputStream(in);

        return (Message) is.readObject();
    }
}
