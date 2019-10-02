package routing;

import peer.MemberPeer;
import peer.Peer;
import peer.Util;

import java.util.ArrayList;


public class RoutingTable {
    private ArrayList<ArrayList<String>> table = new ArrayList<>();
    private MemberPeer owner;

    public RoutingTable(MemberPeer owner) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                table.get(i).add("");
            }
        }
        this.owner = owner;
    }

    public String findClosest(String id) {
        int rowIndex = Util.getIdDifference(owner.getId(), id);
        int closestIndex = 20; //Some value > 16 (hex max value value)
        for (int i = 0; i < 16; i++) {
            String entry = table.get(rowIndex).get(i);
            if (!entry.isEmpty()) {
                int idDigit = Character.digit(id.charAt(rowIndex), 16);
                int tableDigit = Character.digit(entry.charAt(rowIndex), 16);
                int diff = Math.abs(tableDigit - idDigit);
                if (diff < closestIndex) {
                    closestIndex = i;
                }
            }
        }
        if (closestIndex == 20) {
            //TODO - Set the leafset nodes because there is a blank row
            //TODO - Maybe also insert to the row not sure yet
            return "";
        } else
            return table.get(rowIndex).get(closestIndex);
    }

    public void insertNewPeer(String id) {

    }
}
