package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;

public class Block {
    private int consensusInstance;

    private List<LedgerRequest> requests;

    public Block(int consensusInstance, ArrayList<LedgerRequest> requests) {
        this.consensusInstance = consensusInstance;
        this.requests = requests;
    }

    public void addRequest(LedgerRequest request) {
        requests.add(request);
    }

    public List<LedgerRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<LedgerRequest> requests) {
        this.requests = requests;
    }

    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }
}