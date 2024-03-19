package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class LedgerRequest extends Message {

    // Serialized request
    private String message;

    private String clientSignature;

    public LedgerRequest(String senderId, Type type, String message, String signature) {
        super(senderId, type);
        this.message = message;
        this.clientSignature = signature;
    }

    public LedgerRequestAppend deserializeAppend() {
        return new Gson().fromJson(message, LedgerRequestAppend.class);
    }

    public LedgerRequestBalance deserializeBalance() {
        return new Gson().fromJson(message, LedgerRequestBalance.class);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClientSignature() {
        return clientSignature;
    }

    public void setClientSignature(String clientSignature) {
        this.clientSignature = clientSignature;
    }
}