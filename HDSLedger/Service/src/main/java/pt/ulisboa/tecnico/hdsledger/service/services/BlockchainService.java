package pt.ulisboa.tecnico.hdsledger.service.services;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.DigitalSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

public class BlockchainService implements UDPService {
    private static final CustomLogger LOGGER = new CustomLogger(BlockchainService.class.getName());

    private final ProcessConfig selfConfig;

    private final Link nodesLink;

    private final Link clientsLink;

    private final ProcessConfig[] nodesConfig;

    private final NodeService nodeService;

    private final ProcessConfig[] clientsConfig;

    private volatile boolean consensusReached;

    public BlockchainService(Link nodesLink, Link clientsLink, ProcessConfig selfConfig,
                             ProcessConfig[] nodesConfig, ProcessConfig[] clientsConfig,
                             NodeService nodeService) {

        this.nodesLink = nodesLink;
        this.clientsLink = clientsLink;
        this.selfConfig = selfConfig;
        this.nodesConfig = nodesConfig;
        this.clientsConfig = clientsConfig;
        this.nodeService = nodeService;
        this.consensusReached = false;
    }

    public boolean isConsensusReached() {
        return consensusReached;
    }

    public synchronized void setConsensusReached(boolean consensusReached) {
        this.consensusReached = consensusReached;
    }

    public void append(LedgerRequest message) {

        // get client that made the request
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestAppend ledgerRequest = message.deserializeAppend();

        if (DigitalSignature.verifySignature(ledgerRequest.getValue(), ledgerRequest.getClientSignature(), clientConfig.getPublicKeyPath())) {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is valid. Starting consensus...");
            this.nodeService.startConsensus(ledgerRequest.getValue());
        } else {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is invalid");
            return;
        }

        while (!consensusReached);

        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached");

        LedgerResponseAppend ledgerResponse = getLedgerResponse(ledgerRequest);
        String serializedResponse = new Gson().toJson(ledgerResponse);
        LedgerResponse response = new LedgerResponse(Message.Type.REPLY, Message.Type.APPEND, selfConfig.getId(), serializedResponse);
        clientsLink.send(message.getSenderId(), response);
        this.setConsensusReached(false);
    }

    // this is blocking
    // receive library requests and does consensus stuff
    @Override
    public void listen() {
        try {
            new Thread(() -> {
                while (true)  {
                    try {
                        // receba
                        Message message = clientsLink.receive();

                        new Thread(() -> {
                            switch (message.getType())  {

                                case APPEND -> {
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - BLOCKCHAIN SERVICE: Received append request from {1}",
                                            selfConfig.getId(), message.getSenderId()));
                                    append((LedgerRequest) message);
                                }

                                case BALANCE -> {
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - BLOCKCHAIN SERVICE: Received balance request from {1}",
                                            selfConfig.getId(), message.getSenderId()));

                                    LedgerResponseBalance ledgerResponse = new LedgerResponseBalance(this.selfConfig.getId(),
                                                    Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get().getBalance(), message.getMessageId());

                                    ledgerResponse.setType(Message.Type.REPLY);
                                    System.out.printf("[BLOCKCHAIN SERVICE]: Sending balance response to %s\n", message.getSenderId());
                                    System.out.printf("[BLOCKCHAIN SERVICE]: Balance is %d\n", ledgerResponse.getBalance());
                                    System.out.printf("[BLOCKCHAIN SERVICE]: LEDGER RESPONSE: %s\n", ledgerResponse.getClass());
                                    clientsLink.send(message.getSenderId(), ledgerResponse);
                                }

                                case PING -> {
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - BLOCKCHAIN SERVICE: Received ping request from {1}",
                                            selfConfig.getId(), message.getSenderId()));
                                    //nodeService.ping();
                                    //clientsLink.send(message.getSenderId(), new Message(selfConfig.getId(), Message.Type.PING));
                                }
                            }
                            System.out.println("BLOCKCHAIN SERVICE: Finished processing message");
                        }).start();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LedgerResponseAppend getLedgerResponse(LedgerRequestAppend message) {
        int consensusInstance = this.nodeService.getConsensusInstance();

        LedgerResponseAppend ledgerResponse = new LedgerResponseAppend(this.selfConfig.getId(),
                consensusInstance,
                this.nodeService.getLastCommitedValue(),
                message.getRequestId());

        ledgerResponse.setType(Message.Type.REPLY);
        ledgerResponse.setValues(new ArrayList<>(this.nodeService.getLedger()));
        return ledgerResponse;
    }
}
