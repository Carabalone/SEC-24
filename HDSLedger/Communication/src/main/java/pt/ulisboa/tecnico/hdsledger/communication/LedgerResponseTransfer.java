package pt.ulisboa.tecnico.hdsledger.communication;


public class LedgerResponseTransfer extends Message {

    private long sourceBalance;

    private long destinationBalance;

    private long payedFee;


    public LedgerResponseTransfer(String senderId, long sourceBalance, long destinationBalance, long payedFee) {
        super(senderId, Type.REPLY);
        this.sourceBalance = sourceBalance;
        this.destinationBalance = destinationBalance;
        this.payedFee = payedFee;
    }

    public long getSourceBalance() { return sourceBalance; }

    public long getDestinationBalance() { return destinationBalance; }

    public long getPayedFee() { return payedFee; }
}