package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Ledger {
    private ArrayList<Block> ledger;
    // id -> accounts;
    private Map<String, Account> accounts;
    private Map<String, Account> accountDangerZone;

    public Ledger() {
        ledger = new ArrayList<>();
        accounts = new ConcurrentHashMap<>();
        accountDangerZone = new ConcurrentHashMap<>();
    }

    public void addBlock(Block block) {
        ledger.add(block);
    }

    public void addAccount(Account account) {
        accounts.putIfAbsent(account.getId(), account);
    }

    public Optional<Account> getAccount(String id) {
        Account acc = accounts.get(id);
        return acc == null ?
                Optional.empty() :
                Optional.of(acc);
    }

    public void addBlockAt(int place, Block block) {
        ledger.add(place, block);
    }

    public void ensureCapacity(int capacity) {
        ledger.ensureCapacity(capacity);
    }

    public ArrayList<Block> getLedger() {
        return ledger;
    }

    public int size() {
        return ledger.size();
    }
}
