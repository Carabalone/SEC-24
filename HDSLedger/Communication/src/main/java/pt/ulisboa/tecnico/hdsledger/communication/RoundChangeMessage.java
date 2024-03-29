package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class RoundChangeMessage {

    private int consensusInstance;

    private int currentRound;

    private int preparedRound;

    private String preparedBlock;


    public RoundChangeMessage(int consensusInstance, int currentRound, int preparedRound, String preparedBlock) {
        this.consensusInstance = consensusInstance;
        this.currentRound = currentRound;
        this.preparedRound = preparedRound;
        this.preparedBlock = preparedBlock;
    }


    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public String getPreparedBlock() {
        return preparedBlock;
    }

    public void setPreparedBlock(String preparedBlock) {
        this.preparedBlock = preparedBlock;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "RoundChangeMessage{" +
                "consensusInstance=" + consensusInstance +
                ", currentRound=" + currentRound +
                ", preparedRound=" + preparedRound +
                ", preparedValue='" + preparedBlock + '\'' +
                '}';
    }
}
