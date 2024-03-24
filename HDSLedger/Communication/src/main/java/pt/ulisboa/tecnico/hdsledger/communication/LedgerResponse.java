package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class LedgerResponse extends Message {
    private String message;

    private Type typeOfSerializedMessage;

    private int requestId;


    public LedgerResponse(Type type, Type typeOfSerializedMessage, String senderId, String message, int requestId) {
        super(senderId, type);
        this.message = message;
        this.typeOfSerializedMessage = typeOfSerializedMessage;
        this.requestId = requestId;
    }

    public LedgerResponseAppend deserializeAppend() {
        return new Gson().fromJson(this.getMessage(), LedgerResponseAppend.class);
    }

    public LedgerResponseBalance deserializeBalance() {
        return new Gson().fromJson(this.getMessage(), LedgerResponseBalance.class);
    }

    public LedgerResponseTransfer deserializeTransfer() {
        return new Gson().fromJson(this.getMessage(), LedgerResponseTransfer.class);
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

    public Type getTypeOfSerializedMessage() {
        return typeOfSerializedMessage;
    }

    public void setTypeOfSerializedMessage(Type typeOfSerializedMessage) {
        this.typeOfSerializedMessage = typeOfSerializedMessage;
    }
}