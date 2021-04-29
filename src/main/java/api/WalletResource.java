package api;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
import data.Block;
import data.ObtainCoinsParams;
import data.TransferCoinsParams;
import data.Transaction;

@Singleton
@Path("/wallet")
public class WalletResource extends DefaultSingleRecoverable {

    private final String base_dir = System.getProperty("java.io.tmpdir");
    private final String filename;
    private final Map<String, Double> userBalances;
    private Map<Long, Transaction> transactions;
    private ObjectOutputStream out;
    private final ServiceProxy counterProxy;
    private long height;

    public WalletResource(int id, int processID) throws Exception {
        new ServiceReplica(id, this, this);
        this.counterProxy = new ServiceProxy(processID);
        this.filename = "transactions" + id + ".data";
        this.transactions = new TreeMap<>();
        this.userBalances = new TreeMap<>();
        this.height = 0;

        this.loadMemoryData();
    }

    /**
     * Loads transactions data from local memory if available, and only after connects to other nodes
     */
    private void loadMemoryData() throws Exception {
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(base_dir + filename));
            while (true) {
                try {
                    this.transactions.put(height++, (Transaction) is.readObject());
                } catch (Exception e) {
                    break;
                }
            }

            this.updateAccountBalances();
        } catch (IOException e) {
            ObjectOutputStream os1 = new ObjectOutputStream(new FileOutputStream(base_dir + filename));
            os1.close();
        }
        out = new ObjectOutputStream(new FileOutputStream(base_dir + filename, true)) {
            protected void writeStreamHeader() throws IOException {
                reset();
            }
        };
    }

    @POST
    @Path("/receive")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public double obtainCoins(ObtainCoinsParams p) throws IOException, InterruptedException {
        if (!p.isDataValid()) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        String addr = p.getAddress();
        double amount = p.getAmount();
        Transaction t = new Transaction(null, addr, amount);
        synchronized (this) {
            this.sendTransactionBFT(t);
        }
        return userBalances.get(addr);
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void transferCoins(TransferCoinsParams p) throws IOException {
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
    public Transaction[] getTransactionsData() {
        return transactions.values().toArray(new Transaction[0]);
    }

    @GET
    @Path("/transactions/{addr}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Transaction> getTransactionsOf(@PathParam("addr") String addr) {
        return transactions.values().stream().filter(t -> t.getReceiver().equals(addr) || t.getSender().equals(addr))
                .collect(Collectors.toList());
    }


    /**
     * Broadcast transaction to other nodes
     */
    private void sendTransactionBFT(Transaction t) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(new Block(this.height++, t));
            oos.close();
            byte[] reply = counterProxy.invokeOrdered(buffer.toByteArray());

            if (reply != null) {
                Block block = (Block) new ObjectInputStream(new ByteArrayInputStream(reply)).readObject();
                System.out.println(block.getHeight());
            } else {
                System.out.println(", ERROR! Exiting.");
            }
        } catch (IOException | NumberFormatException | ClassNotFoundException e) {
            counterProxy.close();
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

    /**
     * Updates account balances based on transactions data
     */
    private void updateAccountBalances() {
        // Sync transaction list and updates every account balance in memory
        for (Transaction t : transactions.values()) {
            String receiver = t.getReceiver();
            String sender = t.getSender();
            double amount = t.getAmount();

            if (sender != null) {
                userBalances.put(sender, userBalances.get(sender) - amount);
            }
            userBalances.merge(receiver, amount, Double::sum);
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(this.transactions);
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
            System.out.println(b.getHeight());
            Transaction t = b.getTransaction();
            transactions.put(b.getHeight(), t);
            out.writeObject(t);
            String sender = t.getSender();
            String receiver = t.getReceiver();
            double amount = t.getAmount();
            if (sender != null) {
                userBalances.put(sender, userBalances.get(sender) - amount);
            }
            userBalances.merge(receiver, amount, Double::sum);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(buffer);
            oos.writeObject(b);
            oos.close();
            return buffer.toByteArray();
        } catch (IOException | ClassNotFoundException ex) {
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


    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            this.transactions = (Map<Long, Transaction>) in.readObject();
            in.close();
            bis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(this.transactions);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }

}