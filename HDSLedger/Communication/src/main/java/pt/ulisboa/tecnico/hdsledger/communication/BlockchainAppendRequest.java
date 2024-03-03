package pt.ulisboa.tecnico.hdsledger.communication;

import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;

import java.security.PublicKey;

public class BlockchainAppendRequest extends Message {

    // Account Public Key
    private String accountPubKey;

    private String data;

    public BlockchainAppendRequest(PublicKey accountPubKey, String data, String senderId) {
        super(senderId, Type.APPEND);
        this.data = data;
        this.accountPubKey = DigitalSignature.encodePublicKey(accountPubKey);
    }

    public PublicKey getAccountPubKey() {
        return DigitalSignature.decodePublicKey(this.accountPubKey);
    }

    public void setAccountPubKey(PublicKey accountPubKey) {
        this.accountPubKey = DigitalSignature.encodePublicKey(accountPubKey);
    }

    public String getString() {
        return data;
    }
}