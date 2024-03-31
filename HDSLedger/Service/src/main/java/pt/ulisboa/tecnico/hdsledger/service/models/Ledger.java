package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Ledger {
    private ArrayList<Block> ledger;
    // id -> account;
    private Map<String, Account> accounts;
    private Map<String, Account> accountDangerZone;
    // consensus instance -> id -> signature (DS.sign(value, node_pk))
    private Map<Integer, Map<String, String>> signatures;

    public Ledger() {
        ledger = new ArrayList<>();
        accounts = new ConcurrentHashMap<>();
        accountDangerZone = new ConcurrentHashMap<>();
        signatures = new ConcurrentHashMap<>();
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

    public Map<String, String> getSignatures(int consensusInstance) {
        signatures.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        return signatures.get(consensusInstance);
    }

    public void addSignature(int consensusInstance, String id, String signedBlock) {
        signatures.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        signatures.get(consensusInstance).put(id, signedBlock);
    }
}
