package pt.ulisboa.tecnico.hdsledger.service.models;

import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;


public class Signature {

    ConcurrentHashMap<String, Long> balances;

    Block block;

    public Signature(Block block, ConcurrentHashMap<String, Long> balances) {
        this.block = block;
        this.balances = balances;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
