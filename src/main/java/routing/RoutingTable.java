package routing;

import peer.Peer;
import peer.Util;

import java.util.ArrayList;

public class RoutingTable {
    private ArrayList<ArrayList<String>> table = new ArrayList<>();
    private Peer owner;

    public RoutingTable(Peer owner) {
        this.owner = owner;
    }

    public String findClosest(String id) {
        int rowIndex = Util.getIdDifference(owner.getId(), id);
        for (int i = 0; i < 4; i++) {
            return table.get(rowIndex).get(i);
        }
        return "";
    }

    public void insertNewPeer(String id) {

    }
}
