package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequestTransfer extends Message {

    private String destinationId;

    private int amount;

    public LedgerRequestTransfer(Type type, String senderId, String destinationId, int amount) {
        super(senderId, type);
        this.destinationId = destinationId;
        this.amount = amount;
    }

    public String getDestinationId() { return destinationId; }

    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public int getAmount() { return amount; }

    public void setAmount(int amount) { this.amount = amount; }
}