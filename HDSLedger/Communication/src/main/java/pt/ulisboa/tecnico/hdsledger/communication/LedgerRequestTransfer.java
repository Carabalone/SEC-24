package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestTransfer extends Message {

    private String destinationId;

    private int amount;

    private String signature;

    public LedgerRequestTransfer(Type type, String senderId, String destinationId, int amount, String signature) {
        super(senderId, type);
        this.destinationId = destinationId;
        this.amount = amount;
        this.signature = signature;
    }

    public String getDestinationId() { return destinationId; }

    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public int getAmount() { return amount; }

    public void setAmount(int amount) { this.amount = amount; }

    public String getSignature() { return signature; }

    public void setSignature(String signature) { this.signature = signature; }
}