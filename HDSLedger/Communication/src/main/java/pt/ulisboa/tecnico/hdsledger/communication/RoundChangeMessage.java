package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class RoundChangeMessage {

    int consensusInstance;
    int currentRound;
    int preparedRound;
    String preparedValue;

    public RoundChangeMessage(int consensusInstance, int currentRound, int preparedRound, String preparedValue) {
        this.consensusInstance = consensusInstance;
        this.currentRound = currentRound;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
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

    public String getPreparedValue() {
        return preparedValue;
    }

    public void setPreparedValue(String preparedValue) {
        this.preparedValue = preparedValue;
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
                ", preparedValue='" + preparedValue + '\'' +
                '}';
    }
}
