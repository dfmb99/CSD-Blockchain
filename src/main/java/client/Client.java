package client;

import bftsmart.tom.util.TOMUtil;
import com.google.gson.Gson;
import data.*;
import org.bouncycastle.util.encoders.Hex;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class Client {

    static Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println(" ------- P2P Wallet Service client ---------");
        System.out.println(" ( Type help for command list in prompt ) ");
        System.out.println(" -------------------------------------------- ");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Server IP: ");
        String ip = scanner.nextLine();
        System.out.print("Port: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        boolean exit = false;
        while (!exit) {
            System.out.print("> ");
            String input = scanner.next();
            switch (input) {
                case "transferCoins":
                    try {
                        String sender = scanner.next();
                        String rec = scanner.next();
                        long amt = scanner.nextLong();
                        String key = scanner.next();
                        scanner.nextLine();
                        long timestamp = System.currentTimeMillis();
                        String sig = generateTransactionSignature(key, sender, rec, timestamp, amt);
                        TransferCoinsParams p = new TransferCoinsParams(sender, rec, amt, timestamp, sig);
                        if (p.isDataValid())
                            transferCoins(ip, port, p);
                        else
                            System.out.println("Usage: transferCoins <sender public key> <receiver public key> <amount> <sender private key>");
                    } catch (Exception e) {
                        System.out.println("Usage: transferCoins <sender public key> <receiver public key> <amount> <sender private key>");
                    }
                    break;
                case "allTransactions":
                    Transaction[] txs = getAllTransactions(ip, port);
                    if (txs != null) {
                        for (Transaction t : txs) {
                            t.printTransactionData();
                        }
                    } else {
                        System.out.println("Error making request!");
                    }
                    break;
                case "getTransactionsOf":
                    try {
                        String addr = scanner.next();
                        scanner.nextLine();
                        txs = getTransactionsOf(ip, port, addr);
                        if (txs != null) {
                            for (Transaction t : txs) {
                                t.printTransactionData();
                            }
                        } else {
                            System.out.println("Error making request!");
                        }
                    } catch (Exception e) {
                        System.out.println("Usage: getTransactionsOf <public key> ");
                    }
                    break;
                case "getBalanceOf":
                    try {
                        String addr = scanner.next();
                        scanner.nextLine();
                        Long bal = getBalanceOf(ip, port, addr);
                        if (bal != null) {
                            System.out.println(bal);
                        } else {
                            System.out.println("Error making request!");
                        }
                    } catch (Exception e) {
                        System.out.println("Usage: getBalanceOf <public key> ");
                    }
                    break;
                case "getMemPool":
                    txs = getMemPool(ip, port);
                    if (txs != null) {
                        for (Transaction t : txs) {
                            t.printTransactionData();
                        }
                    } else {
                        System.out.println("Error making request!");
                    }
                    break;
                case "getLastBlock":
                    Block b = getLastBlock(ip, port);
                    if (b != null) {
                        b.printBlockData();
                    } else {
                        System.out.println("Error making request!");
                    }
                    break;
                case "mineBlock":
                    try {
                        String addr = scanner.next();
                        scanner.nextLine();
                        if (addr == null || addr.equals("")) {
                            System.out.println("Usage: mineBlock <public key to receive block reward> ");
                            break;
                        }
                        System.out.println("Mining block...");
                        b = mineBlock(ip, port, addr);
                        if (b != null) {
                            mineBlock(ip, port, b);
                        } else {
                            System.out.println("Error making request!");
                        }
                    } catch (Exception e) {
                        System.out.println("Usage: mineBlock <public key to receive block reward> ");
                    }
                    break;
                case "exit":
                    System.out.println("Client exiting...");
                    exit = true;
                    break;
                default:
                    System.out.println("transferCoins <sender public key> <receiver public key> <amount> <sender private key> - Transfer coins from an account to other.");
                    System.out.println("allTransactions - Lists all transactions recorded on the ledger.");
                    System.out.println("getTransactionsOf <public key> - Lists all transactions of an account.");
                    System.out.println("getBalanceOf <public key> - Gets current balance of an account.");
                    System.out.println("getMemPool - Gets unconfirmed transactions stored on node.");
                    System.out.println("getLastBlock - Gets information of last block mined.");
                    System.out.println("mineBlock <public key to receive block reward> - Tries to mine a block (can take some time).");
                    System.out.println("exit - Exits client");
            }
        }
    }

    private static void transferCoins(String ip, int port, TransferCoinsParams p) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/send");
        Response r = target.request()
                .post(Entity.entity(gson.toJson(p), MediaType.APPLICATION_JSON));

        int status = r.getStatus();
        if (status == Response.Status.NO_CONTENT.getStatusCode()) {
            System.out.println("Transaction broadcast to nodes with success (wait for miners to confirm transaction)!");
        } else {
            if (status == Response.Status.FORBIDDEN.getStatusCode()) {
                System.out.println("Invalid signature!");
            } else if (status == Response.Status.CONFLICT.getStatusCode()) {
                System.out.println("Not enough balance to do transaction!");
            } else {
                System.out.println("Invalid transaction!");
            }
        }
    }

    private static Long getBalanceOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;
        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/" + URLEncoder.encode(addr, StandardCharsets.UTF_8));
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(Long.class);
        } else {
            return null;
        }
    }

    private static Transaction[] getTransactionsOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/transactions/" + URLEncoder.encode(addr, StandardCharsets.UTF_8));
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(new GenericType<Transaction[]>() {
            });
        } else {
            return null;
        }
    }

    private static Transaction[] getMemPool(String ip, int port) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/mempool");
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(new GenericType<Transaction[]>() {
            });
        } else {
            return null;
        }
    }

    private static Transaction[] getAllTransactions(String ip, int port) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/allTransactions");
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(new GenericType<Transaction[]>() {
            });
        } else {
            return null;
        }
    }

    private static void mineBlock(String ip, int port, Block b) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/mineBlock");
        Response r = target.request()
                .post(Entity.entity(gson.toJson(b), MediaType.APPLICATION_JSON));

        int status = r.getStatus();
        if (status == Response.Status.NO_CONTENT.getStatusCode()) {
            System.out.println("Mined block with hash: " + b.getHash());
        } else {
            System.out.println("Invalid block data!");
        }
    }

    private static Block getLastBlock(String ip, int port) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/lastBlock");
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(Block.class);
        } else {
            return null;
        }
    }

    private static Block mineBlock(String ip, int port, String addr) {
        Block b = getLastBlock(ip, port);
        Transaction[] txs = getMemPool(ip, port);
        if (txs == null) {
            txs = new Transaction[1];
        }
        // if there is no last block, we are mining the genesis block
        if (b == null) {
            b = new Block("", "", "", -1, -1, -1, null);
        }
        return generateBlock(ConsensusRules.DIFFICULTY, b, new ArrayList<>(Arrays.asList(txs)), addr);
    }

    private static Block generateBlock(int difficulty, Block prev_block, List<Transaction> txsl, String addr) {
        Transaction coinbase_tx = new Transaction(null, addr, ConsensusRules.BLOCK_REWARD, System.currentTimeMillis(), null);
        txsl.add(0, coinbase_tx);
        Transaction[] txs = txsl.toArray(new Transaction[0]);
        long nonce = -1L;
        long timestamp = -1L;
        String z = "";
        String hash = "";
        for (int i = 0; i < difficulty; i++) {
            z = z.concat("0");
            hash = hash.concat("1");
        }
        while (!hash.substring(0, difficulty).equals(z)) {
            nonce++;
            timestamp = System.currentTimeMillis();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                hash = prev_block.getHash() + nonce + difficulty + (prev_block.getHeight() + 1) + timestamp + new Gson().toJson(txs);
                byte[] encodedHash = digest.digest(hash.getBytes(StandardCharsets.UTF_8));
                hash = new String(Hex.encode(encodedHash));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return new Block(prev_block.getHash(), hash, String.valueOf(nonce), difficulty, prev_block.getHeight() + 1, timestamp, txs);
    }

    private static String generateTransactionSignature(String key, String sender, String rec, long timestamp, double amount) {
        try {
            byte[] signature;
            Signature eng;

            eng = TOMUtil.getSigEngine();
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] encodedPrivateKey = b64.decode(key);


            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            eng.initSign(kf.generatePrivate(spec));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(sender);
            oos.flush();
            eng.update(bos.toByteArray());

            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(rec);
            oos.flush();
            eng.update(bos.toByteArray());

            ByteBuffer b = ByteBuffer.allocate(8);
            b.putDouble(amount);
            eng.update(b.array());

            b = ByteBuffer.allocate(8);
            b.putLong(timestamp);
            eng.update(b.array());

            signature = eng.sign();
            return new String(Hex.encode(signature));
        } catch (Exception e) {
            return null;
        }
    }
}