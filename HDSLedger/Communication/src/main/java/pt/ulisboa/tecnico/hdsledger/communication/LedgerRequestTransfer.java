package pt.ulisboa.tecnico.hdsledger.communication;

import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;

import java.security.PublicKey;

public class LedgerRequestTransfer {
    private String sourcePubKey;

    private String destinationPubKey;

    private int amount;


    public LedgerRequestTransfer(PublicKey sourcePubKey, PublicKey destinationPubKey, int amount) {
        this.sourcePubKey = DigitalSignature.encodePublicKey(sourcePubKey);
        this.destinationPubKey = DigitalSignature.encodePublicKey(destinationPubKey);
        this.amount = amount;
    }

    public PublicKey getSourcePubKey() {
        return DigitalSignature.decodePublicKey(this.sourcePubKey);
    }

    public void setSourcePubKey(PublicKey sourcePubKey) {
        this.sourcePubKey = DigitalSignature.encodePublicKey(sourcePubKey);
    }

    public PublicKey getDestinationPubKey() {
        return DigitalSignature.decodePublicKey(this.destinationPubKey);
    }

    public void setDestinationPubKey(PublicKey destinationPubKey) {
        this.destinationPubKey = DigitalSignature.encodePublicKey(destinationPubKey);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}