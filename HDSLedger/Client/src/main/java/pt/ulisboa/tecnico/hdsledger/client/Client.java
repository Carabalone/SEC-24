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
        System.out.println("Type 'balance <clientId>' to check your balance.");
        System.out.println("Type 'transfer <ammount> <destinationId>' to transfer value to another client.");
    }

    public static final void main(String[] args) throws HDSSException {
        try {
            String clientId = args[0];
            String configOption = args[1];

            ProcessConfig[] nodes, clients;

            nodes = new ProcessConfigBuilder().fromFile(nodePath + configOption);
            clients = new ProcessConfigBuilder().fromFile(clientPath + configOption);
            Optional<ProcessConfig> config = Arrays.stream(clients).filter(c -> c.getId().equals(clientId)).findFirst();

            if (config.isEmpty()) throw new HDSSException(ErrorMessage.ClientNotFound);

            Scanner scanner = new Scanner(System.in);

            System.out.println("I'm alive! " + clientId);
            System.out.println("You can start pressing commands: ");
            Client.help();

            final Library library = new Library(config.get(), nodes, clients, false);
            library.listen();

            while (true) {
                System.out.print("$ ");

                String line = scanner.nextLine().trim();
                String[] terms = line.split("\\s+");

                String command = terms[0];

                switch (command) {

                    case "help" -> help();

                    case "append" -> {
                        if (terms.length < 2)
                            System.out.println("bad input, usage: append <string_to_append>");
                        System.out.println("Sent request to append" + terms[1] + " to the blockchain");
                        library.append(terms[1]);
                    }

                    case "balance" -> {
                        if (terms.length < 2)
                            System.out.println("bad input, usage: balance <clientId>");
                        System.out.println("Sent request to check balance");
                        library.checkBalance(terms[1]);
                    }

                    case "transfer" -> {
                        if (terms.length < 3)
                            System.out.println("bad input, usage: transfer <value> <destination>");
                        System.out.println("Sent request to transfer " + terms[1] + " to " + terms[2]);
                        library.transfer(Integer.parseInt(terms[1]), terms[2]);
                    }

                    case "wait" -> System.out.println("waiting for " + terms[1] + " Milliseconds");

                    case "testTimer" -> {
                        System.out.println("Starting timer test");

                        HDSTimer timer1 = new HDSTimer();
                        timer1.subscribe(config.get().getId(), () -> System.out.println("Timer 1 has expired!"));
                        System.out.println("Starting timer 1 for 2 seconds");
                        timer1.start(1);

                        HDSTimer timer2 = new HDSTimer();
                        timer2.subscribe(config.get().getId(), () -> System.out.println("Timer 2 has expired!"));
                        timer2.start(2);
                        System.out.println("Starting timer 2 for 4 seconds");

                        HDSTimer timer3 = new HDSTimer();
                        timer3.subscribe(config.get().getId(), () -> System.out.println("Timer 3 has expired!"));
                        timer3.start(3);
                        System.out.println("Starting timer 3 for 8 seconds");

                        HDSTimer timer4 = new HDSTimer();
                        timer4.subscribe(config.get().getId(), () -> System.out.println("Timer 4 has expired!"));
                        timer4.start(4);
                        System.out.println("Starting timer 4 for 16 seconds, stopping it in 6");

                        Thread.sleep(6 * 1000); // wait 6 seconds
                        System.out.println("Stopping timer 4. Lets wait 16 seconds to see if it really stopped");
                        timer4.stop();
                        Thread.sleep(16 * 1000); // making sure timer would have expired without stopping
                        System.out.println("16 seconds passed, timer 4 would have expired by now, lets restart it for 4 seconds");
                        timer4.startOrRestart(2);
                        Thread.sleep(5 * 1000);

                    }

                    case "testTimer2" -> {
                        System.out.println("Starting timer 2 test");

                        HDSTimer timer1 = new HDSTimer();
                        timer1.subscribe(config.get().getId(), () -> System.out.println("Timer 1 has expired!"));
                        System.out.println("Starting timer for 2 seconds");
                        timer1.start(1);

                        Thread.sleep(3 * 1000); // wait 3 seconds

                        System.out.println("Retarting timer for 4 seconds");
                        timer1.startOrRestart(2);

                        Thread.sleep(5 * 1000);

                        System.out.println("Restarting timer for 4 seconds");
                        timer1.startOrRestart(2);
                        System.out.println("Restarting timer for 2 seconds");
                        timer1.startOrRestart(1);
                        Thread.sleep(2 * 1000);

                        System.out.println("restarting timer for 8 seconds, waiting for 4");
                        timer1.startOrRestart(3);
                        Thread.sleep(4 * 1000);
                        timer1.stop();

                        System.out.println("Ended test.");

                    }

                    default -> System.out.println("unrecognized command");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
