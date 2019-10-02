package routing;

import peer.Peer;

public class LeafSet {
    String hi, lo;

    public LeafSet(String hi, String lo) {
        this.hi = hi;
        this.lo = lo;
    }

    public String getHi() {
        return this.hi;
    }

    public String getLo() {
        return this.lo;
    }

    public void setHi(String newHi) {
        this.hi = newHi;
    }

    public void setLo(String newLo) {
        this.lo = newLo;
    }

    @Override
    public String toString() {
        return "Hi: " + getHi() + " Lo: " + getLo();
    }
}
