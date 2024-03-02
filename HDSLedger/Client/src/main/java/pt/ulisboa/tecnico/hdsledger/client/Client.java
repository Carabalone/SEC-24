package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.library.Library;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private static final String nodePath = "../Service/src/main/resources/";
    private static final String clientPath = "src/main/resources/";


    public Client() {
    }

    private static void help() {
        System.out.println("List of commands: ");
        System.out.println("append <string_to_append>");
    }

    public static final void main(String[] args) throws HDSSException {
        try {

            System.out.println("Working Directory = " + System.getProperty("user.dir"));

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
            final Library library = new Library(config.get(), nodes, clients, false);
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
                        break;
                    }
                    case "append" -> {
                        if (terms.length < 2)
                            System.out.println("bad input, usage: append <string_to_append>");
                        System.out.println("appending " + terms[1]);
                        library.append(terms[1]);
                    }

                    case "ping" -> {
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
