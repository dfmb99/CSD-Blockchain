package api;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import bftsmart.tom.util.TOMUtil;
import data.Block;
import data.ObtainCoinsParams;
import data.TransferCoinsParams;
import data.Transaction;

@Singleton
@Path("/wallet")
public class WalletResource extends DefaultSingleRecoverable {

    public static String[] repPubKeys = new String[]{
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEZ1khs6W4EA0r7JrhWNQAM79skNT1dDtfxO1smXmYBVl8PxdlWqMnE3kDgbTyX4ZqGEf5dKILDLzQbJVgiOmuow==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEwHVs6M04HtYukdIfXyHQh/Ab9CVtWvPSI8QOFhrzPak2WKdPGNe4ShsqqdWakmMAgk4q+8dFianPfrzLWPky3Q==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEH+EJqewBoZoKSGaooynw6C6E+ONgvyAeXRd1x2uzQZMK9Tdj1ut3XGI/jm38MTxLlv95Yw0Fn5SrazjiH20XQA==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEdIFueJ3KCYkdnfifV2odkaiHl1mFSPfXG/3DFHfVp20Cng0Pe6yoxg7BQ6YlJDI65YLSq6njmxNG0lGp4DJfpQ=="};

    // TODO: Save data to disk instead of memory, data can get too big
    private Map<String, Double> userBalances;
    private Map<Long, Block> blocks;
    private final ServiceProxy proxy;
    private long height;
    private final int clientID;
    private final ServiceReplica replica;

    public WalletResource(int id, int processID) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        replica = new ServiceReplica(id, this, this);
        this.proxy = new ServiceProxy(processID);
        this.clientID = id;

        if ( this.blocks == null ) {
            this.blocks = new TreeMap<>();
            this.userBalances = new TreeMap<>();
            this.height = 0;
        }
    }

    @POST
    @Path("/receive")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public void obtainCoins(ObtainCoinsParams p) {
        if (!p.isDataValid()) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        String addr = p.getAddress();
        double amount = p.getAmount();
        Transaction t = new Transaction(null, addr, amount);
        synchronized (this) {
            this.sendTransactionBFT(t);
        }
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void transferCoins(TransferCoinsParams p) {
        if (!p.isDataValid()) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        String sender = p.getSender();
        String receiver = p.getReceiver();
        Double amount = p.getAmount();
        if (!hasSpendableBalance(sender, amount)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        Transaction t = new Transaction(sender, receiver, amount);
        synchronized (this) {
            this.sendTransactionBFT(t);
        }
    }

    @GET
    @Path("/{addr}")
    @Produces(MediaType.APPLICATION_JSON)
    public double getBalanceOf(@PathParam("addr") String addr) {
        Double balance = userBalances.get(addr);
        if (balance != null) {
            return balance;
        }
        return 0.0;
    }

    @GET
    @Path("/allTransactions")
    @Produces(MediaType.APPLICATION_JSON)
    public Block[] getTransactionsData() {
        return blocks.values().toArray(new Block[0]);
    }

    @GET
    @Path("/transactions/{addr}")
    @Produces(MediaType.APPLICATION_JSON)
    public Block[] getTransactionsOf(@PathParam("addr") String addr) {
        return blocks.values().stream().filter(t -> t.getTransaction().getReceiver().equals(addr) || (t.getTransaction().getSender() != null && t.getTransaction().getSender().equals(addr)))
                .toArray(Block[]::new);
    }


    /**
     * Broadcast transaction to other nodes
     */
    private void sendTransactionBFT(Transaction t) {
        try {
            byte[] signature;
            Signature eng;
            eng = TOMUtil.getSigEngine();
            eng.initSign(replica.getReplicaContext().getStaticConfiguration().getPrivateKey());

            // updates signature with transaction info, clientID and block height, so other replicas can verify that messages are authentic
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(clientID);
            eng.update(b.array());

            b = ByteBuffer.allocate(8);
            b.putLong(this.height);
            eng.update(b.array());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(t);
            oos.flush();

            eng.update(bos.toByteArray());

            signature = eng.sign();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(buffer);
            Block block_sent = new Block(signature, clientID, this.height, t);
            oos.writeObject(block_sent);
            oos.close();

            byte[] reply = proxy.invokeOrdered(buffer.toByteArray());

            if (reply == null) {
                System.out.println("ERROR! No reply received!");
            }
        } catch (IOException | NumberFormatException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            proxy.close();
        }

    }

    /**
     * Returns true if address 'address' has a balance of >= 'amount'
     *
     * @param address - address to check balance
     * @param amount  - minimum amount of coins address must hold to return true
     */
    private boolean hasSpendableBalance(String address, double amount) {
        Double currBalance = userBalances.get(address);
        return currBalance != null && currBalance >= amount;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(this.blocks);
            oos.close();
            return buffer.toByteArray();
        } catch (IOException ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            Block b = (Block) new ObjectInputStream(new ByteArrayInputStream(command)).readObject();
            byte[] signature = b.getSignature();
            int clientID = b.getClientID();
            Signature eng;

            eng = Signature.getInstance("SHA256withECDSA", "BC");
            Base64.Decoder b64 = Base64.getDecoder();
            // gets public key of clientID that we received message, only the real server can have the private key to create a valid signature
            byte[] encodedPublicKey = b64.decode(repPubKeys[clientID]);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
            KeyFactory kf = KeyFactory.getInstance ("ECDSA", "BC");
            eng.initVerify(kf.generatePublic(spec));

            // updates signature with transaction info, clientID and block height, so we can verify that message received is authentic
            ByteBuffer bf = ByteBuffer.allocate(4);
            bf.putInt(clientID);
            eng.update(bf.array());

            bf = ByteBuffer.allocate(8);
            bf.putLong(b.getHeight());
            eng.update(bf.array());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(b.getTransaction());
            oos.flush();
            eng.update(bos.toByteArray());

            if (!eng.verify(signature)) {
                System.out.println("Client sent invalid signature!");
            }

            Transaction t = b.getTransaction();
            blocks.put(b.getHeight(), b);
            this.height++;
            String sender = t.getSender();
            String receiver = t.getReceiver();
            double amount = t.getAmount();
            if (sender != null) {
                userBalances.put(sender, userBalances.get(sender) - amount);
            }
            userBalances.merge(receiver, amount, Double::sum);

            ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(buffer1);
            oos.writeObject(b);
            oos.close();
            return buffer1.toByteArray();
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException | InvalidKeySpecException ex) {
            ex.printStackTrace();
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Use: java WalletResource <id> <processId>");
            System.exit(-1);
        }
        new WalletResource(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }


    @Override
    public void installSnapshot(byte[] state) {
       this.blocks = new TreeMap<>();
       this.userBalances = new TreeMap<>();
       this.height = 0;
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput o = new ObjectOutputStream(bos);
            o.writeObject(this.blocks);
            o.flush();
            bos.flush();
            o.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }

}