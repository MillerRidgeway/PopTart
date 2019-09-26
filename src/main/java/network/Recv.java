package network;

import message.Message;
import peer.Peer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class Recv extends Thread {
    private Socket s;
    private DataInputStream input;

    public Recv(Socket s) throws IOException {
        this.s = s;
        input = new DataInputStream(s.getInputStream());
    }

    public void run() {
        while (s != null) {
            try {
                readMessage();
            } catch (Exception e) {
                System.out.println("Failure reading message");
                e.printStackTrace();
            }
        }
    }

    public void readMessage() throws IOException {
        String contents = input.readUTF();
        System.out.println("The message was: " + contents);
    }
}
