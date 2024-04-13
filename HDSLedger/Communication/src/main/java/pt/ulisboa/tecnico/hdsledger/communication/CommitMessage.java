package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;


public class CommitMessage {

    private String block;
    private String signature;

    public CommitMessage(String block, String signature) {
        this.block = block;
        this.signature = signature;
    }

    public String getBlock() {
        return block;
    }

    public String getSignature() { return signature; }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
