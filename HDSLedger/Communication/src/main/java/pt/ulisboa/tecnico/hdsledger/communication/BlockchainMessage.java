package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class BlockchainMessage extends Message {
    private String message;

    public BlockchainMessage(String senderId, Type type, String message) {
        super(senderId, type);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
