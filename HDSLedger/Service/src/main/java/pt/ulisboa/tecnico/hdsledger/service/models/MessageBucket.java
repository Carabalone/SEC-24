package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;


public class MessageBucket {

    private static final CustomLogger LOGGER = new CustomLogger(MessageBucket.class.getName());

    // Quorum size
    private final int quorumSize;

    // Instance -> Round -> Sender ID -> Consensus message
    private final Map<Integer, Map<Integer, Map<String, ConsensusMessage>>> bucket = new ConcurrentHashMap<>();


    public MessageBucket(int nodeCount) {
        int f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    /*
     * Add a message to the bucket
     * 
     * @param consensusInstance
     * 
     * @param message
     */
    public void addMessage(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        bucket.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).putIfAbsent(round, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).get(round).put(message.getSenderId(), message);
    }

    public Optional<Block> hasValidPrepareQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<Block, Integer> frequency = new HashMap<>();

        if (bucket.get(instance) == null || bucket.get(instance).get(round) == null)
            return Optional.empty();

        bucket.get(instance).get(round).values().forEach((message) -> {
            PrepareMessage prepareMessage = message.deserializePrepareMessage();
            Block block = Block.fromJson(prepareMessage.getBlock());

            frequency.put(block, frequency.getOrDefault(block, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<Block, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<Block, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Optional<Block> hasValidCommitQuorum(String nodeId, int instance, int round) {

        if (bucket.get(instance) == null || bucket.get(instance).get(round) == null)
            return Optional.empty();

        // Create mapping of value to frequency
        HashMap<Block, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            CommitMessage commitMessage = message.deserializeCommitMessage();
            Block block = Block.fromJson(commitMessage.getBlock());
            frequency.put(block, frequency.getOrDefault(block, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<Block, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<Block, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public boolean hasValidRoundChangeQuorum(String nodeId, int instance, int round) {
        if (bucket.get(instance) == null || bucket.get(instance).get(round) == null)
            return false;
        return bucket.get(instance).get(round).values().size() >= quorumSize;
    }

    public Map<String, ConsensusMessage> getMessages(int instance, int round) {
        bucket.putIfAbsent(instance, new ConcurrentHashMap<>());
        bucket.get(instance).putIfAbsent(round, new ConcurrentHashMap<>());

        if (bucket.get(instance) == null || bucket.get(instance).get(round) == null)
            return new ConcurrentHashMap<>();

        return bucket.get(instance).get(round);
    }
}
