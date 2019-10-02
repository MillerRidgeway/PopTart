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

    @Override
    public String toString() {
        return "Hi: " + getHi() + " Lo: " + getLo();
    }
}
