package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {

    public ProcessConfig() {}

    public enum FailureType {
        NONE("NONE"),
        CRASH("CRASH"),
        BYZANTINE("BYZANTINE"),
        MESSAGE_DELAY("MESSAGE_DELAY");

        String type;
        FailureType(String type) {
            this.type = type;
        }
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public void setFailureType(FailureType failureType) {
        this.failureType = failureType;
    }

    private FailureType failureType;

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private String publicKeyPath;

    private String privateKeyPath;

    private int clientPort;

    public boolean isLeader() {
        return isLeader;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPublicKeyPath() { return publicKeyPath; }

    public String getPrivateKeyPath() { return privateKeyPath; }

    public int getClientPort() { return clientPort; }

    protected void setClientPort(int clientPort) { this.clientPort = clientPort; }
}
