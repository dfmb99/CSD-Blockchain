package data;

import java.io.Serializable;

public class Block implements Serializable {

    private Transaction transaction;
    private long height;

    public Block() { }

    public Block(long height, Transaction transaction) {
        this.height = height;
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public long getHeight() {
        return height;
    }
}
