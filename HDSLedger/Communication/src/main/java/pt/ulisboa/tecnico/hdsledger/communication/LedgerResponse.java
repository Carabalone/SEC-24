package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.ArrayList;
import java.util.List;

public class LedgerResponse extends Message {

    // Consensus instance when value was decided
    private int consensusInstance;
    // New blockchain values
    private ArrayList<String> values;

    public LedgerResponse(String senderId, int consensusInstance, ArrayList<String> values) {
        super(senderId, Type.REPLY);
        this.consensusInstance = consensusInstance;
        this.values = values;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }

    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }
}