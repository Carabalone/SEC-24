package pt.ulisboa.tecnico.hdsledger.communication;

import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;

import java.security.PublicKey;

public class LedgerRequestAppend {

    // Account Public Key
    private String accountPubKey;

    public LedgerRequestAppend(PublicKey accountPubKey) {
        this.accountPubKey = DigitalSignature.encodePublicKey(accountPubKey);
    }

    public PublicKey getAccountPubKey() {
        return DigitalSignature.decodePublicKey(this.accountPubKey);
    }

    public void setAccountPubKey(PublicKey accountPubKey) {
        this.accountPubKey = DigitalSignature.encodePublicKey(accountPubKey);
    }
}