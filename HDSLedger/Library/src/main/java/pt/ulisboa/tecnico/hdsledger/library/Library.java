package pt.ulisboa.tecnico.hdsledger.library;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.security.PublicKey;
import java.sql.SQLOutput;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    private Map<String, Long> balanceCache = new ConcurrentHashMap<String, Long>();


    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs) {
        this(clientConfig, nodeConfigs, clientConfigs, true);
    }

    public Library(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs, ProcessConfig[] clientConfigs, boolean activateLogs) throws HDSSException {
        this.config = clientConfig;
        this.nodeConfigs = nodeConfigs;
        this.clientConfigs = clientConfigs;
        Arrays.stream(clientConfigs).forEach(c -> balanceCache.put(c.getId(), (long) c.getBalance()));
        Arrays.stream(nodeConfigs).forEach(c -> balanceCache.put(c.getId(), (long) c.getBalance()));

        // Create link to communicate with nodes
        System.out.println("[LIBRARY] creating link");
        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, LedgerResponse.class,
                activateLogs, 5000, true);
    }

    private void addResponse(int requestId, Message response) {
        synchronized (this.responses) {
            List<Message> responses = this.responses.get(requestId);
            if (responses == null) {
                responses = new ArrayList<>();
                this.responses.put(requestId, responses);
            }
            responses.add(response);
        }
    }

    public Optional<PublicKey> getPublicKey(String accountId) {
        Optional<ProcessConfig> accountConfig = findConfig(accountId);

        if (accountConfig.isEmpty()) return Optional.empty();

        PublicKey accountPubKey;
        try {
            accountPubKey = DigitalSignature.readPublicKey(accountConfig.get().getPublicKeyPath());
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.FailedToReadPublicKey);
        }

        return Optional.of(accountPubKey);
    }

    public void checkBalance(String clientId, LedgerRequestBalance.Consistency consistency) {
        if (findConfig(clientId).isEmpty()) {
            System.out.println("Client does not exist, not continuing operation");
            return;
        }

        Optional<PublicKey> clientPublicKey = getPublicKey(clientId);

        if (clientPublicKey.isEmpty()) {
            System.out.println("Receiver public key does not exist, not continuing operation");
            return;
        }

        int clientRequestId = this.requestId.getAndIncrement();

        LedgerRequestBalance request = new LedgerRequestBalance(Message.Type.BALANCE, this.config.getId(), clientId,
                clientPublicKey.get(), consistency
        );
        String serializedRequest = new Gson().toJson(request);
        String signature;

        try {
            signature = DigitalSignature.sign(serializedRequest, this.config.getPrivateKeyPath());
        }  catch (Exception e) {
            throw new HDSSException(ErrorMessage.UnableToSignMessage);
        }

        LedgerRequest ledgerRequest = new LedgerRequest(this.config.getId(), Message.Type.BALANCE, clientRequestId, serializedRequest, signature);
        this.link.broadcast(ledgerRequest);

        System.out.printf("[LIBRARY] WAITING FOR MINIMUM SET OF RESPONSES FOR REQUEST: " + request.getMessageId() + "\n");
        waitForMinSetOfResponses(ledgerRequest.getRequestId());

        // check if any of the responses is valid and return that balance

        synchronized (responses) {

            int amountOfResponses = responses.get(clientRequestId).size();

            Optional<LedgerResponseBalance> response = responses.get(clientRequestId).stream()
                    .map(r -> (LedgerResponse) r)
                    .filter(r -> r.getTypeOfSerializedMessage() == Message.Type.BALANCE)
                    .map(r -> r.deserializeBalance())
                    .filter(r -> verifyBalanceResponse(r))
                    .findAny();

            long balance;

            if (response.isPresent()) {
                balance = response.get().getBalance();
                balanceCache.put(clientId, balance);
            } else {
                balance = balanceCache.get(clientId);
                System.out.println("Not enough signatures, returning cached value");
            }

            System.out.printf("[LIBRARY] Balance of clientId %s: is %d\n", clientId, balance);
        }

    }

    public void transfer(int amount, String destinationId) {
        int clientRequestId = this.requestId.getAndIncrement();
        String requestSignature, amountSignature;

        if (findConfig(destinationId).isEmpty()) {
            System.out.println("Destination does not exist, not continuing operation");
            return;
        }


        if (destinationId.equals(this.config.getId()) && this.config.getFailureType() != ProcessConfig.FailureType.GREEDY_CLIENT) {
            System.out.println("Cannot transfer to self");
            return;
        }
        else if (this.config.getFailureType() == ProcessConfig.FailureType.GREEDY_CLIENT) {
            destinationId.equals(this.config.getId());
            System.out.printf("[LIBRARY] Greedy client, transferring to self\n");
        }

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

        System.out.printf("[LIBRARY] WAITING UNTIL BLOCK IS DECIDED: \n", request.getMessageId());
        waitForMinSetOfResponses(ledgerRequest.getRequestId());

        LedgerResponse ledgerResponse = (LedgerResponse) responses.get(clientRequestId).get(0);
        LedgerResponseTransfer ledgerResponseTransfer = ledgerResponse.deserializeTransfer();
        long sourceBalance = ledgerResponseTransfer.getSourceBalance();
        long destinationBalance = ledgerResponseTransfer.getDestinationBalance();
        long payedFee = ledgerResponseTransfer.getPayedFee();

        System.out.println("[LIBRARY] Transferred " + amount + " to " + destinationId);
        System.out.println("[LIBRARY] Payed fee to block producer: " + payedFee);
        System.out.printf("[LIBRARY] My balance: %d\n", sourceBalance);
        System.out.printf("[LIBRARY] Destination Balance: %d\n", destinationBalance);
    }

    Optional<ProcessConfig> findConfig(String id) {
        Optional<ProcessConfig> node = Arrays.stream(nodeConfigs).filter(config -> config.getId().equals(id)).findFirst();
        Optional<ProcessConfig> client = Arrays.stream(clientConfigs).filter(config -> config.getId().equals(id)).findFirst();

        return node.isPresent() ? node : client;
    }
    Optional<ProcessConfig> findNodeConfig(String nodeId) {
        return Arrays.stream(nodeConfigs).filter(config -> config.getId().equals(nodeId)).findFirst();
    }

    public boolean verifyBalanceResponse(LedgerResponseBalance response) {
        // node ID, signature of block
        Map<String, String> signatures = response.getSignatures();

        if (signatures.size() < 2 * maxFaults() + 1) {
            System.out.println("[BALANCE] - There are not enough signatures, not accepting");
            return false;
        }

        try {
            Map<String, Long> frequencyMap = signatures.entrySet().stream().map(entry -> {
                try {
                    String digest = Base64.getEncoder().encodeToString(
                            DigitalSignature.decrypt(
                                    Base64.getDecoder().decode(entry.getValue()),
                                    findNodeConfig(entry.getKey()).get().getPublicKeyPath()
                            )
                    );
                    System.out.println("Digest for node " + findNodeConfig(entry.getKey()).get() + " : " + digest);
                    return digest;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new HDSSException(ErrorMessage.FailedToReadPublicKey);
                }
            }).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            System.out.println("Frequency map length: " + frequencyMap.entrySet().size());

            return frequencyMap.entrySet().stream().anyMatch(entry -> entry.getValue() >= 2 * maxFaults() + 1);
        } catch (Exception e) {
            System.out.println("Exception when verifying balance response");
            e.printStackTrace();
            return false;
        }
    }

    private int maxFaults() {
        // we need 3f + 1 nodes to tolerate f faults
        // so n = 3f+1, f = (n-1)/3
        return (nodeConfigs.length - 1) / 3;
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
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (HDSSException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}