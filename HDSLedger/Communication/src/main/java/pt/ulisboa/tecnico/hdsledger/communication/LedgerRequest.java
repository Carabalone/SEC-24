package pt.ulisboa.tecnico.hdsledger.communication;

public class LedgerRequest extends Message {

    // Serialized request
    private String message;
    // Signature of amount with client's private key
    private String clientSignature;

    public LedgerRequest(String senderId, Type type, String message, String signature) {
        super(senderId, type);
        this.message = message;
        this.clientSignature = signature;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClientSignature() {
        return clientSignature;
    }

    public void setClientSignature(String clientSignature) {
        this.clientSignature = clientSignature;
    }

    @Override
    public String toString() {
        return "LedgerRequest [message=" + message + ", clientSignature=" + clientSignature + ", getSenderId()="
                + getSenderId() + ", getType()=" + getType() + ", getMessageId()=" + getMessageId() + "]";
    }

    @Override
    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof LedgerRequest)) {
            return false;
        }
        LedgerRequest request = (LedgerRequest) o;
        return request.getMessage().equals(message) && request.getClientSignature().equals(clientSignature) &&
                request.getSenderId().equals(getSenderId()) && request.getType().equals(getType()) &&
                request.getMessageId() == getMessageId();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}