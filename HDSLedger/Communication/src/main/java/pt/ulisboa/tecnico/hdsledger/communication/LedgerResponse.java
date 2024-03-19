package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class LedgerResponse extends Message {

    // Serialized request
    private String message;

    private Type typeOfSerializedMessage;

    public LedgerResponse(Type type, Type typeOfSerializedMessage,String senderId, String message) {
        super(senderId, type);
        this.message = message;
        this.typeOfSerializedMessage = typeOfSerializedMessage;
    }

    public LedgerResponseAppend deserializeAppend() {
        return new Gson().fromJson(this.getMessage(), LedgerResponseAppend.class);
    }

    public LedgerResponseBalance deserializeBalance() {
        return new Gson().fromJson(this.getMessage(), LedgerResponseBalance.class);
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