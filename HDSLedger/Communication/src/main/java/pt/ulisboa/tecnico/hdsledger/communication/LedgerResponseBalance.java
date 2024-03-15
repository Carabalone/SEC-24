package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerResponseBalance extends Message {

    private int balance;

    private int requestId;

    public LedgerResponseBalance(String senderId, int balance, int requestId) {
        super(senderId, Type.REPLY);
        this.requestId = requestId;
        this.balance = balance;
    }


    public int getBalance() { return balance; }

    public void setBalance(int balance) { this.balance = balance; }

    public int getRequestId() {
        return requestId;
    }
}