package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerResponseTransfer extends Message {
    private int sourceBalance;

    private int destinationBalance;


    public LedgerResponseTransfer(String senderId, int sourceBalance, int destinationBalance) {
        super(senderId, Type.REPLY);
        this.sourceBalance = sourceBalance;
        this.destinationBalance = destinationBalance;
    }

    public int getSourceBalance() { return sourceBalance; }

    public void setSourceBalance(int sourceBalance) { this.sourceBalance = sourceBalance; }

    public int getDestinationBalance() { return destinationBalance; }

    public void setDestinationBalance(int destinationBalance) { this.destinationBalance = destinationBalance; }
}