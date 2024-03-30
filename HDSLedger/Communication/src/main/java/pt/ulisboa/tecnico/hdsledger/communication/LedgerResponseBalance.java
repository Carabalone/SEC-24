package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerResponseBalance extends Message {

    private long balance;

    private int lastConsensusInstance;

    public LedgerResponseBalance(String senderId, long balance, int lastConsensusInstance) {
        super(senderId, Type.REPLY);
        this.balance = balance;
        this.lastConsensusInstance = lastConsensusInstance;
    }

    public long getBalance() { return balance; }

    public void setBalance(int balance) { this.balance = balance; }
}