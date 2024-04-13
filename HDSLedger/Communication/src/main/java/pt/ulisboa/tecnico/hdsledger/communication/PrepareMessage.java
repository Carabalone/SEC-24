package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.Objects;


public class PrepareMessage {

    private String block;

    private String leaderSignature;

    public PrepareMessage(String block/*, String leaderSignature*/) {
        this.block = block;
        this.leaderSignature = "TODO: tirar";
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) { this.block = block; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrepareMessage that = (PrepareMessage) o;
        System.out.println("block> " + Objects.equals(block, that.block));
        System.out.println("signature> " + Objects.equals(leaderSignature, that.leaderSignature));
        return Objects.equals(block, that.block) && Objects.equals(leaderSignature, that.leaderSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, leaderSignature);
    }

    public String getLeaderSignature() {
        return leaderSignature;
    }

    public void setLeaderSignature(String leaderSignature) {
        this.leaderSignature = leaderSignature;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}