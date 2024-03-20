package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class LedgerRequest extends Message {

    // Serialized request
    private String message;

    private String clientSignature;

    private int requestId;

    public LedgerRequest(String senderId, Type type, int requestId, String message, String signature) {
        super(senderId, type);
        this.requestId = requestId;
        this.message = message;
        this.clientSignature = signature;
    }

    public LedgerRequestAppend deserializeAppend() {
        return new Gson().fromJson(message, LedgerRequestAppend.class);
    }

    public LedgerRequestBalance deserializeBalance() {
        return new Gson().fromJson(message, LedgerRequestBalance.class);
    }

    public LedgerRequestTransfer deserializeTransfer() {
        return new Gson().fromJson(message, LedgerRequestTransfer.class);
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
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