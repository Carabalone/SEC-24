package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;

public class Block {
    private int consensusInstance;

    private List<LedgerRequest> requests;

    public Block() {
        requests = new ArrayList<>();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return consensusInstance == block.consensusInstance && Objects.equals(requests, block.requests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consensusInstance, requests);
    }

    @Override
    public String toString() {
        return "Block{" +
                "consensusInstance=" + consensusInstance +
                ", requests=" + requests +
                '}';
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public static String getBlockJson(Block block) {
        if (block == null)
            return null;
        else
            return block.toJson();
    }


    public String toJson() { return new Gson().toJson(this); }

    public static Block fromJson(String json) { return new Gson().fromJson(json, Block.class); }
}