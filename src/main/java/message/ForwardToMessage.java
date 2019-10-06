package message;

import java.util.List;

public class ForwardToMessage extends Message {
    private String destId, destIp;
    private int destHostPort;
    private List<String> tableRow;

    public ForwardToMessage(String destId, String destIp, List<String> tableRow) {
        this.destId = destId;
        this.destIp = destIp.split("_")[0];
        this.destHostPort = Integer.parseInt(destIp.split("_")[1]);
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
}
