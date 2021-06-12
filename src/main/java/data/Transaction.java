package data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Transaction implements Serializable {

    private String sender;
    private String receiver;
    private double amount;
    private String signature;
    private transient boolean transactionValid;

    public Transaction() {
    }

    public Transaction(String sender, String receiver, double amount, String signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.signature = signature;
    }

    public String getReceiver() {
        return this.receiver;
    }

    public String getSender() {
        return this.sender;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getSignature() {
        return this.signature;
    }

    public boolean isTransactionValid() {
        try {
            Signature eng = Signature.getInstance("SHA256withECDSA", "BC");

            Base64.Decoder b64 = Base64.getDecoder();
            // gets public key of clientID that we received message, only the real server can have the private key to create a valid signature
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

            return eng.verify(signature.getBytes());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | InvalidKeyException | IOException | SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void printTransactionData() {
        System.out.println("Sender: " + this.sender +
                " \n Receiver: " + this.receiver +
                " \n Amount: " + this.amount +
                " \n Signature: " + this.signature +
                " \n --------");
    }
}
