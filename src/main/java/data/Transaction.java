package data;

import org.bouncycastle.util.encoders.Hex;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Transaction implements Serializable {

    private String sender;
    private String receiver;
    private long amount;
    private long timestamp;
    private String signature;
    private transient boolean transactionValid;

    public Transaction() {
    }

    public Transaction(String sender, String receiver, long amount, long timestamp,String signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    public String getReceiver() {
        return this.receiver;
    }

    public String getSender() {
        return this.sender;
    }

    public long getAmount() {
        return this.amount;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getSignature() {
        return this.signature;
    }

    public boolean isTransactionValid() {
        if (this.sender == null || this.receiver == null || this.timestamp <= 0 || this.amount <= 0 || this.signature == null)
            return false;

        try {
            Signature eng = Signature.getInstance("SHA256withECDSA", "BC");
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] encodedPublicKey = b64.decode(this.sender);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            eng.initVerify(kf.generatePublic(spec));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.sender);
            oos.flush();
            eng.update(bos.toByteArray());

            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(this.receiver);
            oos.flush();
            eng.update(bos.toByteArray());

            ByteBuffer b = ByteBuffer.allocate(8);
            b.putDouble(this.amount);
            eng.update(b.array());

            b = ByteBuffer.allocate(8);
            b.putLong(this.timestamp);
            eng.update(b.array());

            return eng.verify(Hex.decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    public void printTransactionData() {
        System.out.println(" Sender: " + this.sender +
                " \n Receiver: " + this.receiver +
                " \n Amount: " + this.amount +
                " \n Timestamp: " + this.timestamp +
                " \n Signature: " + this.signature +
                " \n --------");
    }
}
