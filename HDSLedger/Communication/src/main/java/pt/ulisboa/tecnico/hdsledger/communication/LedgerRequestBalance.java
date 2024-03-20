package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestBalance extends Message {
    private int knownBlockchainSize;

    public LedgerRequestBalance(Type type, String senderId, int knownBlockchainSize) {
        super(senderId, type);
        this.knownBlockchainSize = knownBlockchainSize;
    }

    public int getKnownBlockchainSize() {
        return knownBlockchainSize;
    }

    public void setKnownBlockchainSize(int knownBlockchainSize) {
        this.knownBlockchainSize = knownBlockchainSize;
    }
}