package pt.ulisboa.tecnico.hdsledger.service;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.services.BlockchainService;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.service.services.BlockPool;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

public class Node {
    private static final CustomLogger LOGGER = new CustomLogger(Node.class.getName());
    // Hardcoded path to files
    private static String nodesConfigPath = "src/main/resources/";

    private static String clientsConfigPath = "../Client/src/main/resources/";

    public static void main(String[] args) {

        try {
            // Command line arguments
            String id = args[0];
            nodesConfigPath += args[1];
            clientsConfigPath += args[2];
            int blockSize = Integer.parseInt(args[3]);

            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig[] clientConfigs = new ProcessConfigBuilder().fromFile(clientsConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            // Node pretends he is the leader
            if (nodeConfig.getFailureType() == ProcessConfig.FailureType.BAD_CONSENSUS) {
                Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).forEach(n -> n.setLeader(false));
                nodeConfig.setLeader(true);
            }

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; private key: {4}; public key: {5}",
                    nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                    nodeConfig.isLeader(), nodeConfig.getPrivateKeyPath(), nodeConfig.getPublicKeyPath()));

            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                    ConsensusMessage.class);

            Link linkToClients = new Link(nodeConfig, nodeConfig.getClientPort(), clientConfigs,
                    LedgerRequest.class);

            BlockPool blockPool = new BlockPool(blockSize);

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes,
                    nodeConfig, leaderConfig, nodeConfigs, clientConfigs,
                    blockPool);

            BlockchainService blockchainService = new BlockchainService(linkToClients,
                    nodeConfig, nodeConfigs, clientConfigs, nodeService,
                    blockPool);

            nodeService.setBlockchainService(blockchainService);

            blockchainService.listen();
            nodeService.listen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
