package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.Objects;


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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LedgerRequest that = (LedgerRequest) o;
        return requestId == that.requestId && Objects.equals(request, that.request) && Objects.equals(clientSignature, that.clientSignature);
    }

    @Override
    public String toString() {
        return "LedgerRequest{" +
                "requestId=" + requestId +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, clientSignature, requestId);
    }
}