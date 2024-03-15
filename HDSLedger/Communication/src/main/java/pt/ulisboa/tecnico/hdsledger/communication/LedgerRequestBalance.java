package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestBalance extends Message {

    // Message identifier
    private int requestId;

    // Stored blockchain size
    private int knownBlockchainSize;

    // Signature of value with client's private key
    private String clientSignature;

    public LedgerRequestBalance(Type type, String senderId, int requestId, int knownBlockchainSize) {
        super(senderId, type);
        this.requestId = requestId;
        this.knownBlockchainSize = knownBlockchainSize;
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