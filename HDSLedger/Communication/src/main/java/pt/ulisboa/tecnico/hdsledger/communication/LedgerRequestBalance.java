package pt.ulisboa.tecnico.hdsledger.communication;

import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;

import java.security.PublicKey;


public class LedgerRequestBalance extends Message {
    private String clientKey;

    private String clientId;


    public LedgerRequestBalance(Type type, String senderId, String clientId, PublicKey clientKey) {
        super(senderId, type);
        this.clientId = clientId;
        this.clientKey = DigitalSignature.encodePublicKey(clientKey);
    }

    public PublicKey getClientKey() {
        return DigitalSignature.decodePublicKey(clientKey);
    }

    public void setClientKey(PublicKey clientKey) {
        this.clientKey = DigitalSignature.encodePublicKey(clientKey);
    }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }
}