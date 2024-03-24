package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class LedgerRequest extends Message {

    // Serialized request
    private String request;

    private String clientSignature;

    private int requestId;

    public LedgerRequest(String senderId, Type type, int requestId, String request, String signature) {
        super(senderId, type);
        this.requestId = requestId;
        this.request = request;
        this.clientSignature = signature;
    }

    public LedgerRequestAppend deserializeAppend() {
        return new Gson().fromJson(request, LedgerRequestAppend.class);
    }

    public LedgerRequestBalance deserializeBalance() { return new Gson().fromJson(request, LedgerRequestBalance.class); }

    public LedgerRequestTransfer deserializeTransfer() { return new Gson().fromJson(request, LedgerRequestTransfer.class); }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getClientSignature() {
        return clientSignature;
    }

    public void setClientSignature(String clientSignature) {
        this.clientSignature = clientSignature;
    }
}