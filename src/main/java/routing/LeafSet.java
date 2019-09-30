package routing;

import peer.Peer;

public class LeafSet {
    Peer hi, lo;

    public LeafSet(Peer hi, Peer lo) {
        this.hi = hi;
        this.lo = lo;
    }

    public Peer getHi() {
        return this.hi;
    }

    public Peer getLo() {
        return this.lo;
    }

    @Override
    public String toString() {
        return "Hi: " + getHi() + " Lo: " + getLo();
    }
}
