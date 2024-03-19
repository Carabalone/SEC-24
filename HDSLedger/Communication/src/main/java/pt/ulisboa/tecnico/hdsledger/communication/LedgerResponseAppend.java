package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.ArrayList;

public class LedgerResponseAppend extends Message {

    // Consensus instance when value was decided
    private int consensusInstance;
    // New blockchain values
    private ArrayList<String> values;
    private int requestId;

    public LedgerResponseAppend(String senderId, int consensusInstance, ArrayList<String> values, int requestId) {
        super(senderId, Type.REPLY);
        this.consensusInstance = consensusInstance;
        this.values = values;
        this.requestId = requestId;
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

    public int getRequestId() {
        return requestId;
    }
}