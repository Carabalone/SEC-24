package pt.ulisboa.tecnico.hdsledger.library;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs) {
        this(clientConfig, nodeConfigs, clientConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs,
                   boolean activateLogs) throws HDSSException {

        this.nodeConfigs = nodeConfigs;
        this.clientConfigs = clientConfigs;
        this.config = clientConfig;

        this.allConfigs = new ProcessConfig[nodeConfigs.length + clientConfigs.length];
        System.arraycopy(nodeConfigs, 0, this.allConfigs, 0, nodeConfigs.length);
        System.arraycopy(clientConfigs, 0, this.allConfigs, nodeConfigs.length, clientConfigs.length);

        // Create link to communicate with nodes
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000);
    }

    /*
     * Creates a new account in the ledger
     * The request is not blocking and the response is received asynchronously
     * The request will be sent to a small quorum of nodes that includes the leader
     * and will wait for a single response
     */
    public void append(String stringToAppend) {
        PublicKey accountPubKey;
        try {
            accountPubKey = DigitalSignature.readPublicKey(this.config.getPublicKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.FailedToReadPublicKey);
        }

        // Each LedgerRequest receives a specific ledger request which is serialized and
        // signed
        LedgerRequestAppend requestCreate = new LedgerRequestAppend(accountPubKey);
        String serializedCreateRequest = new Gson().toJson(requestCreate);
        String signature;
        try {
            signature = DigitalSignature.sign(serializedCreateRequest, config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        // Send generic ledger request
        LedgerRequest request = new LedgerRequest(this.config.getId(), Message.Type.APPEND, serializedCreateRequest,
                signature);

        // Add to pending requests map
        //this.requests.put(currentNonce, request);
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
            case APPEND -> {
                System.out.printf("TODO: LOGGAR ISSO AQUI");
            }

            default -> {
                LOGGER.log(Level.INFO, MessageFormat.format(
                        "{0} - Unknown request type {1}",
                        config.getId(), request.getType()));
            }
        }
    }

    /*
     * Verify if the signatures within a LedgerResponse are all valid and have the
     * minimum size of the small quorum
     *
     * @param response LedgerResponse to verify
     */

    public void listen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {

                        // Receive message from nodes
                        Message message = link.receive();

                        switch (message.getType()) {
                            case ACK -> {
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK {1} message from {2}",
                                        config.getId(), message.getMessageId(), message.getSenderId()));
                                continue;
                            }
                            case IGNORE -> {
                                LOGGER.log(Level.INFO,
                                        MessageFormat.format("{0} - Received IGNORE {1} message from {2}",
                                                config.getId(), message.getMessageId(), message.getSenderId()));
                                continue;
                            }
                            case REPLY -> {
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received REPLY message from {1}",
                                        config.getId(), message.getSenderId()));
                            }
                            default -> {
                                throw new HDSSException(ErrorMessage.CannotParseMessage);
                            }
                        }

                        LedgerResponse response = (LedgerResponse) message;
                    }
                } catch (HDSSException e) {
                    e.printStackTrace();
                    LOGGER.log(Level.INFO,
                            MessageFormat.format("{0} - EXCEPTION: {1}", config.getId(), e.getMessage()));
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}