package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerResponseBalance extends Message {

    private int balance;


    public LedgerResponseBalance(String senderId, int balance) {
        super(senderId, Type.REPLY);
        this.balance = balance;
    }

    public int getBalance() { return balance; }

    public void setBalance(int balance) { this.balance = balance; }
}