package pt.ulisboa.tecnico.hdsledger.service.services;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

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

    private LedgerResponseAppend getLedgerResponse() {
        int consensusInstance = this.nodeService.getConsensusInstance();

        LedgerResponseAppend ledgerResponse = new LedgerResponseAppend(this.selfConfig.getId(),
                consensusInstance, this.nodeService.getLastCommitedValue());

        ledgerResponse.setType(Message.Type.REPLY);
        ledgerResponse.setValues(new ArrayList<>(this.nodeService.getLedger()));
        return ledgerResponse;
    }

    public void append(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestAppend ledgerRequest = message.deserializeAppend();

        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        if (!DigitalSignature.verifySignature(ledgerRequest.getValue(), ledgerRequest.getSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        Block blockToAppend = new Block();
        blockToAppend.addRequest(message);
        this.nodeService.startConsensus(blockToAppend);
        while (!consensusReached);
        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached");

        LedgerResponseAppend ledgerResponse = getLedgerResponse();
        sendResponse(ledgerResponse, message.getSenderId(), message.getRequestId(), Message.Type.APPEND);
        this.setConsensusReached(false);
    }

    public void checkBalance(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestBalance ledgerRequest = message.deserializeBalance();

        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        balanceOperation(ledgerRequest, message);
    }

    public void balanceOperation(LedgerRequestBalance ledgerRequest, LedgerRequest message) {
        ProcessConfig clientToCheckConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(ledgerRequest.getClientId())).findFirst().get();

        LedgerResponseBalance ledgerResponse = new LedgerResponseBalance(this.selfConfig.getId(), clientToCheckConfig.getBalance());
        sendResponse(ledgerResponse, message.getSenderId(), message.getRequestId(), Message.Type.BALANCE);
    }

    public void transfer(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestTransfer ledgerRequest = message.deserializeTransfer();

        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        if (!DigitalSignature.verifySignature(String.valueOf(ledgerRequest.getAmount()), ledgerRequest.getSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        transferOperation(ledgerRequest, message.getRequestId());
    }

    public void transferOperation(LedgerRequestTransfer ledgerRequest, int requestId) {
        ProcessConfig sourceConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(ledgerRequest.getSenderId())).findFirst().get();
        ProcessConfig destinationConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(ledgerRequest.getDestinationId())).findFirst().get();
        int amount = ledgerRequest.getAmount();
        int sourceBalance = sourceConfig.getBalance();
        int destinationBalance = destinationConfig.getBalance();

        if (sourceConfig.getId().equals(destinationConfig.getId())) throw new HDSSException(ErrorMessage.CannotTransferToSelf);
        if (sourceBalance < amount) throw new HDSSException(ErrorMessage.InsufficientFunds);
        if (sourceBalance <= 0) throw new HDSSException(ErrorMessage.CannotTranferNegativeAmount);

        destinationConfig.setBalance(destinationBalance + amount);
        sourceConfig.setBalance(sourceBalance - amount);

        LedgerResponseTransfer ledgerResponse = new LedgerResponseTransfer(this.selfConfig.getId(), sourceConfig.getBalance(), destinationConfig.getBalance());
        sendResponse(ledgerResponse, ledgerRequest.getSenderId(), requestId, Message.Type.TRANSFER);
    }

    public void sendResponse(Message responseOperation, String clientId, int requestId, Message.Type type) {
        String serializedResponse = new Gson().toJson(responseOperation);
        LedgerResponse response = new LedgerResponse(Message.Type.REPLY, type, selfConfig.getId(), serializedResponse, requestId);
        clientsLink.send(clientId, response);
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
                                    transfer((LedgerRequest) message);
                                }
                            }
                            System.out.println("[BLOCKCHAIN SERVICE]: Finished processing message");
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
}
