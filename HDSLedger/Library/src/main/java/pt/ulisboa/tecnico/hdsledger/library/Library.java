package pt.ulisboa.tecnico.hdsledger.library;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Library {

    private static final CustomLogger LOGGER = new CustomLogger(Library.class.getName());

    // Nodes configs
    private final ProcessConfig[] nodeConfigs;
    // Client identifier (self)
    private final ProcessConfig config;

    // Link to communicate with blockchain nodes
    private final Link link;

    // we broadcast a request to all nodes and wait for their responses
    private final Map<Integer, List<Message>> responses = new ConcurrentHashMap<>();
    // Messages sent by the client {nonce -> request}
    private final Map<Integer, Message> requests = new ConcurrentHashMap<>();

    private AtomicInteger requestId = new AtomicInteger(0);

    private List<String> blockchain = new ArrayList<>();


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs) {
        this(clientConfig, nodeConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, boolean activateLogs) throws HDSSException {

        this.config = clientConfig;
        this.nodeConfigs = nodeConfigs;

        // Create link to communicate with nodes
        System.out.println("[LIBRARY] creating link");
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000, true);
    }

    private void addResponse(int requestId, Message response) {
        List<Message> responses = this.responses.get(requestId);
        if (responses == null) {
            responses = new ArrayList<>();
            this.responses.put(requestId, responses);
        }
        responses.add(response);
    }

    public List<String> append(String value) {
        int clientRequestId = this.requestId.getAndIncrement();

        LedgerRequest request = new LedgerRequest(Message.Type.APPEND, this.config.getId(), clientRequestId, value, this.blockchain.size());
        this.link.broadcast(request);

        LedgerResponse ledgerResponse;
        while (!responses.containsKey(clientRequestId) || (responses.containsKey(clientRequestId) && responses.get(clientRequestId).isEmpty())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ledgerResponse = (LedgerResponse) responses.get(clientRequestId).get(0);

        // Add new values to the blockchain
        List<String> blockchainValues = ledgerResponse.getValues();

        // we send the whole ledger from the node to the client
        // we could send only the new values, but as of right now I don't know if there would be any
        // consistency problems with that option, so for now we just replace the whole blockchain
        blockchain = ledgerResponse.getValues();
        //blockchain.addAll(ledgerResponse.getValues().stream().toList());

        responses.remove(clientRequestId);

        // log responses after removal

        return blockchainValues;
    }

    public void checkBalance() {
        System.out.println("Checking balance");
        int clientRequestId = this.requestId.getAndIncrement();

        LedgerRequestBalance request = new LedgerRequestBalance(Message.Type.BALANCE, this.config.getId(), clientRequestId, this.blockchain.size());
        this.link.broadcast(request);

        System.out.printf("LIBRARY: Sent balance request\n");
        LedgerResponseBalance ledgerResponse;
        System.out.printf("LIBRARY: Waiting for balance response\n");
        while (!responses.containsKey(clientRequestId) || (responses.containsKey(clientRequestId) && responses.get(clientRequestId).isEmpty())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("LIBRARY: Received balance response\n");

        ledgerResponse = (LedgerResponseBalance) responses.get(clientRequestId).get(0);
        ledgerResponse.getBalance();
        System.out.printf("[LIBRARY]: MY BALANCE IS: %d\n", ledgerResponse.getBalance());
    }

    public void ping() {
        System.out.println("Broadcasting ping");
        link.broadcast(new Message(config.getId(), Message.Type.PING));
    }


    /*
    *  this listen to responses from the blockchain, not from the client
    */

    public void listen() {
        try {
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = link.receive();
                        System.out.printf("[LIBRARY] RECEIVED MESSAGE: %s\n", message.getClass());

                        switch (message.getType()) {
                            case ACK ->
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK {1} message from {2}",
                                        config.getId(), message.getMessageId(), message.getSenderId()));

                            case REPLY -> {
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received REPLY message from {1}",
                                        config.getId(), message.getSenderId()));

                                LOGGER.log(Level.INFO, MessageFormat.format("Message content: {0}", message.getSenderId(), message.getType()));

                                if (message instanceof LedgerResponse) {
                                    LedgerResponse response = (LedgerResponse) message;
                                    addResponse(response.getRequestId(), response);
                                    // there is a delay here. We add the response and wee need to wait until the value is actually added by the append
                                    // method that is waiting for the response to be added.
                                    // maybe we should refactor this.
                                    try {
                                        Thread.sleep(150); // just for debug purposes
                                    }   catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("LIBRARY: Added values to the blockhain: " + response.getValues().get(response.getValues().size() - 1));
                                    System.out.printf("LIBRARY: Blockchain: %s\n", blockchain);
                                }

/*                                if (message instanceof LedgerResponseBalance) {
                                    LedgerResponseBalance response = (LedgerResponseBalance) message;
                                    addResponse(response.getRequestId(), response);
                                    System.out.printf("LIBRARY: Balance response: %d\n", response.getBalance());
                                }*/

                            }

                            default -> {
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received unknown {1} message from {2}",
                                        config.getId(), message.getType(), message.getSenderId()));
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} Continuing as normal...", config.getId()));

                            }
                        }
                    }
                }

                catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}