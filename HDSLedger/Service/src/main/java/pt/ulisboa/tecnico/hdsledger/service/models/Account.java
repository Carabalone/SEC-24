package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Account {
    public enum Type {
        NODE, CLIENT;
    }

    private final String id;

    private long balance;

    private Type type;


    public Account(String id, long balance, Type type) {
        this.id = id;
        this.balance = balance;
        this.type = type;
    }

    public boolean subtractBalance(long amount) {
        if (balance - amount < 0 ) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public void addBalance(long amount) {
        balance += amount;
    }

    public String getId() {
        return id;
    }

    public long getBalance() {
        return balance;
    }

    public Type getType() { return type; }
}
