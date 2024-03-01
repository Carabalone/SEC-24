package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

import java.util.List;
import java.util.Scanner;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private final String nodePath = "../Service/src/main/java/resources/regular_config.json";
    private final String clientPath = "src/main/java/resources/regular_config.json";

    private ProcessConfig config;
    private ProcessConfig[] nodes;
    private ProcessConfig[] clients;

    public Client() {
        nodes = new ProcessConfigBuilder().fromFile(nodePath);
        clients = new ProcessConfigBuilder().fromFile(clientPath);
    }

    private static void help() {
        System.out.println("List of commands: ");
        System.out.println("append <string_to_append>");
    }

    public final static void main(String[] args) {
        String clientId = args[0];

        Scanner scanner = new Scanner(System.in);

        System.out.println("I'm alive! " + clientId);

        System.out.println("You can start pressing commands: ");
        Client.help();

        while (true) {
            System.out.print("$ ");

            String line = scanner.nextLine().trim();
            String[] terms = line.split("\\s+");

            if (terms.length < 1)
                System.out.println("Input Something.");

            String command = terms[0];

            switch (command) {
                case "help" -> {
                    help();
                    break;
                }
                case "append" -> {
                    if (terms.length < 2 )
                        System.out.println("bad input, usage: append <string_to_append>");
                    System.out.println("appending " + terms[1]);
                }
                default -> {
                    System.out.println("unrecognized command");
                }
            }

        }
    }
}
