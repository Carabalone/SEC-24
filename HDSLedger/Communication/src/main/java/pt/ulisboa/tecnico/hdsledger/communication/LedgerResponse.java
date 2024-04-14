package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;


public class LedgerResponse extends Message {

    // Serialized response
    private String response;

    private Type typeOfSerializedMessage;

    private int requestId;


    public LedgerResponse(Type type, Type typeOfSerializedMessage, String senderId, String response, int requestId) {
        super(senderId, type);
        this.response = response;
        this.typeOfSerializedMessage = typeOfSerializedMessage;
        this.requestId = requestId;
    }

    public LedgerResponseBalance deserializeBalance() {
        return new Gson().fromJson(this.getResponse(), LedgerResponseBalance.class);
    }

    public LedgerResponseTransfer deserializeTransfer() {
        return new Gson().fromJson(this.getResponse(), LedgerResponseTransfer.class);
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Type getTypeOfSerializedMessage() {
        return typeOfSerializedMessage;
    }

    public void setTypeOfSerializedMessage(Type typeOfSerializedMessage) {
        this.typeOfSerializedMessage = typeOfSerializedMessage;
    }
}