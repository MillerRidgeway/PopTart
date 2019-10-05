package message;

import routing.LeafSet;

import java.io.Serializable;

public class UpdateLeafSetMessage extends Message {
    private LeafSet responseLeaf;

    public UpdateLeafSetMessage(LeafSet responseLeaf) {
        this.responseLeaf = responseLeaf;
    }

    public LeafSet getResponseLeaf() {
        return this.responseLeaf;
    }

}
