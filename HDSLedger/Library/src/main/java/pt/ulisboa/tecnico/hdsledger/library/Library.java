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

public class Library {

    private static final CustomLogger LOGGER = new CustomLogger(Library.class.getName());

    // Nodes configs
    private final ProcessConfig[] nodeConfigs;
    // Client identifier (self)
    private final ProcessConfig config;

    // Link to communicate with blockchain nodes
    private final Link link;

    // Responses received to specific nonce request {nonce -> responses[]}
    private final Map<Integer, List<LedgerResponse>> responses = new ConcurrentHashMap<>();
    // Messages sent by the client {nonce -> request}
    private final Map<Integer, LedgerRequest> requests = new ConcurrentHashMap<>();

    private AtomicInteger requestId = new AtomicInteger(0);

    private final List<String> blockchain = new ArrayList<>();


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

    public List<String> append(String value) {
        int clientRequestId = this.requestId.getAndIncrement();

        LedgerRequest request = new LedgerRequest(Message.Type.APPEND, this.config.getId(), clientRequestId, value, this.blockchain.size());
        this.link.broadcast(request);

        LedgerResponse ledgerResponse;
        while ((ledgerResponse = (LedgerResponse) responses.get(clientRequestId)) == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Add new values to the blockchain
        List<String> blockchainValues = ledgerResponse.getValues();
        blockchain.addAll(ledgerResponse.getValues().stream().toList());

        responses.remove(clientRequestId);
        return blockchainValues;
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
                                    blockchain.add(response.getValues().get(0));
                                    System.out.printf("LIBRARY: Blockchain: %s\n", blockchain);
                                    System.out.println("LIBRARY: Added values to the blockhain: " + response.getValues().get(0));
                                }
                            }

                            default -> throw new HDSSException(ErrorMessage.CannotParseMessage);
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