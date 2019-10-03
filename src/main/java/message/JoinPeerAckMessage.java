package message;

import routing.LeafSet;

import java.io.Serializable;

public class JoinPeerAckMessage extends Message {
    public String responseHiId, responseLoId, responseHiAddr, responseLoAddr;


}
