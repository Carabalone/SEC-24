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

    private final Link clientsLink;

    private final ProcessConfig[] nodesConfig;

    private final ProcessConfig[] clientsConfig;

    private final NodeService nodeService;

    private volatile boolean consensusReached;

    private BlockPool blockPool;


    public BlockchainService(Link clientsLink, ProcessConfig selfConfig,
                             ProcessConfig[] nodesConfig, ProcessConfig[] clientsConfig,
                             NodeService nodeService, BlockPool blockPool) {

        this.clientsLink = clientsLink;
        this.selfConfig = selfConfig;
        this.nodesConfig = nodesConfig;
        this.clientsConfig = clientsConfig;
        this.nodeService = nodeService;
        this.blockPool = blockPool;
        this.consensusReached = false;
    }

    /** ************************************* */
    /** ********** CLIENT REQUESTS ********** */
    /** ************************************* */

    public void checkBalance(LedgerRequest message) {
        ProcessConfig clientConfig = getClientAccount(message.getSenderId());
        LedgerRequestBalance ledgerRequest = message.deserializeBalance();
        checkRequestSignature(message, clientConfig);
        weakBalanceRead(ledgerRequest, message);
    }

    public void weakBalanceRead(LedgerRequestBalance ledgerRequest, LedgerRequest message) {
        long balance = nodeService.getLedger().getAccount(ledgerRequest.getClientId())
                .orElseThrow(() -> new HDSSException(ErrorMessage.CannotFindAccount))
                .getBalance();

        LedgerResponseBalance ledgerResponse = new LedgerResponseBalance(this.selfConfig.getId(),
                                balance,
                                nodeService.getLedger().getSignatures(nodeService.getLastDecidedConsensusInstance()));

        sendResponse(ledgerResponse, message.getSenderId(), message.getRequestId(), Message.Type.BALANCE);
    }

    public void strongBalanceRead(LedgerRequestBalance ledgerRequest, LedgerRequest message) {
        if (ledgerRequest.getConsistency() != LedgerRequestBalance.Consistency.STRONG) {
            System.out.println("Strong read got weak read request, redirecting...");
            weakBalanceRead(ledgerRequest, message);
            return;
        }

        ProcessConfig clientToCheckConfig = Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(ledgerRequest.getClientId())).findFirst().get();

        Block blockToAppend = new Block();
        blockToAppend.addRequest(message);
        nodeService.startConsensus(blockToAppend);
        while (!consensusReached);
        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached! Sending response to client...");

        weakBalanceRead(ledgerRequest, message);
    }

    public void transfer(LedgerRequest message) {
        ProcessConfig clientConfig = getClientAccount(message.getSenderId());
        LedgerRequestTransfer ledgerRequest = message.deserializeTransfer();

        checkTransferRequestSignature(message, ledgerRequest, clientConfig);
        addRequestToPool(message);

        while (!consensusReached);
        System.out.println("[BLOCKCHAIN SERVICE]: Consensus reached! Sending response to client...");

        sendTransferResponse(ledgerRequest, message.getRequestId());
    }

    private void sendTransferResponse(LedgerRequestTransfer ledgerRequest, int requestId) {
        Optional<Account> sourceAccountOpt = nodeService.getLedger().getAccount(ledgerRequest.getSenderId());
        Optional<Account> destinationAccountOpt = nodeService.getLedger().getAccount(ledgerRequest.getDestinationId());

        sourceAccountOpt.ifPresentOrElse(sourceAccount -> {
            destinationAccountOpt.ifPresentOrElse(destinationAccount -> {

                LedgerResponseTransfer ledgerResponse = new LedgerResponseTransfer(
                        this.selfConfig.getId(), sourceAccount.getBalance(), destinationAccount.getBalance(),
                        nodeService.getFeeToBlockProducer()
                );
                sendResponse(ledgerResponse, ledgerRequest.getSenderId(), requestId, Message.Type.TRANSFER);
                this.setConsensusReached(false);

            }, () -> { throw new HDSSException(ErrorMessage.CannotFindAccount); });
        }, () -> { throw new HDSSException(ErrorMessage.CannotFindAccount); });
    }

    /** ************************************* */
    /** ********** HELPER FUNCTIONS ********** */
    /** ************************************* */

    public void sendResponse(Message responseOperation, String clientId, int requestId, Message.Type type) {
        String serializedResponse = new Gson().toJson(responseOperation);
        LedgerResponse response = new LedgerResponse(Message.Type.REPLY, type, selfConfig.getId(), serializedResponse, requestId);
        clientsLink.send(clientId, response);
    }

    private void startConsensusIfBlock(Optional<Block> block) {
        if (block.isEmpty()) return;
        this.nodeService.startConsensus(block.get());
    }

    public synchronized void setConsensusReached(boolean consensusReached) {
        this.consensusReached = consensusReached;
    }

    public ProcessConfig getClientAccount(String clientId) {
        return Arrays.stream(this.clientsConfig).filter(config -> config.getId().equals(clientId)).findFirst().get();
    }

    public void checkRequestSignature(LedgerRequest message, ProcessConfig clientConfig) {
        if (!DigitalSignature.verifySignature(message.getRequest(), message.getClientSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);
    }

    public void checkTransferRequestSignature(LedgerRequest message, LedgerRequestTransfer request, ProcessConfig clientConfig) {
        checkRequestSignature(message, clientConfig);

        if (!DigitalSignature.verifySignature(request.getDestinationId(), request.getDestinationIdSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);

        if (!DigitalSignature.verifySignature(String.valueOf(request.getAmount()), request.getAmountSignature(), clientConfig.getPublicKeyPath()))
            throw new HDSSException(ErrorMessage.InvalidSignature);
    }

    public void addRequestToPool(LedgerRequest message) {
        synchronized (blockPool) {
            blockPool.accept(queue -> {
                queue.add(message);
            });
            startConsensusIfBlock(blockPool.addRequest(message));
        }
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
        } catch (HDSSException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
