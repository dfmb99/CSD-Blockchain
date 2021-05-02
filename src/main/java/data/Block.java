package data;

import java.io.Serializable;

public class Block implements Serializable {

    private int clientID;
    private byte[] signature;
    private Transaction transaction;
    private long height;

    public Block() { }

    public Block(byte[] signature, int clientID, long height, Transaction transaction) {
        this.signature = signature;
        this.clientID = clientID;
        this.height = height;
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public long getHeight() {
        return height;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getClientID() {
        return clientID;
    }
}
