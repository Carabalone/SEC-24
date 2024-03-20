package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestAppend extends Message {
    private String value;

    private int knownBlockchainSize;


    public LedgerRequestAppend(Type type, String senderId, String value, int knownBlockchainSize) {
        super(senderId, type);
        this.value = value;
        this.knownBlockchainSize = knownBlockchainSize;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getKnownBlockchainSize() {
        return knownBlockchainSize;
    }

    public void setKnownBlockchainSize(int knownBlockchainSize) {
        this.knownBlockchainSize = knownBlockchainSize;
    }
}