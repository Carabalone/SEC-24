package pt.ulisboa.tecnico.hdsledger.library;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class Library {

    private static final CustomLogger LOGGER = new CustomLogger(Library.class.getName());

    // Client configs
    private final ProcessConfig[] clientConfigs;
    // Nodes configs
    private final ProcessConfig[] nodeConfigs;
    // All configs (client + nodes)
    private final ProcessConfig[] allConfigs;
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


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs) {
        this(clientConfig, nodeConfigs, clientConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs,
                   boolean activateLogs) throws HDSSException {

        this.config = clientConfig;
        this.nodeConfigs = nodeConfigs;
        this.clientConfigs = clientConfigs;

        this.allConfigs = new ProcessConfig[nodeConfigs.length + clientConfigs.length];
        System.arraycopy(nodeConfigs, 0, this.allConfigs, 0, nodeConfigs.length);
        System.arraycopy(clientConfigs, 0, this.allConfigs, nodeConfigs.length, clientConfigs.length);

        // Create link to communicate with nodes
        System.out.println("[LIBRARY] creating link");
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000);
    }

    public List<String> append(String value) {
        int clientRequestId = this.requestId.getAndIncrement();

        String signature;
        try {
            signature = DigitalSignature.sign(value, config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest request = new LedgerRequest(this.config.getId(), Message.Type.REQUEST, value, signature);
        request.setClientSignature(signature);
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
     * Find id by public key
     *
     * @param publicKey Public key
     */
    private String findIdByPublicKey(PublicKey publicKey) {
        for (ProcessConfig config : this.allConfigs) {
            try {
                PublicKey accountPubKey = DigitalSignature.readPublicKey(config.getPublicKeyPath());
                if (accountPubKey.equals(publicKey)) {
                    return config.getId();
                }
            } catch (Exception e) {
                throw new HDSSException(ErrorMessage.FailedToReadPublicKey);
            }
        }
        return null;
    }

    /*
     * Log request response to console
     *
     * @param request Request
     *
     * @param response Response
     *
     * @param isSuccessful True if the request was successful
     */
    private void logRequestResponse(LedgerRequest request, LedgerResponse response, boolean isSuccessful,
                                    boolean isValid) {
        switch (request.getType()) {
            case APPEND -> System.out.printf("TODO: LOGGAR ISSO AQUI");

            case PING -> System.out.println("Received pong from" + request.getSenderId());

            default ->
                LOGGER.log(Level.INFO, MessageFormat.format(
                        "{0} - Unknown request type {1}",
                        config.getId(), request.getType()));
        }
    }

    /*
     * Verify if the signatures within a LedgerResponse are all valid and have the
     * minimum size of the small quorum
     *
     * @param response LedgerResponse to verify
     */

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

                            case REPLY ->
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received REPLY message from {1}",
                                        config.getId(), message.getSenderId()));

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