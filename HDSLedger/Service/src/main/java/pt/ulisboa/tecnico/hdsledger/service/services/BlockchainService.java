package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;
import pt.ulisboa.tecnico.hdsledger.communication.LedgerResponse;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
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
        if (this.selfConfig.isLeader()) {
            //this.nodeService.setTimer();
            this.nodeService.startConsensus(message.getValue());
        }

        while (!consensusReached);

        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached");
        LedgerResponse ledgerResponse = getLedgerResponse((LedgerRequest) message);
        clientsLink.send(message.getSenderId(), ledgerResponse);
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

    private LedgerResponse getLedgerResponse(LedgerRequest message) {
        int consensusInstance = this.nodeService.getConsensusInstance();

        LedgerResponse ledgerResponse = new LedgerResponse(this.selfConfig.getId(),
                consensusInstance,
                this.nodeService.getLastCommitedValue(),
                message.getRequestId());

        ledgerResponse.setType(Message.Type.REPLY);
        ledgerResponse.setValues(new ArrayList<>(this.nodeService.getLedger()));
        return ledgerResponse;
    }
}
