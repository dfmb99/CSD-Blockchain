package client;

import com.google.gson.Gson;
import data.Block;
import data.ConsensusRules;
import data.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Miner {

    static Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println();
        System.out.println(" ------- Miner client ---------");
        System.out.println(" -------------------------------------------- ");
        System.out.println("Node to connect information");
        Scanner scanner = new Scanner(System.in);
        System.out.print("IP: ");
        String ip = scanner.nextLine();
        System.out.print("Port: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Public key to receive block rewards: ");
        String addr = scanner.nextLine();
        System.out.println("Mining blocks...");
        while (true) {
            Block b = mineBlock(ip, port, addr);
            if (b != null) {
                mineBlock(ip, port, b);
            } else {
                System.out.println("Error making request!");
                break;
            }
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
            System.out.println(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + " - Mined block with hash: " + b.getHash());
        } else {
            System.out.println(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + " - Invalid block data!");
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
}
