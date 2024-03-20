package pt.ulisboa.tecnico.hdsledger.library;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class Library {

    private static final CustomLogger LOGGER = new CustomLogger(Library.class.getName());

    // Nodes configs
    private final ProcessConfig[] nodeConfigs;
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


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs) {
        this(clientConfig, nodeConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, boolean activateLogs) throws HDSSException {

        this.config = clientConfig;
        this.nodeConfigs = nodeConfigs;

        // Create link to communicate with nodes
        System.out.println("[LIBRARY] creating link");
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000, true);
    }

    private void addResponse(int requestId, Message response) {
        System.out.printf("[LIBRARY] Adding response to request: %d\n", requestId);
        List<Message> responses = this.responses.get(requestId);
        if (responses == null) {
            responses = new ArrayList<>();
            this.responses.put(requestId, responses);
        }
        responses.add(response);
        System.out.printf("[LIBRARY] Added response to request: %d\n", requestId);
    }

    public List<String> append(String value) {
        int clientRequestId = this.requestId.getAndIncrement();
        String signature;

        try {
            signature = DigitalSignature.sign(value, this.config.getPrivateKeyPath());
        }
        catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }


        LedgerRequestAppend request = new LedgerRequestAppend(Message.Type.APPEND, this.config.getId(), value, this.blockchain.size());
        String serializedRequest = new Gson().toJson(request);

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.APPEND, clientRequestId, serializedRequest, signature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: \n", ledgerRequest.getMessageId());
        waitForMinSetOfResponses(ledgerRequest.getMessageId());

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

    public void checkBalance() {
        int clientRequestId = this.requestId.getAndIncrement();

        LedgerRequestBalance request = new LedgerRequestBalance(Message.Type.BALANCE, this.config.getId(),  this.blockchain.size());
        String serializedRequest = new Gson().toJson(request);
        String signature;

        try {
            signature = DigitalSignature.sign(serializedRequest, this.config.getPrivateKeyPath());
        }
        catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.BALANCE, clientRequestId, serializedRequest, signature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: \n", request.getMessageId());
        waitForMinSetOfResponses(clientRequestId);

        LedgerResponseBalance ledgerResponse = (LedgerResponseBalance) responses.get(clientRequestId).get(0);
        ledgerResponse.getBalance();
        System.out.printf("[LIBRARY] MY BALANCE IS: %d\n", ledgerResponse.getBalance());
    }

    public void ping() {
        System.out.println("Broadcasting ping");
        link.broadcast(new Message(config.getId(), Message.Type.PING));
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

    public void handleBalanceRequest(LedgerResponse response) {
        LedgerResponseBalance responseBalance = response.deserializeBalance();
        addResponse(responseBalance.getRequestId(), response);
    }

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

                                if (message instanceof LedgerResponse) {
                                    LedgerResponse response = (LedgerResponse) message;

                                    if (response.getTypeOfSerializedMessage() == Message.Type.APPEND) handleAppendRequest(response);
                                    if (response.getTypeOfSerializedMessage() == Message.Type.BALANCE) handleBalanceRequest(response);
                                }
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