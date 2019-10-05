package routing;

import peer.MemberPeer;
import peer.Util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RoutingTable {
    private ArrayList<ArrayList<String>> idTable = new ArrayList<>();
    private Map<String, String> idConnectionMap = new ConcurrentHashMap<>();
    private MemberPeer owner;

    public RoutingTable(MemberPeer owner) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                idTable.get(i).add("DEFAULT");
            }
        }
        this.owner = owner;
    }

    public String findClosestIp(String id) {
        return idConnectionMap.get(id);
    }

    public String findClosest(String id) {
        int rowIndex = Util.getIdDifference(owner.getId(), id);
        if (rowIndex == -1) //Arrived at the closest node
            return owner.getId();
        int closestIndex = 20; //Some value > 16 (hex max value value)
        for (int i = 0; i < 16; i++) {
            String entry = idTable.get(rowIndex).get(i);
            if (!entry.isEmpty()) {
                int idDigit = Character.digit(id.charAt(rowIndex), 16);
                int tableDigit = Character.digit(entry.charAt(rowIndex), 16);
                int diff = Math.abs(tableDigit - idDigit);
                if (diff < closestIndex) {
                    closestIndex = i;
                }
            }
        }
        return idTable.get(rowIndex).get(closestIndex);
    }

    public ArrayList<String> getRow(int rowIndex) {
        return idTable.get(rowIndex);
    }

    public void insertNewPeer(String id, String addr, int port) {

    }

    @Override
    public String toString() {
        String tableString = "";
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                if (j == 15)
                    tableString += idTable.get(i).get(j) + "\n";
                else
                    tableString += idTable.get(i).get(j) + ",";
            }
        }
        return tableString;
    }
}
