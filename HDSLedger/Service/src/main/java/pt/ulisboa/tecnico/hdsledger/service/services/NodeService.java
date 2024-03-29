package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSTimer;
import pt.ulisboa.tecnico.hdsledger.utilities.Pair;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;


public class NodeService implements UDPService, HDSTimer.TimerListener {

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    // Current node is leader
    private final ProcessConfig config;
    // Leader configuration
    private ProcessConfig leaderConfig;

    // Link to communicate with nodes
    private final Link nodesLink;

    private final Link clientsLink;

    // Consensus instance -> Round -> List of prepare messages
    private final MessageBucket prepareMessages;
    // Consensus instance -> Round -> List of commit messages
    private final MessageBucket commitMessages;

    private final MessageBucket roundChangeMessages;

    // Store if already received pre-prepare for a given <consensus, round>
    private final Map<Integer, Map<Integer, Boolean>> receivedPrePrepare = new ConcurrentHashMap<>();
    // Consensus instance information per consensus instance
    private final Map<Integer, InstanceInfo> instanceInfo = new ConcurrentHashMap<>();
    // Current consensus instance
    private final AtomicInteger consensusInstance = new AtomicInteger(0);
    // Last decided consensus instance
    private final AtomicInteger lastDecidedConsensusInstance = new AtomicInteger(0);

    // Ledger (for now, just a list of strings)
    private ArrayList<String> ledger = new ArrayList<String>();

    private BlockchainService blockchainService;

    private ArrayList<String> lastCommitedValue = new ArrayList<>();

    // consensusInstance -> timer
    private final Map<Integer, HDSTimer> timers = new ConcurrentHashMap<>();

    // used for message delay failure type
    private int messageDelayCounter = 0;
    // used for testing of timers every 10 seconds
    private boolean started = false;

    public NodeService(Link nodesLink, Link clientsLink,
                       ProcessConfig config, ProcessConfig leaderConfig, ProcessConfig[] nodesConfig) {

        this.nodesLink = nodesLink;
        this.clientsLink = clientsLink;
        this.config = config;

        this.leaderConfig = config.hasFailureType(ProcessConfig.FailureType.DICTATOR_LEADER) ?
                config :
                leaderConfig;

        this.nodesConfig = nodesConfig;

        this.prepareMessages = new MessageBucket(nodesConfig.length);
        this.commitMessages = new MessageBucket(nodesConfig.length);
        this.roundChangeMessages = new MessageBucket(nodesConfig.length);
    }

    @Override
    public void onTimerExpired() {
        uponTimerExpire();
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public ArrayList<String> getLedger() {
        return this.ledger;
    }

    private boolean isLeader(String id) {
        return this.leaderConfig.getId().equals(id);
    }

    public BlockchainService getBlockchainService() {
        return this.blockchainService;
    }

    public void setBlockchainService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    public ConsensusMessage createConsensusMessage(String value, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .build();

        return consensusMessage;
    }

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(String value) {
        if (config.getFailureType() == ProcessConfig.FailureType.CRASH) {
            LOGGER.log(
                    Level.INFO,
                    MessageFormat.format("{0} Leader has failure and will not start consensus", config.getId())
            );
            return;
        }

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();

        LOGGER.log(Level.WARNING, MessageFormat.format("[NODE] got consensus {0}", localConsensusInstance));
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(value));

        // If startConsensus was already called for a given round
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    config.getId(), localConsensusInstance));
            return;
        }

        // Only start a consensus instance if the last one was decided
        // We need to be sure that the previous value has been decided
        while (lastDecidedConsensusInstance.get() < localConsensusInstance - 1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Leader broadcasts PRE-PREPARE message
        if (this.config.isLeader()) {
            if (config.getFailureType() == ProcessConfig.FailureType.SILENT_LEADER) {
                LOGGER.log(Level.INFO,
                        "[SILENT-LEADER] - Will not Broadcast Pre-Prepare..."
                );
                return;
            }

            InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);

            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));

            this.nodesLink.broadcast(this.createConsensusMessage(value, localConsensusInstance, instance.getCurrentRound()));
        }

        else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }

        startOrRestartTimer(localConsensusInstance, 1);
    }

    private void startOrRestartTimer(int instance, int round) {
        synchronized (timers) {
            HDSTimer timer = timers.get(instance);
            if (timer == null) {
                timer = new HDSTimer();
                timers.put(instance, timer);
            }
            timer.subscribe(config.getId(), this);
            timer.startOrRestart(round);
        }
    }

    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();
        int senderMessageId = message.getMessageId();

        PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();

        String value = prePrepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Verify if pre-prepare was sent by leader
        if (!isLeader(senderId)) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Received PRE-PREPARE message from {1} but not leader, ignoring",
                            config.getId(), senderId));
            return;
        }

        // Set instance value
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value));

        // ignore messages from previous rounds
        if (message.getRound() < instanceInfo.get(consensusInstance).getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "[PRE-PREPARE] Received Round {0} but round is lower than current round {1}",
                            round, instanceInfo.get(consensusInstance).getCurrentRound()));
            return;
        }

        if (!justifyPrePrepare(message)) {
            LOGGER.log(
                    Level.INFO,
                    MessageFormat.format(
                            "{0} - Pre-prepare message not justified, ignoring",
                            config.getId())
            );

            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        receivedPrePrepare.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        if (receivedPrePrepare.get(consensusInstance).put(round, true) != null) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));
        }

        PrepareMessage prepareMessage = config.hasFailureType(ProcessConfig.FailureType.LEADER_SPOOFING) ?
                new PrepareMessage("sporting") :
                new PrepareMessage(prePrepareMessage.getValue());

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                .setConsensusInstance(consensusInstance)
                .setRound(round)
                .setMessage(prepareMessage.toJson())
                .setReplyTo(senderId)
                .setReplyToMessageId(senderMessageId)
                .build();

        if (config.getFailureType() == ProcessConfig.FailureType.FAKE_LEADER) {
            /*
             * Because other nodes fail to verify the signature, they will not
             * reply with ACK meaning that this node will be stuck sending messages
             * forever
             */

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Byzantine Fake Leader", config.getId()));
            consensusMessage.setSenderId(this.leaderConfig.getId());
        }
        startOrRestartTimer(consensusInstance, round);
        this.nodesLink.broadcast(consensusMessage);
    }

    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public synchronized void uponPrepare(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        PrepareMessage prepareMessage = message.deserializePrepareMessage();

        String value = prepareMessage.getValue();

        // ignore messages from previous rounds
        if (message.getRound() < instanceInfo.get(this.consensusInstance.get()).getCurrentRound()) return;

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Doesn't add duplicate messages
        prepareMessages.addMessage(message);

        if (config.getFailureType() == ProcessConfig.FailureType.MESSAGE_DELAY && messageDelayCounter < 1) {
            messageDelayCounter++;

            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Sleeping because MESSAGE_DELAY failure",
                            config.getId()));

            try {
                Thread.sleep(5500); // making sure that the timer runs out
            } catch (Exception e) {
               e.printStackTrace();
            }
        }

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // ignore messages from previous rounds
        if (message.getRound() < instance.getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "[PREPARE] Received Round {0} but round is lower than current round {1}",
                            round, instance.getCurrentRound()));
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setMessage(instance.getCommitMessage().toJson())
                    .build();

            nodesLink.send(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<String> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {
            instance.setPreparedValue(preparedValue.get());
            instance.setPreparedRound(round);

            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = prepareMessages.getMessages(consensusInstance, round).values();

            System.out.println("[PREPARE] Received a prepare quorum for value " + value + ", Messages: ");
            sendersMessage.forEach(System.out::println);

            CommitMessage c = new CommitMessage(preparedValue.get());
            instance.setCommitMessage(c);

            sendersMessage.forEach(senderMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(senderMessage.getSenderId())
                        .setReplyToMessageId(senderMessage.getMessageId())
                        .setMessage(c.toJson())
                        .build();

                nodesLink.send(senderMessage.getSenderId(), m);
            });
        }
    }

    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public synchronized void uponCommit(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        // ignore messages from previous rounds
        if (message.getRound() < instanceInfo.get(this.consensusInstance.get()).getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "[COMMIT] Received Round {0} but round is lower than current round {1}",
                            round, instanceInfo.get(this.consensusInstance.get()).getCurrentRound()));
            return;
        }

        commitMessages.addMessage(message);
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // ignore messages from previous rounds
        if (instance == null || message.getRound() < instance.getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "[COMMIT] Received Round {0} but round is lower than current round {1}",
                            round, instance.getCurrentRound()));
            return;
        }

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), consensusInstance, round);
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }

        Optional<String> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);

        if (commitValue.isPresent() && instance.getCommittedRound() < round) {
            // stop timer
            synchronized (timers) {
                HDSTimer timer = timers.get(consensusInstance);
                if (timer != null) {
                    System.out.println("[TIMER] - STOPPING TIMER FOR INSTANCE " + consensusInstance);
                    timer.stop();
                }
            }

            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String value = commitValue.get();

            // Append value to the ledger (must be synchronized to be thread-safe)
            synchronized(ledger) {
                // Increment size of ledger to accommodate current instance
                ledger.ensureCapacity(consensusInstance);
                while (ledger.size() < consensusInstance - 1) {
                    ledger.add("");
                }
                
                ledger.add(consensusInstance - 1, value);
                setLastCommitedValue(value);
                
                LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Current Ledger: {1}",
                            config.getId(), String.join("", ledger)));
            }

            lastDecidedConsensusInstance.getAndIncrement();
            append(value);

            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            config.getId(), consensusInstance, round, true));
        }
    }

    private void setLastCommitedValue(String value) {
        if (lastCommitedValue.size() > 0)
            lastCommitedValue.remove(0);
        lastCommitedValue.add(value);
    }

    public ArrayList<String> getLastCommitedValue() {
        return lastCommitedValue;
    }

    public void append(String value) {
        setLastCommitedValue(value);
        this.blockchainService.setConsensusReached(true);
    }

    public void uponTimerExpire() {
        int localInstance = consensusInstance.get();

        InstanceInfo existingConsensus = this.instanceInfo.get(localInstance);

        LOGGER.log(Level.INFO,
                MessageFormat.format("[TIMER] Timer expired for Consensus Instance {0}, Round {1}",
                        localInstance, existingConsensus.getCurrentRound()));

        // ri ← ri + 1
        existingConsensus.setCurrentRound(existingConsensus.getCurrentRound() + 1);
        LOGGER.log(Level.INFO, MessageFormat.format(
                "{0} - Changed round to {1}", config.getId(), existingConsensus.getCurrentRound()
        ));

        // will change the round but does not want the leader to change, so will not broadcast round change.
        if (config.hasFailureType(ProcessConfig.FailureType.DICTATOR_LEADER)) {
            LOGGER.log(Level.INFO,
                    "[DICTATOR LEADER] Updated my round but will not broadcast round change or change leader");
            return;
        }

        updateLeader();

        int round = existingConsensus.getCurrentRound();
        LOGGER.log(Level.WARNING,
                MessageFormat.format("local Instance in Timer {0}", localInstance));

        LOGGER.log(Level.INFO,
                "[TIMER] - Started Timer in uponTimerExpire");
        startOrRestartTimer(localInstance, round);

        RoundChangeMessage roundChangeMessage = new RoundChangeMessage(localInstance, round,
                                                existingConsensus.getPreparedRound(),
                                                existingConsensus.getPreparedValue());

        existingConsensus.setRoundChangeMessage(roundChangeMessage);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                .setConsensusInstance(localInstance)
                .setRound(round)
                .setMessage(roundChangeMessage.toJson())
                .setReplyTo(config.getId())
                .build();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Broadcasting ROUND CHANGE message to round {1}",
                        config.getId(), round));
        nodesLink.broadcast(consensusMessage);
    }

    public int maxFaults() {
        return (nodesConfig.length - 1) / 3;
    }

    private void uponRoundChangeSet(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        InstanceInfo instance = instanceInfo.get(consensusInstance);

        roundChangeMessages.addMessage(message);

        // upon receiving a set Frc of f + 1 valid 〈ROUND-CHANGE, λi, rj , −, −〉 messages such
        // that ∀〈ROUND-CHANGE, λi, rj , −, −〉 ∈ Frc : rj > ri do
        //   let 〈ROUND-CHANGE, hi, rmin, −, −〉 ∈ Frc such that:
        //     ∀〈ROUND-CHANGE, λi, rj , −, −〉 ∈ Frc : rmin ≤ rj
        Collection<ConsensusMessage> messages = roundChangeMessages.getMessages(consensusInstance, round).values()
                .stream().filter(m -> m.getRound() > instance.getCurrentRound()).toList();

        // TODO: verify round & instance
        // TODO:

        LOGGER.log(Level.INFO, MessageFormat.format("Received {0}/{1} messages", messages.size(), maxFaults() + 1));
        if (messages.size() == maxFaults() + 1) {

            LOGGER.log(Level.INFO,
                    ("[RC] Entered first round Change predicate."));

            Optional<ConsensusMessage> selected = messages.stream()
                    .min(Comparator.comparingInt(ConsensusMessage::getRound));

            if (selected.isPresent()) {
                //  ri ← rmin
                //  set timeri to running and expire after t(ri)
                //  broadcast 〈ROUND-CHANGE, λi, ri, pri, pvi〉

                instance.setCurrentRound(selected.get().getRound());


                LOGGER.log(Level.INFO, MessageFormat.format(
                        "[RC] Got MIN round rj > ri: {0}", instance.getCurrentRound()
                ));

                updateLeader();

                startOrRestartTimer(consensusInstance, instance.getCurrentRound());

                // WARNING: idk if instance should be the local instance instead
                RoundChangeMessage roundChangeMessage = new RoundChangeMessage(consensusInstance, selected.get().getRound(),
                        instance.getPreparedRound(),
                        instance.getPreparedValue());

                instance.setRoundChangeMessage(roundChangeMessage);

                ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setMessage(roundChangeMessage.toJson())
                        .setReplyTo(config.getId())
                        .build();

                nodesLink.broadcast(consensusMessage);
            }
        }
    }

    private void uponRoundChangeQuorum(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        InstanceInfo instance = instanceInfo.get(consensusInstance);

        // upon receiving a quorum Qrc of valid <ROUND-CHANGE, λi, ri, _ , _ > messages such
        // that leader(λi, ri) = pi ∧ JustifyRoundChange(Qrc) do
        boolean existsRoundChangeQuorum = roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round);

        if (existsRoundChangeQuorum) {
            LOGGER.log(
                    Level.INFO,
                    "[RC] Entered second predicate."
            );
            Collection<ConsensusMessage> quorum = roundChangeMessages.getMessages(consensusInstance, round).values();

            LOGGER.log(Level.SEVERE,
                    MessageFormat.format(
                            "[RC] There exists a RC quorum on round {0}", round
                    ));


            if (isLeader(config.getId()) && justifyRoundChange(quorum)) {

                LOGGER.log(Level.INFO,
                        "[RC] Entered second predicate (There is a quorum) and I'm leader"
                );

                String value;

                // If HighestPrepared(Qrc) != ⊥
                if (highestPrepared(quorum).isPresent())
                    value = highestPrepared(quorum).get().getPreparedValue();
                else
                    value = instance.getInputValue();

                ConsensusMessage consensusMessage = this.createConsensusMessage(value, consensusInstance, round);

                //startOrRestartTimer(consensusInstance, round);

                this.nodesLink.broadcast(consensusMessage);
            }

            else {
                if (isLeader(config.getId())) {
                    LOGGER.log(Level.INFO,
                            "[RC] I'm leader but not justifying round change");

                    LOGGER.log(Level.INFO,
                            "[RC] lets see what messages we got in the quorum..."
                    );
                    Collection<ConsensusMessage> q = roundChangeMessages.getMessages(consensusInstance, round).values();

                    for (ConsensusMessage m : q) {
                        LOGGER.log(Level.INFO,
                                MessageFormat.format("[RC] Quorum message: {0}", m.getMessage())
                        );
                    }
                    return;
                }
                LOGGER.log(Level.INFO,
                        "[RC] not leader thus not doing anything"
                );
            }
        }

        else {
            LOGGER.log(Level.INFO,
                    "[RC] Entered second predicate (There is no quorum)."
            );
            LOGGER.log(Level.INFO,
                "[RC] there are " + roundChangeMessages.getMessages(consensusInstance, round).values().size() + " messages in the bucket."
            );

            if (isLeader(config.getId())) {
                LOGGER.log(Level.INFO,
                        "[RC] lets see what messages we got in the quorum..."
                );
                Collection<ConsensusMessage> quorum = roundChangeMessages.getMessages(consensusInstance, round).values();

                for (ConsensusMessage m : quorum) {
                    LOGGER.log(Level.INFO,
                            MessageFormat.format("[RC] Quorum message: {0}", m.getMessage())
                    );
                }
            }
        }
    }

    public void uponRoundChange(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        InstanceInfo instance = instanceInfo.get(consensusInstance);

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received ROUND_CHANGE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        uponRoundChangeSet(message);
        uponRoundChangeQuorum(message);
    }

    // assumes the quorum exists and receives it
    private boolean justifyRoundChange(Collection<ConsensusMessage> quorum) {
        // overview
        // receives: Quorum<RoundChangeMessage>
        // return
        //      ∀〈ROUND-CHANGE, λi, ri, prj , pvj 〉 ∈ Qrc : prj = ⊥ ∧ pvj = ⊥
        //      ∨ received a quorum of valid 〈PREPARE, λi, pr, pv〉 messages such that:
        //      (pr, pv) = HighestPrepared(Qrc)

//        boolean allTheSame = quorum.stream()
//                .map(ConsensusMessage::deserializeRoundChangeMessage)
//                .map(RoundChangeMessage::getConsensusInstance)
//                .distinct().count() == 1;

        LOGGER.log(
                Level.INFO,
                "[RC] Justifying Round Change"
        );

        var firstObject = quorum.stream().toList().get(0);
        if (firstObject == null) {
            LOGGER.log(Level.SEVERE, "[JUSTIFY RC] SHOULD NEVER HAPPEN: quorum is empty, returning false...");
            return false;
        }

        LOGGER.log(Level.SEVERE,
                "[JRC] There is a quorum for round " +firstObject.getRound() + "This are the messages"
                );

        quorum.forEach(System.out::println);

        int instance = firstObject.getConsensusInstance();

        boolean nullPredicate = quorum.stream()
                .map(ConsensusMessage::deserializeRoundChangeMessage)
                .allMatch(m -> m.getPreparedRound() == -1 && m.getPreparedValue() == null);

        LOGGER.log(
                Level.INFO,
                "[RC] Null predicate verified, result: " + nullPredicate
        );

        Optional<Pair<Integer, String>> highestPreparedPair = highestPrepared(quorum);

        if (highestPreparedPair.isEmpty()) {
            LOGGER.log(
                    Level.INFO,
                    "[RC] There is no Highest Prepared Pair\nJustified Round Change, result: " + nullPredicate);
            return nullPredicate;
        }

        int highestPreparedRound = highestPreparedPair.get().getPreparedRound();
        String highestPreparedValue = highestPreparedPair.get().getPreparedValue();

        Optional<String> existsPrepareQuorum = prepareMessages.hasValidPrepareQuorum(config.getId(), instance, highestPreparedRound);

        if (existsPrepareQuorum.isPresent()) {
            LOGGER.log(Level.INFO,
                    "[RC] There is a prepare quorum"
            );

            Collection<ConsensusMessage> prepareQuorum = prepareMessages.getMessages(instance, highestPreparedRound).values();

            //          ∨ received a quorum of valid 〈PREPARE, λi, pr, value〉 messages such that:
            //          (pr, value) = HighestPrepared(Qrc)
            boolean highestPreparedPredicate = prepareQuorum.stream()
                    .allMatch(m -> m.getRound() == highestPreparedRound &&
                            m.deserializePrepareMessage().getValue().equals(highestPreparedValue));

            boolean testPredicate = existsPrepareQuorum.get().equals(highestPreparedValue);

            LOGGER.log(Level.INFO, MessageFormat.format("[RC] HighestPReparedPRedicate is {0} and test predicate is {1}", highestPreparedPredicate, testPredicate));

            LOGGER.log(
                    Level.INFO,
                    MessageFormat.format("[RC] Highest Prepared Predicate verified, result: {0}", highestPreparedPredicate)
            );
            LOGGER.log(
                    Level.INFO,
                    "[RC] Justified Round Change, result: " + (nullPredicate || highestPreparedPredicate)
            );
            return nullPredicate || highestPreparedPredicate;
        }

        else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("[RC] There is NOT a prepare quorum, instance: {0} round = {1}, Messages:",
                           instance, highestPreparedRound
                    ));

            for (var m : prepareMessages.getMessages(instance, highestPreparedRound).values()) {
                LOGGER.log(Level.INFO,
                        MessageFormat.format("Prepare Quorum Message: {0}", m)
                );
            }
        }

        LOGGER.log(
                Level.INFO,
                "[RC] Justified Round Change, result: " + nullPredicate);
        return nullPredicate;
    }

    private boolean justifyPrePrepare(ConsensusMessage message) {
        // overview
        // return (
        //      round = 1 ∨
        //      received a quorum Qrc of valid 〈ROUND-CHANGE, λi, round, prj , pvj〉 messages
        //      such that:
        //          ∀〈ROUND-CHANGE, λi, round, prj , pvj 〉 ∈ Qrc : prj = ⊥ ∧ prj = ⊥
        //          ∨ received a quorum of valid 〈PREPARE, λi, pr, value〉 messages such that:
        //          (pr, value) = HighestPrepared(Qrc)

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        // return round = 1 or
        if (round == 1) return true;

        // begin nullPredicate
        // or received a quorum Qrc of valid 〈ROUND-CHANGE, λi, round, prj , pvj〉 messages
        boolean existsRoundChangeQuorum = roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round);

        if (existsRoundChangeQuorum) {
            Collection<ConsensusMessage> rcQuorum = roundChangeMessages.getMessages(consensusInstance, round).values();

            // such that ∀〈ROUND-CHANGE, λi, round, prj , pvj 〉 ∈ Qrc : prj = ⊥ ∧ prj = ⊥

            boolean nullPredicate = rcQuorum.stream()
                    .map(ConsensusMessage::deserializeRoundChangeMessage)
                    .allMatch(m -> (m.getPreparedRound() == -1 && m.getPreparedValue() == null));

            // end nullPredicate
            // begin highestPreparedPredicate

            Optional<Pair<Integer, String>> highestPreparedPair = highestPrepared(rcQuorum);
            if (highestPreparedPair.isEmpty())
                return nullPredicate;

            int highestPreparedRound = highestPreparedPair.get().getPreparedRound();
            String highestPreparedValue = highestPreparedPair.get().getPreparedValue();

            Optional<String> existsPrepareQuorum = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, highestPreparedRound);

            if (existsPrepareQuorum.isPresent()) {
                Collection<ConsensusMessage> prepareQuorum = prepareMessages.getMessages(consensusInstance, highestPreparedRound).values();

                    //          ∨ received a quorum of valid 〈PREPARE, λi, pr, value〉 messages such that:
                    //          (pr, value) = HighestPrepared(Qrc)
                    boolean highestPreparedPredicate = prepareQuorum.stream()
                            .allMatch(m -> m.getRound() == highestPreparedRound &&
                                      m.deserializePrepareMessage().getValue().equals(highestPreparedValue));

                    return nullPredicate || highestPreparedPredicate;
            }

            return nullPredicate;
            // end highestPreparedPredicate
        } else {
            LOGGER.log(Level.INFO,
                    "[JUSTIFY PRE PREPARE] - There is NOT a prepare quorum");
        }
        return false;
    }

    private long getTimespanMillis(int round) {
        return (long) (1000 * Math.pow(2, round));
    }

    public int leaderByIndex(int round) {
        return (round - 1) % nodesConfig.length;
    }

    public String leader(int round) {
        return nodesConfig[leaderByIndex(round)].getId();
    }

    public Optional<Pair<Integer, String>> highestPrepared(Collection<ConsensusMessage> quorum) {
        return quorum.stream()
                .map(ConsensusMessage::deserializeRoundChangeMessage)
                .max(Comparator.comparingInt(RoundChangeMessage::getPreparedRound))
                .map(m -> new Pair<Integer, String>(m.getPreparedRound(), m.getPreparedValue()))
                .filter(p -> p.getPreparedValue() != null)
                .stream().findAny();
    }

    // this does not change round, it just changes the leader according to the round in the node state
    private void updateLeader() {
        if (config.hasFailureType(ProcessConfig.FailureType.DICTATOR_LEADER)) {

            LOGGER.log(Level.INFO, "I am Dictator Leader. supposed to change leader but I am staying as leader");
            return;
        }
        int round = instanceInfo.get(consensusInstance.get()).getCurrentRound();
        int nextLeaderIndex = leaderByIndex(round);

        LOGGER.log(Level.INFO, MessageFormat.format(
                "[RC] Changing Leader from {0} to {1}", leaderConfig.getId(), nodesConfig[nextLeaderIndex].getId()
        ));

        leaderConfig = nodesConfig[nextLeaderIndex];
    }

    private void testTimer() {
        if (!started) {
            started = true;
            try {
                new Thread(() -> {
                    while (true) {
                        if (timers.get(consensusInstance.get()) == null) continue;
                        System.out.println("Timer is: " + timers.get(consensusInstance.get()).getState());
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void listen() {
        testTimer();
        ProcessConfig.FailureType failureType = config.getFailureType();
        LOGGER.log(Level.INFO, MessageFormat.format("{0} Failure:  {1}", config.getId(), failureType));

        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = nodesLink.receive();

                        /*
                         * Sends ACK to incoming message but doesn't broadcast anything
                         * Meaning that the other nodes will not be stuck waiting for a reply
                         */
                        if (failureType == ProcessConfig.FailureType.DROP) {
                            LOGGER.log(Level.INFO,
                                    MessageFormat.format("{0} - Byzantine Don't Reply", config.getId()));
                            // don't reply
                            continue;
                        }

                        // Separate thread to handle each message
                        new Thread(() -> {
                            switch (message.getType()) {
                                case PRE_PREPARE -> uponPrePrepare((ConsensusMessage) message);

                                case PREPARE -> uponPrepare((ConsensusMessage) message);

                                case COMMIT -> uponCommit((ConsensusMessage) message);

                                case ACK ->{
//                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
//                                            config.getId(), message.getSenderId()));
                                }

                                case IGNORE -> {
//                                    LOGGER.log(Level.INFO,
//                                            MessageFormat.format("IGNORE from {1}",
//                                                    config.getId(), message.getSenderId()));
                                }

                                case ROUND_CHANGE -> uponRoundChange((ConsensusMessage) message);

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));
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
