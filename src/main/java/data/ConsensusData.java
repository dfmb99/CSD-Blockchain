package data;

// CONSENSUS RULES


import java.io.Serializable;

// Nodes can either broadcast between each other blocks full of transactions that were mined or transactions broadcast by clients but that are still not mined
public class ConsensusData implements Serializable {
    int clientID;

    Block block;
    Transaction transaction;
    byte[] signature;

    public ConsensusData(int clientID, Block block, byte[] signature) {
        this.clientID = clientID;
        this.block = block;
        this.signature = signature;
    }

    public ConsensusData(int clientID, Transaction transaction, byte[] signature) {
        this.clientID = clientID;
        this.transaction = transaction;
        this.signature = signature;
    }

    public boolean hasBlock() {
        return this.block != null;
    }

    public boolean hasTransaction() {
        return this.transaction != null;
    }

    public Block getBlock() {
        return block;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getClientID() {
        return clientID;
    }
}
