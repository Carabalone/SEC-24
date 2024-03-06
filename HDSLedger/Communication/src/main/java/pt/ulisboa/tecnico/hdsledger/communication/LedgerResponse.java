package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LedgerResponse extends Message {

    // Consensus instance when value was decided
    private int consensusInstance;
    // New blockchain values
    private ArrayList<String> values;
    private int requestId;

    public LedgerResponse(String senderId, int consensusInstance, ArrayList<String> values, int requestId) {
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