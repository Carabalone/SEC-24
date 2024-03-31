package pt.ulisboa.tecnico.hdsledger.service.services;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;

public class BlockchainService implements UDPService {
    private static final CustomLogger LOGGER = new CustomLogger(BlockchainService.class.getName());

    private final ProcessConfig selfConfig;

    private final Link nodesLink;

    private final Link clientsLink;

    private final ProcessConfig[] nodesConfig;

    private final ProcessConfig[] clientsConfig;

    private final NodeService nodeService;

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

    private void sendTransferResponse(LedgerRequestTransfer ledgerRequest, int requestId) {
        Optional<Account> sourceAccountOpt = nodeService.getLedger().getAccount(ledgerRequest.getSenderId());
        Optional<Account> destinationAccountOpt = nodeService.getLedger().getAccount(ledgerRequest.getDestinationId());

        sourceAccountOpt.ifPresentOrElse(sourceAccount -> {
            destinationAccountOpt.ifPresentOrElse(destinationAccount -> {

                LedgerResponseTransfer ledgerResponse = new LedgerResponseTransfer(this.selfConfig.getId(), sourceAccount.getBalance(), destinationAccount.getBalance());
                sendResponse(ledgerResponse, ledgerRequest.getSenderId(), requestId, Message.Type.TRANSFER);
                this.setConsensusReached(false);

            }, () -> { throw new HDSSException(ErrorMessage.CannotFindAccount); });
        }, () -> { throw new HDSSException(ErrorMessage.CannotFindAccount); });

    }

/*    public void append(LedgerRequest message) {
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
    }*/

    public void checkBalance(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestBalance ledgerRequest = message.deserializeBalance();

        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        weakBalanceRead(ledgerRequest, message);
    }

    public void weakBalanceRead(LedgerRequestBalance ledgerRequest, LedgerRequest message) {
        long balance = nodeService.getLedger().getAccount(ledgerRequest.getClientId())
                .orElseThrow(() -> new HDSSException(ErrorMessage.CannotFindAccount))
                .getBalance();

        LedgerResponseBalance ledgerResponse = new LedgerResponseBalance(this.selfConfig.getId(),
                                balance, nodeService.getLastDecidedConsensusInstance(),
                                nodeService.getLedger().getSignatures(nodeService.getLastDecidedConsensusInstance()));

        sendResponse(ledgerResponse, message.getSenderId(), message.getRequestId(), Message.Type.BALANCE);
    }

    public void strongBalanceRead(LedgerRequestBalance ledgerRequest, LedgerRequest message) {
        ProcessConfig clientToCheckConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(ledgerRequest.getClientId())).findFirst().get();

    }

    public void transfer(LedgerRequest message) {
        ProcessConfig clientConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(message.getSenderId())).findFirst().get();
        LedgerRequestTransfer ledgerRequest = message.deserializeTransfer();

        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        if (!DigitalSignature.verifySignature(String.valueOf(ledgerRequest.getAmount()), ledgerRequest.getSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        Block blockToAppend = new Block();
        blockToAppend.addRequest(message);
        this.nodeService.startConsensus(blockToAppend);
        while (!consensusReached);
        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached");

        sendTransferResponse(ledgerRequest, message.getRequestId());
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
                                    //append((LedgerRequest) message);
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
