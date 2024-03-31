package pt.ulisboa.tecnico.hdsledger.utilities;

public enum ErrorMessage {
    ConfigFileNotFound("The configuration file is not available at the path supplied"),
    ConfigFileFormat("The configuration file has wrong syntax"),
    NoSuchNode("Can't send a message to a non existing node"),
    SocketSendingError("Error while sending message"),
    CannotOpenSocket("Error while opening socket"),
    UnableToSignMessage("Could not sign the message. Check if the key path is correct"),
    InvalidSignature("Invalid signature, the message was tampered with"),
    FailedToReadPublicKey("Could not read the public key"),
    CannotParseMessage("Could not parse the message"),
    ClientNotFound("Client not found"),
    CannotTransferNegativeAmount("Cannot transfer a negative amount"),
    CannotTransferToSelf("Cannot transfer to self"),
    InsufficientFunds("Insufficient funds"),
    CannotFindAccount("Cannot Find account"),
    CannotTransferToNode("Cannot transfer to a node");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
