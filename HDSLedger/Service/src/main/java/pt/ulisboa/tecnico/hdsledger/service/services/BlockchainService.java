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
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestAppend ledgerRequest = message.deserializeAppend();

        if (DigitalSignature.verifySignature(ledgerRequest.getValue(), message.getClientSignature(), clientConfig.getPublicKeyPath())) {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is valid. Starting consensus...");
            this.nodeService.startConsensus(ledgerRequest.getValue());
        } else {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is invalid");
            return;
        }

        while (!consensusReached);

        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached");

        LedgerResponseAppend ledgerResponse = getLedgerResponse();
        String serializedResponse = new Gson().toJson(ledgerResponse);
        LedgerResponse response = new LedgerResponse(Message.Type.REPLY, Message.Type.APPEND, selfConfig.getId(), serializedResponse, message.getRequestId());
        clientsLink.send(message.getSenderId(), response);
        System.out.printf("[BLOCKCHAIN SERVICE]: Sent response with request Id %s\n", response.getRequestId());
        this.setConsensusReached(false);
    }

    public void checkBalance(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestBalance ledgerRequest = message.deserializeBalance();

        if (DigitalSignature.verifySignature(message.getClientSignature(), message.getMessage(), clientConfig.getPublicKeyPath())) {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is valid. Checking balance...");
        } else {
            System.out.println("[BLOCKCHAIN SERVICE]: Signature is invalid");
            return;
        }

        LedgerResponseBalance ledgerResponse = new LedgerResponseBalance(this.selfConfig.getId(),
                Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get().getBalance());
        String serializedResponse = new Gson().toJson(ledgerResponse);
        LedgerResponse response = new LedgerResponse(Message.Type.REPLY, Message.Type.BALANCE, selfConfig.getId(), serializedResponse, message.getRequestId());
        clientsLink.send(message.getSenderId(), response);
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
                                    checkBalance((LedgerRequest) message);
                                }

                                case TRANSFER -> {
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - BLOCKCHAIN SERVICE: Received transfer request from {1}",
                                            selfConfig.getId(), message.getSenderId()));
                                    //transfer((LedgerRequest) message);
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

    private LedgerResponseAppend getLedgerResponse() {
        int consensusInstance = this.nodeService.getConsensusInstance();

        LedgerResponseAppend ledgerResponse = new LedgerResponseAppend(this.selfConfig.getId(),
                consensusInstance,
                this.nodeService.getLastCommitedValue());

        ledgerResponse.setType(Message.Type.REPLY);
        ledgerResponse.setValues(new ArrayList<>(this.nodeService.getLedger()));
        return ledgerResponse;
    }
}
