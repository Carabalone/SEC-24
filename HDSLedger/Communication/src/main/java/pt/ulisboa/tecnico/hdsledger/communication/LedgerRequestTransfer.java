package pt.ulisboa.tecnico.hdsledger.communication;


public class LedgerRequestTransfer extends Message {

    private String destinationId;

    private long amount;

    private String amountSignature;

    private String destinationIdSignature;


    public LedgerRequestTransfer(Type type, String senderId, String destinationId, long amount, String amountSignature, String destinationIdSignature) {
        super(senderId, type);
        this.destinationId = destinationId;
        this.amount = amount;
        this.amountSignature = amountSignature;
        this.destinationIdSignature = destinationIdSignature;
    }

    public String getDestinationId() { return destinationId; }

    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public long getAmount() { return amount; }

    public void setAmount(long amount) { this.amount = amount; }

    public String getAmountSignature() { return amountSignature; }

    public void setAmountSignature(String amountSignature) { this.amountSignature = amountSignature; }

    public String getDestinationIdSignature() { return destinationIdSignature; }

    public void setDestinationIdSignature(String destinationIdSignature) { this.destinationIdSignature = destinationIdSignature; }
}