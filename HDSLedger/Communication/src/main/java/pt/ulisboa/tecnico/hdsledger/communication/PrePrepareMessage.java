package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.sun.tools.jconsole.JConsoleContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrePrepareMessage {

    private String block;

    private String leaderSignature;
    private Map<String, ConsensusMessage> justification = new ConcurrentHashMap<>();

    public PrePrepareMessage(String block/*, String leaderSignature*/) {
        this.block = block;
        this.leaderSignature = "TODO: tirar";
    }

    public PrePrepareMessage(String block/*, String leaderSignature*/, Map<String, ConsensusMessage> justification) {
        this.block = block;
        this.leaderSignature = "TODO: tirar";
        this.justification = justification;
    }

    public String getBlock() { return block; }

    public Map<String, ConsensusMessage> getJustification() {
        return justification;
    }

    public void setBlock(String block) { this.block = block; }

    public String getLeaderSignature() {
        return leaderSignature;
    }

    public void setLeaderSignature(String leaderSignature) {
        this.leaderSignature = leaderSignature;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}