package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import java.util.List;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private ProcessConfig config;
    private ProcessConfig[] nodes;
    private ProcessConfig[] clients;

    public final static void main(String[] args) {
        String clientId = args[0];

        System.out.println("I'm alive: " + clientId);
    }
}
