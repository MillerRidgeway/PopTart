package routing;

import peer.MemberPeer;
import peer.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RoutingTable {
    private ArrayList<List<String>> idTable = new ArrayList<>(4);
    private Map<String, String> idConnectionMap = new ConcurrentHashMap<>();
    private MemberPeer owner;

    public RoutingTable(MemberPeer owner) {
        this.owner = owner;
        for (int i = 0; i < 4; i++) {
            idTable.add(new ArrayList<>());
            for (int j = 0; j < 16; j++) {
                idTable.get(i).add("DEFAULT");
            }
        }
        for (int i = 0; i < owner.getId().length(); i++) {
            int colIndex = Character.digit(owner.getId().charAt(i), 16);
            idTable.get(i).set(colIndex, owner.getId());
        }
        idConnectionMap.put(owner.getId(), owner.getConnectionInfo());
        idConnectionMap.put("DEFAULT", "DEFAULT");
    }

    public String findClosestIp(String id) {
        return idConnectionMap.get(id);
    }

    public String findClosest(String id) {
        int rowIndex = Util.getIdDifference(owner.getId(), id);
        int closestIndex = 20; //Some value > 16 (hex max value value)
        for (int i = 0; i < 16; i++) {
            String entry = idTable.get(rowIndex).get(i);
            int idDigit = Character.digit(id.charAt(rowIndex), 16);
            int tableDigit = Character.digit(entry.charAt(rowIndex), 16);
            if (!entry.equals("DEFAULT")) {
                int diff = Math.abs(tableDigit - idDigit);
                if (diff < closestIndex) {
                    closestIndex = i;
                }
            } else if (i == Character.digit(id.charAt(rowIndex), 16)) {
                idTable.get(rowIndex).set(i, id);
            }
        }
        return idTable.get(rowIndex).get(closestIndex);
    }

    public List<String> getRow(int rowIndex) {
        return idTable.get(rowIndex);
    }

    public List<String> getIpFromRow(List<String> row) {
        List<String> ips = new ArrayList<>();
        for (String s : row) {
            ips.add(idConnectionMap.get(s));
        }
        return ips;
    }

    public synchronized void putIps(List<String> ids, List<String> ips) {
        for (int i = 0; i < ips.size(); i++) {
            idConnectionMap.put(ids.get(i), ips.get(i));
        }
    }

    public void setRow(int index, List<String> row) {
        idTable.set(index, row);
    }

    public void insertNewPeer(String id, String addr, int port) {
        int rowIndex = Util.getIdDifference(owner.getId(), id);
        int colIndex = Character.digit(id.charAt(rowIndex), 16);
        idTable.get(rowIndex).set(colIndex, id);
        idConnectionMap.put(id, addr + "_" + port);
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
