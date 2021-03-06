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
    private ObjectInputStream input;
    private Peer p;
    private boolean running = true;

    public Recv(Socket s, Peer p) throws IOException {
        this.s = s;
        this.p = p;
        input = new ObjectInputStream(s.getInputStream());
    }

    public void run() {
        while (running) {
            try {
                p.parseMessage(readMessage());
            } catch (Exception e) {
                if (running) {
                    System.out.println("Failure reading message");
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopRunning() {
        this.running = false;
    }

    public Message readMessage() throws IOException, ClassNotFoundException {
        return (Message) input.readObject();
    }
}
