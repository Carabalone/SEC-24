package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LedgerResponseBalance extends Message {

    private long balance;

    private int lastConsensusInstance;
    private Map<String, String> signatures;

    public LedgerResponseBalance(String senderId, long balance, int lastConsensusInstance,
                                 Map<String, String> signatures) {
        super(senderId, Type.REPLY);
        this.balance = balance;
        this.lastConsensusInstance = lastConsensusInstance;
        this.signatures = signatures;
    }

    public long getBalance() { return balance; }

    public void setBalance(int balance) { this.balance = balance; }
    public Map<String, String> getSignatures() {
        return signatures;
    }

}