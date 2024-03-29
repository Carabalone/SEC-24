package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.List;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;

public class Block {
    private int consensusInstance;

    private List<LedgerRequest> requests;

    public Block() {}

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

    public String toJson() { return new Gson().toJson(this); }

    public static Block fromJson(String json) { return new Gson().fromJson(json, Block.class); }
}