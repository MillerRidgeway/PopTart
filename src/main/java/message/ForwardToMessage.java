package message;

import java.util.List;

public class ForwardToMessage extends Message {
    private String destId, destIp;
    private int destHostPort, rowIndex;
    private List<String> tableRow, ips;

    public ForwardToMessage(String destId, String destIp, int rowIndex, List<String> tableRow, List<String> ips) {
        this.destId = destId;
        this.tableRow = tableRow;
        this.rowIndex = rowIndex;
        this.ips = ips;
        if (destIp.isEmpty()) {
            this.destIp = "";
        } else {
            this.destIp = destIp.split("_")[0];
            this.destHostPort = Integer.parseInt(destIp.split("_")[1]);
        }
    }

    public List<String> getIps() {
        return ips;
    }

    public int getDestHostPort() {
        return destHostPort;
    }

    public String getDestId() {
        return destId;
    }

    public String getDestIp() {
        return destIp;
    }

    public List<String> getTableRow() {
        return tableRow;
    }

    public int getRowIndex() {
        return rowIndex;
    }
}
