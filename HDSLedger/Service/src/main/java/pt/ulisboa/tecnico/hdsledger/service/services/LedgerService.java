package pt.ulisboa.tecnico.hdsledger.service.services;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ErrorMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerService implements UDPService {

    private static final CustomLogger LOGGER = new CustomLogger(LedgerService.class.getName());
    // Clients configurations
    private final ProcessConfig[] clientConfigs;
    // Link to communicate with client nodes
    private final Link link;
    // Node configuration
    private final ProcessConfig config;
    // Node service that provides consensus interface
    private final NodeService service;
    // Leader configuration
    private final ProcessConfig leaderConfig;
    // Used for BYZANTINE_TESTS
    private ProcessConfig censoredClient = null;

    public LedgerService(ProcessConfig[] clientConfigs, Link link, ProcessConfig config,
                         NodeService service, ProcessConfig leaderConfig) {
        this.clientConfigs = clientConfigs;
        this.link = link;
        this.config = config;
        this.service = service;
        this.leaderConfig = leaderConfig;
    }

    /*
     * Verifies if the client signature is valid by matching the sender id
     * public key with the signature inside the request
     *
     * @param request LedgerRequest to verify
     */
    private boolean verifyClientSignature(LedgerRequest request) {

        // Find config of the sender
        Optional<ProcessConfig> clientConfig = Arrays.stream(this.clientConfigs)
                .filter(c -> c.getId().equals(request.getSenderId())).findFirst();
        if (clientConfig.isEmpty())
            throw new HDSSException(ErrorMessage.ClientNotFound);

        // Verify client action was signed by him
        if (DigitalSignature.verifySignature(request.getMessage(), request.getClientSignature(),
                clientConfig.get().getPublicKeyPath()))
            return true;

        LOGGER.log(Level.INFO, MessageFormat.format(
                "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"
                        + "@          WARNING: INVALID CLIENT SIGNATURE!        @\n"
                        + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"
                        + "IT IS POSSIBLE THAT NODE {0} IS DOING SOMETHING NASTY!",
                request.getSenderId()));
        return false;
    }

    public void ping(LedgerRequest request) {
        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received Ping from {1}", this.config.getId(),
                        request.getSenderId(), request.getMessage()));

        if (!verifyClientSignature(request)) {
            return;
        }

    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every request
            // This is not thread safe but it's okay because
            // a client only sends one request at a time
            // thread listening for client requests on clientPort {Append, Read}
            new Thread(() -> {
                try {
                    while (true) {

                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {
                                case PING ->{
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received PING message from {1}",
                                                    config.getId(), message.getSenderId()));
                                    System.out.println("Received ping from: " + message.getSenderId());
                                    ping((LedgerRequest) message);
                                }

                                case ACK -> LOGGER.log(Level.INFO,
                                        MessageFormat.format("{0} - Received ACK message from {1}",
                                                this.config.getId(), message.getSenderId()));

                                case IGNORE -> LOGGER.log(Level.INFO,
                                        MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                this.config.getId(), message.getSenderId()));

                                default -> throw new HDSSException(ErrorMessage.CannotParseMessage);
                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}