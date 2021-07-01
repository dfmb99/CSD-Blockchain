package data;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import com.google.gson.Gson;
import org.bouncycastle.util.encoders.Hex;

public class Block implements Serializable {

    private String previous_hash;
    private String hash;
    private String nonce;
    private int difficulty;
    private long height;
    private long timestamp;
    private Transaction[] transactions;
    private transient boolean blockValid;

    public Block() {
    }

    public Block(String previous_hash, String hash, String nonce, int difficulty, long height, long timestamp, Transaction[] transactions) {
        this.previous_hash = previous_hash;
        this.hash = hash;
        this.nonce = nonce;
        this.difficulty = difficulty;
        this.height = height;
        this.timestamp = timestamp;
        this.transactions = transactions;
    }

    public String getPrevious_hash() {
        return previous_hash;
    }

    public String getHash() {
        return hash;
    }

    public String getNonce() {
        return nonce;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public long getHeight() {
        return height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public boolean isBlockValid() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = this.previous_hash + this.nonce + this.difficulty + this.height + this.timestamp + new Gson().toJson(transactions);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = new String(Hex.encode(encodedHash));
            String z = "";
            for (int i = 0; i < difficulty; i++) {
                z = z.concat("0");
            }
            double rewards = 0;
            for (Transaction t: this.transactions) {
                if (t.getSender() == null) {
                    rewards += t.getAmount();
                }
            }
            return hash.substring(0, this.difficulty).equals(z)
                    && this.transactions.length <= ConsensusRules.MAX_TRANSACTIONS
                    && this.difficulty == ConsensusRules.DIFFICULTY
                    && rewards <= ConsensusRules.BLOCK_REWARD;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void printBlockData() {
        System.out.println(" Previous Hash: " + this.previous_hash +
                " \n Block hash: " + this.hash +
                " \n Nonce: " + this.nonce +
                " \n Difficulty: " + this.difficulty +
                " \n Height: " + this.height +
                " \n Timestamp: " + this.timestamp +
                " \n Transactions: " + new Gson().toJson(this.transactions) +
                " \n --------");
    }
}
