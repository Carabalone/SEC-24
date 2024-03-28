package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {

    public ProcessConfig() {}

    public enum FailureType {
        NONE("NONE"),
        CRASH("CRASH"),
        BYZANTINE("BYZANTINE"),
        MESSAGE_DELAY("MESSAGE_DELAY"),
        SILENT_LEADER("SILENT_LEADER"),
        DROP("DROP"),
        DICTATOR_LEADER("DICTATOR_LEADER");

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

    public boolean hasFailureType(FailureType failureType) {
        return this.failureType == failureType;
    }

    private FailureType failureType;

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private String publicKeyPath;

    private String privateKeyPath;

    private int clientPort;

    private int balance;

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

    public int getBalance() { return balance; }

    public void setBalance(int balance) { this.balance = balance; }
}
