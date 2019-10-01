package message;

import java.io.Serializable;

public class JoinPeerMessage extends Message implements Serializable {
    private String id;

    public JoinPeerMessage(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }
}
