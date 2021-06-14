package api;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import data.*;

@Singleton
@Path("/wallet")
public class WalletResource extends DefaultSingleRecoverable {

    // public keys of the replicas that participate in consensus
    public static String[] repPubKeys = new String[]{
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEZ1khs6W4EA0r7JrhWNQAM79skNT1dDtfxO1smXmYBVl8PxdlWqMnE3kDgbTyX4ZqGEf5dKILDLzQbJVgiOmuow==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEwHVs6M04HtYukdIfXyHQh/Ab9CVtWvPSI8QOFhrzPak2WKdPGNe4ShsqqdWakmMAgk4q+8dFianPfrzLWPky3Q==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEH+EJqewBoZoKSGaooynw6C6E+ONgvyAeXRd1x2uzQZMK9Tdj1ut3XGI/jm38MTxLlv95Yw0Fn5SrazjiH20XQA==",
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEdIFueJ3KCYkdnfifV2odkaiHl1mFSPfXG/3DFHfVp20Cng0Pe6yoxg7BQ6YlJDI65YLSq6njmxNG0lGp4DJfpQ=="};

    // TODO: Save data to disk instead of memory, data can get too big
    private Map<String, Long> userBalances;
    // full ledger with confirmed blocks
    private Map<Long, Block> blocks;
    // list of unconfirmed transactions
    private List<Transaction> mem_pool;
    private final ServiceProxy proxy;
    private long currHeight;
    private long numCoins;
    private final int clientID;
    private final ServiceReplica replica;

    public WalletResource(int id, int processID) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        replica = new ServiceReplica(id, this, this);
        this.proxy = new ServiceProxy(processID);
        this.clientID = id;

        if (this.blocks == null) {
            this.blocks = new TreeMap<>();
            this.mem_pool = new ArrayList<>();
            this.userBalances = new TreeMap<>();
            this.currHeight = -1;
            this.numCoins = 0;
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
        Long amount = p.getAmount();
        if (!hasSpendableBalance(sender, amount)) {
            throw new WebApplicationException(Status.CONFLICT);
        }
        Transaction t = new Transaction(sender, p.getReceiver(), amount, p.getTimestamp(), p.getSignature());
        if (!t.isTransactionValid()) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        synchronized (this) {
            this.sendTransactionBFT(t);
        }
    }

    @GET
    @Path("/{addr}")
    @Produces(MediaType.APPLICATION_JSON)
    public long getBalanceOf(@PathParam("addr") String addr) {
        try {
            if (addr != null) {
                String decoded_addr = URLDecoder.decode(addr, StandardCharsets.UTF_8);
                decoded_addr = decoded_addr.replaceAll(" ", "+");
                Long balance = userBalances.get(decoded_addr);
                if (balance != null) {
                    return balance;
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // invalid address received as parameter
            return 0L;
        }
        return 0L;
    }

    @GET
    @Path("/allTransactions")
    @Produces(MediaType.APPLICATION_JSON)
    public Transaction[] getAllConfirmedTransactions() {
        Block[] ledger = this.blocks.values().toArray(new Block[0]);
        List<Transaction> txs = new ArrayList<>();
        for (Block b : ledger) {
            txs.addAll(Arrays.asList(b.getTransactions()));
        }
        return txs.toArray(new Transaction[0]);
    }

    @GET
    @Path("/transactions/{addr}")
    @Produces(MediaType.APPLICATION_JSON)
    public Transaction[] getTransactionsOf(@PathParam("addr") String addr) {
        String decoded_addr;
        try {
            decoded_addr = URLDecoder.decode(addr, StandardCharsets.UTF_8);
            decoded_addr = decoded_addr.replaceAll(" ", "+");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // invalid address received as parameter
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Block[] ledger = this.blocks.values().toArray(new Block[0]);
        List<Transaction> txs = new ArrayList<>();
        for (Block b : ledger) {
            for (Transaction t : b.getTransactions()) {
                if (t.getReceiver().equals(addr) || (t.getSender() != null && t.getSender().equals(decoded_addr))) {
                    txs.add(t);
                }
            }
        }
        return txs.toArray(new Transaction[0]);
    }

    @GET
    @Path("/lastBlock")
    @Produces(MediaType.APPLICATION_JSON)
    public Block getLastBlock() {
        return this.blocks.get(this.currHeight);
    }

    @GET
    @Path("/mempool")
    @Produces(MediaType.APPLICATION_JSON)
    public Transaction[] getUnconfirmedTransactions() {
        return this.mem_pool.toArray(new Transaction[0]);
    }

    @POST
    @Path("/mineBlock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public void mineBlock(Block b) {
        if (!b.isBlockValid()) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        synchronized (this) {
            this.sendBlockBFT(b);
        }
    }

    @GET
    @Path("/totalCoins")
    @Produces(MediaType.APPLICATION_JSON)
    public long getTotalCoins() {
        return this.numCoins;
    }

    @POST
    @Path("/sendSmartContract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public void transferCoinsSmartContract(Block b) {
        // TODO
    }

    @POST
    @Path("/sendPrivate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public void transferCoinsPrivate() {
        // TODO
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

            // updates signature with clientID and block, so other replicas can verify that messages are authentic
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(clientID);
            eng.update(b.array());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(t);
            oos.flush();

            eng.update(bos.toByteArray());

            signature = eng.sign();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(buffer);
            oos.writeObject(new ConsensusData(clientID, t, signature));
            oos.close();

            byte[] reply = proxy.invokeOrdered(buffer.toByteArray());

            if (reply == null) {
                System.err.println("ERROR! No reply received!");
            }
        } catch (IOException | NumberFormatException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            proxy.close();
        }

    }

    /**
     * Broadcast block to other nodes
     */
    private void sendBlockBFT(Block block) {
        try {
            byte[] signature;
            Signature eng;
            eng = TOMUtil.getSigEngine();
            eng.initSign(replica.getReplicaContext().getStaticConfiguration().getPrivateKey());

            // updates signature with clientID and block, so other replicas can verify that messages are authentic
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(clientID);
            eng.update(b.array());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(block);
            oos.flush();
            eng.update(bos.toByteArray());

            signature = eng.sign();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(buffer);
            oos.writeObject(new ConsensusData(clientID, block, signature));
            oos.close();

            byte[] reply = proxy.invokeOrdered(buffer.toByteArray());

            if (reply == null) {
                System.err.println("ERROR! No reply received!");
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
    private boolean hasSpendableBalance(String address, long amount) {
        Long currBalance = userBalances.get(address);
        return currBalance != null && currBalance >= amount;
    }

    private void verifyRecBlock(Block b) {
        Transaction[] txs = b.getTransactions();
        boolean prev_hash = currHeight < 0 || b.getPrevious_hash().equals(this.blocks.get(currHeight).getHash());
        if (b.isBlockValid() && allTransactionsValid(new TreeMap<>(this.userBalances), txs) && b.getHeight() == this.currHeight + 1 && prev_hash) {
            for (Transaction t : txs) {
                String sender = t.getSender();
                String receiver = t.getReceiver();
                long amount = t.getAmount();
                if (sender != null) {
                    userBalances.put(sender, userBalances.get(sender) - amount);
                } else {
                    // if sender is null than coins are being created through mining
                    this.numCoins += t.getAmount();
                }
                userBalances.merge(receiver, amount, Long::sum);
                this.mem_pool.removeIf(o -> o.getSignature().equals(t.getSignature()));
            }
            this.blocks.put(b.getHeight(), b);
            this.currHeight++;
        } else {
            System.err.println("Received invalid block!");
        }
    }

    public boolean allTransactionsValid(Map<String, Long> balances, Transaction[] txs_block) {
        List<Transaction> txs_conf = Arrays.asList(this.getAllConfirmedTransactions());
        for (Transaction t : txs_block) {
            String sender = t.getSender();
            String receiver = t.getReceiver();
            long amount = t.getAmount();
            if (sender != null) {
                if (t.isTransactionValid()) {
                    if (balances.containsKey(sender))
                        balances.put(sender, balances.get(sender) - amount);
                    else
                        return false;
                }
                // if there is a confirmed transaction with the same signature , transaction is invalid
                if (txs_conf.stream().anyMatch(o -> o.getSignature() != null && o.getSignature().equals(t.getSignature()))) {
                    System.err.println("Rejected block! Found duplicate transactions");
                    return false;
                }
            }
            balances.merge(receiver, amount, Long::sum);

            if ((sender != null && balances.get(sender) < 0) || balances.get(receiver) < 0) {
                System.err.println("Rejected block! Found negative balances");
                return false;
            }
        }
        return true;
    }

    private void verifyRecTransaction(Transaction t) {
        List<Transaction> txs = Arrays.asList(this.getUnconfirmedTransactions());
        if (t.isTransactionValid() && txs.stream().noneMatch(o -> o.getSignature().equals(t.getSignature()))) {
            if (this.mem_pool.size() >= 100) {
                this.mem_pool.remove(0);
            }
            this.mem_pool.add(t);
        } else {
            System.err.println("Received invalid transaction!");
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return null;
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            ConsensusData d = (ConsensusData) new ObjectInputStream(new ByteArrayInputStream(command)).readObject();
            byte[] signature = d.getSignature();
            int clientID = d.getClientID();
            Signature eng;

            eng = Signature.getInstance("SHA256withECDSA", "BC");
            Base64.Decoder b64 = Base64.getDecoder();
            // gets public key of clientID that we received message, only the real server can have the private key to create a valid signature
            byte[] encodedPublicKey = b64.decode(repPubKeys[clientID]);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            eng.initVerify(kf.generatePublic(spec));

            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(clientID);
            eng.update(b.array());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            if (d.hasBlock()) {
                oos.writeObject(d.getBlock());
            } else if (d.hasTransaction()) {
                oos.writeObject(d.getTransaction());
            }
            oos.flush();
            eng.update(bos.toByteArray());

            if (eng.verify(signature)) {
                if (d.hasBlock()) {
                    verifyRecBlock(d.getBlock());
                } else if (d.hasTransaction()) {
                    verifyRecTransaction(d.getTransaction());
                }
            } else {
                System.err.println("Client sent invalid signature!");
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(buffer);
            oos.writeObject(d);
            oos.close();
            return buffer.toByteArray();
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException | InvalidKeySpecException ex) {
            ex.printStackTrace();
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        this.blocks = new TreeMap<>();
        this.userBalances = new TreeMap<>();
        this.mem_pool = new ArrayList<>();
        this.currHeight = -1;
        this.numCoins = 0;
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

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Use: java WalletResource <id> <processId>");
            System.exit(-1);
        }
        new WalletResource(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

}