package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PrePrepareMessage {

    private String block;

    private Map<String, ConsensusMessage> justification = new ConcurrentHashMap<>();


    public PrePrepareMessage(String block) {
        this.block = block;
    }

    public PrePrepareMessage(String block, Map<String, ConsensusMessage> justification) {
        this.block = block;
        this.justification = justification;
    }

    public String getBlock() { return block; }

    public Map<String, ConsensusMessage> getJustification() { return justification; }

    public void setBlock(String block) { this.block = block; }

    public String toJson() { return new Gson().toJson(this); }
}