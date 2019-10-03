package routing;

import peer.Peer;

import java.io.Serializable;

public class LeafSet {
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

    public String getHiAddr() {
        return this.hiAddr;
    }

    public String getLoAddr() {
        return this.loAddr;
    }

    public void setHi(String newHi, String newHiAddr) {
        this.hi = newHi;
        this.hiAddr = newHiAddr;
    }

    public void setLo(String newLo, String newLoAddr) {
        this.lo = newLo;
        this.loAddr = newLoAddr;
    }

    public boolean contains(String id) {
        return id.equals(this.getHi()) || id.equals(this.getLo());
    }

    public boolean isEmpty() {
        return this.getHi().equals("") && this.getLo().equals("");
    }

    @Override
    public String toString() {
        return "Hi: " + getHi() + " Lo: " + getLo();
    }
}
