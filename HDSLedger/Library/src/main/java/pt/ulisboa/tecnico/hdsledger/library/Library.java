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

    private final ProcessConfig[] nodeConfigs;

    private final ProcessConfig[] clientConfigs;


    // Client identifier (self)
    private final ProcessConfig config;

    // Link to communicate with blockchain nodes
    private final Link link;

    // we broadcast a request to all nodes and wait for their responses
    private final Map<Integer, List<Message>> responses = new ConcurrentHashMap<>();

    // Messages sent by the client {nonce -> request}
    private final Map<Integer, Message> requests = new ConcurrentHashMap<>();

    private AtomicInteger requestId = new AtomicInteger(0);

    private List<String> blockchain = new ArrayList<>();


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs) {
        this(clientConfig, nodeConfigs, clientConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs, boolean activateLogs) throws HDSSException {
        this.config = clientConfig;
        this.nodeConfigs = nodeConfigs;
        this.clientConfigs = clientConfigs;

        // Create link to communicate with nodes
        System.out.println("[LIBRARY] creating link");
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000, true);
    }

    private void addResponse(int requestId, Message response) {
        List<Message> responses = this.responses.get(requestId);
        if (responses == null) {
            responses = new ArrayList<>();
            this.responses.put(requestId, responses);
        }
        responses.add(response);
    }

    public List<String> append(String value) {
        int clientRequestId = this.requestId.getAndIncrement();
        String valueSignature, requestSignature;

        try {
            valueSignature = DigitalSignature.sign(value, this.config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequestAppend request = new LedgerRequestAppend(Message.Type.APPEND, this.config.getId(), value, this.blockchain.size(), valueSignature);
        String serializedRequest = new Gson().toJson(request);

        try {
            requestSignature = DigitalSignature.sign(serializedRequest, this.config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.APPEND, clientRequestId, serializedRequest, requestSignature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: \n", ledgerRequest.getMessageId());
        waitForMinSetOfResponses(ledgerRequest.getRequestId());

        LedgerResponse ledgerResponse = (LedgerResponse) responses.get(clientRequestId).get(0);
        LedgerResponseAppend ledgerResponseAppend = ledgerResponse.deserializeAppend();

        // Add new values to the blockchain
        List<String> blockchainValues = ledgerResponseAppend.getValues();

        // we send the whole ledger from the node to the client
        // we could send only the new values, but as of right now I don't know if there would be any
        // consistency problems with that option, so for now we just replace the whole blockchain
        blockchain = ledgerResponseAppend.getValues();
        //blockchain.addAll(ledgerResponse.getValues().stream().toList());

        responses.remove(clientRequestId);

        return blockchainValues;
    }

    // Mandar email pro Sidnei pq o enunciado n faz sentido querer a public key do destino
    public PublicKey getPublicKey(String accountId) {
        ProcessConfig accountConfig = Arrays.stream(this.clientConfigs).filter(c -> c.getId().equals(accountId)).findFirst().get();

        PublicKey accountPubKey;
        try {
            accountPubKey = DigitalSignature.readPublicKey(accountConfig.getPublicKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.FailedToReadPublicKey);
        }

        return accountPubKey;
    }

    // Pensar melhor no caso bizantino
    public void checkBalance(String clientId) {
        int clientRequestId = this.requestId.getAndIncrement();
        PublicKey clientPublicKey = getPublicKey(clientId);

        LedgerRequestBalance request = new LedgerRequestBalance(Message.Type.BALANCE, this.config.getId(), clientId, clientPublicKey);
        String serializedRequest = new Gson().toJson(request);
        String signature;

        try {
            signature = DigitalSignature.sign(serializedRequest, this.config.getPrivateKeyPath());
        }  catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.BALANCE, clientRequestId, serializedRequest, signature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: \n", request.getMessageId());
        waitForMinSetOfResponses(ledgerRequest.getRequestId());

        LedgerResponse ledgerResponse = (LedgerResponse) responses.get(clientRequestId).get(0);
        LedgerResponseBalance ledgerResponseBalance = ledgerResponse.deserializeBalance();
        ledgerResponseBalance.getBalance();
        System.out.printf("[LIBRARY] Balance of clientId %s: is %d\n", clientId, ledgerResponseBalance.getBalance());
    }

    public void transfer(int amount, String destinationId) {
        int clientRequestId = this.requestId.getAndIncrement();
        String requestSignature, amountSignature;

        try {
            amountSignature = DigitalSignature.sign(String.valueOf(amount), this.config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequestTransfer request = new LedgerRequestTransfer(Message.Type.TRANSFER, this.config.getId(), destinationId, amount, amountSignature);
        String serializedRequest = new Gson().toJson(request);

        try {
            requestSignature = DigitalSignature.sign(serializedRequest, this.config.getPrivateKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.TRANSFER, clientRequestId, serializedRequest, requestSignature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: \n", request.getMessageId());
        waitForMinSetOfResponses(ledgerRequest.getRequestId());

        LedgerResponse ledgerResponse = (LedgerResponse) responses.get(clientRequestId).get(0);
        LedgerResponseTransfer ledgerResponseTransfer = ledgerResponse.deserializeTransfer();
        int sourceBalance = ledgerResponseTransfer.getSourceBalance();
        int destinationBalance = ledgerResponseTransfer.getDestinationBalance();

        System.out.println("[LIBRARY] Transferred " + amount + " to " + destinationId);
        System.out.printf("[LIBRARY] My balance: %d\n", sourceBalance);
        System.out.printf("[LIBRARY] Destination Balance: %d\n", destinationBalance);
    }

    public void waitForMinSetOfResponses(int requestId) {
        while (!responses.containsKey(requestId) ||
                (responses.containsKey(requestId) && responses.get(requestId).isEmpty()) ||
                responses.get(requestId).size() < (nodeConfigs.length / 3 + 1)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleAppendRequest(LedgerResponse response) {
        addResponse(response.getRequestId(), response);

        // there is a delay here. We add the response and wee need to wait until the value is actually added by the append
        // method that is waiting for the response to be added.
        // maybe we should refactor this.
        try {
            Thread.sleep(150); // just for debug purposes
        }   catch (InterruptedException e) {
            e.printStackTrace();
        }
        //System.out.println("LIBRARY: Added values to the blockhain: " + response.getValues().get(response.getValues().size() - 1));
        System.out.printf("LIBRARY: Blockchain: %s\n", blockchain);
    }

    // Esses 2 handles tao no fds aqui, no futuro pode fazer sentido separar mas 99% de certeza que nao
    public void handleBalanceRequest(LedgerResponse response) {
        addResponse(response.getRequestId(), response);
    }

    public void handleTransferRequest(LedgerResponse response) { addResponse(response.getRequestId(), response);}

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

                                LedgerResponse response = (LedgerResponse) message;

                                if (response.getTypeOfSerializedMessage() == Message.Type.APPEND) handleAppendRequest(response);
                                if (response.getTypeOfSerializedMessage() == Message.Type.BALANCE) handleBalanceRequest(response);
                                if (response.getTypeOfSerializedMessage() == Message.Type.TRANSFER) handleTransferRequest(response);
                            }

                            default -> {
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received unknown {1} message from {2}",
                                        config.getId(), message.getType(), message.getSenderId()));
                                LOGGER.log(Level.INFO, MessageFormat.format("{0} Continuing as normal...", config.getId()));

                            }
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