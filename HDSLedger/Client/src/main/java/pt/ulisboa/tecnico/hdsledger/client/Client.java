package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.library.Library;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

public class Client {
    private static final String nodePath = "../Service/src/main/resources/";
    private static final String clientPath = "src/main/resources/";

    private static void help() {
        System.out.println("Welcome to the Serenity Ledger");
        System.out.println("Type 'append <value>' to append a string to the blockchain.");
        System.out.println("Type 'ping' to ping all nodes.");
    }

    public static final void main(String[] args) throws HDSSException {
        try {
            String clientId = args[0];
            String configOption = args[1];

            ProcessConfig[] nodes, clients;

            nodes = new ProcessConfigBuilder().fromFile(nodePath + configOption);
            clients = new ProcessConfigBuilder().fromFile(clientPath + configOption);
            Optional<ProcessConfig> config = Arrays.stream(clients).filter(c -> c.getId().equals(clientId)).findFirst();

            if (config.isEmpty()) {
                throw new HDSSException(ErrorMessage.ClientNotFound);
            }

            Scanner scanner = new Scanner(System.in);

            System.out.println("I'm alive! " + clientId);
            System.out.println("You can start pressing commands: ");
            Client.help();

            // Library to interact with the blockchain
            final Library library = new Library(config.get(), nodes, false);
            library.listen();

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
                    }

                    case "append" -> {
                        if (terms.length < 2)
                            System.out.println("bad input, usage: append <string_to_append>");
                        System.out.println("appending string to the blockchain" + terms[1]);
                        library.append(terms[1]);
                    }

                    case "ping" -> {
                        System.out.printf("pinging all nodes, my id: %s\n", config.get().getId());
                        library.ping();
                    }

                    default -> {
                        System.out.println("unrecognized command");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
