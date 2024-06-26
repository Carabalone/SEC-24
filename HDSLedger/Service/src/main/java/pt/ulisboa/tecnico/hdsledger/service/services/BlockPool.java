package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.LedgerRequest;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

import com.google.gson.GsonBuilder;


public class BlockPool {

    private final Queue<LedgerRequest> pool = new LinkedList<>();

    private final int blockSize;

    public BlockPool(int blockSize) {
        this.blockSize = blockSize;
    }

    public Queue<LedgerRequest> getPool() { return pool; }

    public void setPool(Queue<LedgerRequest> pool) {
        synchronized (this.pool) {
            this.pool.clear();
            this.pool.addAll(pool);
        }
    }

    public int getBlockSize() { return blockSize; }

    public Optional<Block> addRequest(LedgerRequest request) {
        synchronized (this.pool) {
            if (!this.pool.contains(request)) {
                this.pool.add(request);
            }
        }
        return checkTransactionThreshold();
    }

    public void removeRequest(LedgerRequest request) {
        this.pool.remove(request);
    }

    /*
     * Check if mempool has enough transactions to create a block
     * Only the leader tries to create blocks
     * Note that the leader will remove transactions from the mempool
     * but other nodes will not
     * They will be removed when the block is added to the blockchain
     */
    private Optional<Block> checkTransactionThreshold() {
        synchronized (this.pool) {
            if (this.pool.size() < this.blockSize) return Optional.empty();

            var block = new Block();

            // sort pool by request id
            this.pool.stream().sorted(Comparator.comparing(LedgerRequest::getRequestId));

            for (int i = 0; i < this.blockSize; i++){
                LedgerRequest req = this.pool.poll();
                block.addRequest(req);
            }

            return Optional.of(block);
        }
    }

    public void accept(Consumer<Queue<LedgerRequest>> handler) {
        synchronized (this.pool) {
            handler.accept(this.pool);
        }
    }

    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.pool);
    }
}