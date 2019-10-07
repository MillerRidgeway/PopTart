package routing;

import peer.Peer;

import java.io.Serializable;

public class LeafSet implements Serializable {
    private String hi, lo, hiAddr, loAddr;

    public LeafSet(String hi, String lo, String hiAddr, String loAddr) {
        this.hi = hi;
        this.lo = lo;
        this.hiAddr = hiAddr;
        this.loAddr = loAddr;
    }

    public String getHi() {
        return this.hi;
    }

    public String getLo() {
        return this.lo;
    }

    public String getFullHi() {
        return this.hiAddr;
    }

    public String getFullLo() {
        return this.loAddr;
    }

    public String getHiAddr() {
        return this.hiAddr.split("_")[0];
    }

    public String getLoAddr() {
        return this.loAddr.split("_")[0];
    }

    public int getHiHostPort() {
        return Integer.parseInt(this.hiAddr.split("_")[1]);
    }

    public int getLoHostPort() {
        return Integer.parseInt(this.loAddr.split("_")[1]);
    }

    public int getHiPort() {
        return Integer.parseInt(this.hiAddr.split("_")[2]);
    }

    public int getLoPort() {
        return Integer.parseInt(this.loAddr.split("_")[2]);
    }

    public void setHi(String newHi, String newHiAddr) {
        this.hi = newHi;
        this.hiAddr = newHiAddr;
    }

    public void setLo(String newLo, String newLoAddr) {
        this.lo = newLo;
        this.loAddr = newLoAddr;
    }

    public boolean isEmpty() {
        return this.getHi().equals("") && this.getLo().equals("");
    }

    @Override
    public String toString() {
        return "Hi: " + getHi() + "-" + getFullHi() +
                "\nLo: " + getLo() + "-" + getFullLo();
    }
}
