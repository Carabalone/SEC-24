package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestAppend extends Message {

    // value to append to the blockchain
    private String value;

    // Signature of value with client's private key
    private String clientSignature;

    private int requestId;
    // Stored blockchain size

    private int knownBlockchainSize;
    // Value to append to the blockchain


    public LedgerRequestAppend(Type type, String senderId, String value, String clientSignature, int requestId, int knownBlockchainSize) {
        super(senderId, type);
        this.value = value;
        this.clientSignature = clientSignature;
        this.requestId = requestId;
        this.knownBlockchainSize = knownBlockchainSize;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getClientSignature() {
        return clientSignature;
    }

    public void setClientSignature(String clientSignature) {
        this.clientSignature = clientSignature;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getKnownBlockchainSize() {
        return knownBlockchainSize;
    }

    public void setKnownBlockchainSize(int knownBlockchainSize) {
        this.knownBlockchainSize = knownBlockchainSize;
    }
}