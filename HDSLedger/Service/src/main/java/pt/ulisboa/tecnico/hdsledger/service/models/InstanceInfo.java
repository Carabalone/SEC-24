package pt.ulisboa.tecnico.hdsledger.service.models;


import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private String preparedValue;
    private CommitMessage commitMessage;
    private String inputValue;
    private int committedRound = -1;


    private RoundChangeMessage roundChangeMessage;

    public InstanceInfo(String inputValue) {
        this.inputValue = inputValue;
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

    public String getInputValue() {
        if (inputValue == null) {
            System.out.println("\n\n\n\n\nNULL INPUT VALUE \n\n\n\n");
        }
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public int getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(int committedRound) {
        this.committedRound = committedRound;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(CommitMessage commitMessage) {
        this.commitMessage = commitMessage;
    }

    public RoundChangeMessage getRoundChangeMessage() {
        return roundChangeMessage;
    }

    public void setRoundChangeMessage(RoundChangeMessage roundChangeMessage) {
        this.roundChangeMessage = roundChangeMessage;
    }
}
