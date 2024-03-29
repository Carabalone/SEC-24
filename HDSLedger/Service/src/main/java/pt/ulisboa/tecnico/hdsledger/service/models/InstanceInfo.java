package pt.ulisboa.tecnico.hdsledger.service.models;


import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private Block preparedBlock;
    private CommitMessage commitMessage;
    private Block inputBlock;
    private int committedRound = -1;


    private RoundChangeMessage roundChangeMessage;

    public InstanceInfo(Block inputBlock) {
        this.inputBlock = inputBlock;
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

    public Block getPreparedBlock() {
        return preparedBlock;
    }

    public void setPreparedBlock(Block preparedBlock) { this.preparedBlock = preparedBlock; }

    public Block getInputBlock() {
        return inputBlock;
    }

    public void setInputValue(Block inputBlock) {
        this.inputBlock = inputBlock;
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
