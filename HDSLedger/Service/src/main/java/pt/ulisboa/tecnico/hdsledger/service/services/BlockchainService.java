package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainAppendRequest;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.io.IOException;
import java.util.ArrayList;

public class BlockchainService implements UDPService {

    private static final CustomLogger LOGGER = new CustomLogger(BlockchainService.class.getName());

    private final ProcessConfig selfConfig;

    private final Link nodesLink;
    private final Link clientsLink;

    private final ArrayList<String> ledger = new ArrayList<String>();

    private final ProcessConfig[] nodesConfig;

    private final NodeService nodeService;
    private final ProcessConfig[] clientsConfig;

    public BlockchainService(Link nodesLink, Link clientsLink, ProcessConfig selfConfig,
                             ProcessConfig[] nodesConfig, ProcessConfig[] clientsConfig,
                             NodeService nodeService) {

        this.clientsLink = clientsLink;
        this.nodesLink = nodesLink;
        this.selfConfig = selfConfig;
        this.nodesConfig = nodesConfig;
        this.clientsConfig = clientsConfig;
        this.nodeService = nodeService;

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
                                    nodeService.append((BlockchainAppendRequest) message);
                                }

                                case PING -> {
                                    nodeService.ping();
                                    clientsLink.send(message.getSenderId(), new Message(selfConfig.getId(), Message.Type.PING));
                                }
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
