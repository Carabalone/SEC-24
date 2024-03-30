package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerResponseTransfer extends Message {

    private long sourceBalance;

    private long destinationBalance;


    public LedgerResponseTransfer(String senderId, long sourceBalance, long destinationBalance) {
        super(senderId, Type.REPLY);
        this.sourceBalance = sourceBalance;
        this.destinationBalance = destinationBalance;
    }

    public long getSourceBalance() { return sourceBalance; }

    public void setSourceBalance(long sourceBalance) { this.sourceBalance = sourceBalance; }

    public long getDestinationBalance() { return destinationBalance; }

    public void setDestinationBalance(long destinationBalance) { this.destinationBalance = destinationBalance; }
}