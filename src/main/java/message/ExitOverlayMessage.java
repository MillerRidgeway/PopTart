package message;

public class ExitOverlayMessage extends Message {
    private String exitingIp, exitingId;
    private int exitingPort;

    public ExitOverlayMessage(String exitingId, String exitingIp, int exitingPort) {
        this.exitingId = exitingId;
        this.exitingIp = exitingIp;
        this.exitingPort = exitingPort;
    }

    public String getExitingId() {
        return exitingId;
    }

    public String getExitingIp() {
        return exitingIp;
    }

    public int getExitingPort() {
        return exitingPort;
    }
}
