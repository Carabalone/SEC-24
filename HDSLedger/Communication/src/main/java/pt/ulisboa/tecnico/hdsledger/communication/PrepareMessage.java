package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class PrepareMessage {

    private String value;

    private String leaderSignature;

    public PrepareMessage(String value, String leaderSignature) {
        this.value = value;
        this.leaderSignature = leaderSignature;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

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