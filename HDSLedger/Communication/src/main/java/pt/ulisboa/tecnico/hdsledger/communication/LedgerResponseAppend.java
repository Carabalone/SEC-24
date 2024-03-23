package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.ArrayList;

public class LedgerResponseAppend extends Message {
    private int consensusInstance;

    private ArrayList<String> values;


    public LedgerResponseAppend(String senderId, int consensusInstance, ArrayList<String> values) {
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