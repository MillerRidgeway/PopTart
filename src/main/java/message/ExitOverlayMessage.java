package message;

public class ExitOverlayMessage extends Message {
    private String exitingIp;
    private int exitingPort;

    public ExitOverlayMessage(String exitingIp, int exitingPort){
        this.exitingIp = exitingIp;
        this.exitingPort = exitingPort;
    }

    public String getExitingIp() {
        return exitingIp;
    }

    public int getExitingPort() {
        return exitingPort;
    }
}
