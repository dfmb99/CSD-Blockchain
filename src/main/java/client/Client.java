package client;

import com.google.gson.Gson;
import data.Block;
import data.ObtainCoinsParams;
import data.TransferCoinsParams;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Scanner;

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
            String input = scanner.nextLine();
            switch (input) {
                case "obtainCoins":
                    System.out.print("Address to receive coins: ");
                    String addr = scanner.nextLine();
                    System.out.print("Amount: ");
                    int amnt = scanner.nextInt();
                    scanner.nextLine();
                    System.out.println(obtainCoins(ip, port, new ObtainCoinsParams(addr, amnt)));
                    break;
                case "transferCoins":
                    System.out.print("Sender address: ");
                    String sender = scanner.nextLine();
                    System.out.print("Receiver address: ");
                    String rec = scanner.nextLine();
                    System.out.print("Amount: ");
                    amnt = scanner.nextInt();
                    scanner.nextLine();
                    transferCoins(ip, port, new TransferCoinsParams(sender, rec, amnt));
                    break;
                case "AllTransactions":
                    List<Block> res = getAllTransactions(ip, port);
                    for(Block b: res) {
                        b.getTransaction().printTransactionData();
                    }
                    break;
                case "getTransactions":
                    System.out.print("Address: ");
                    addr = scanner.nextLine();
                    res = getTransactionsOf(ip, port, addr);
                    for(Block b: res) {
                        b.getTransaction().printTransactionData();
                    }
                    break;
                case "getBalance":
                    System.out.print("Address: ");
                    addr = scanner.nextLine();
                    System.out.println(getBalanceOf(ip, port, addr));
                    break;
                case "help":
                    System.out.println("obtainCoins - To receive coins to an account");
                    System.out.println("transferCoins - Transfer coins from an account to other");
                    System.out.println("AllTransactions - Lists all transactions recorded on the ledger");
                    System.out.println("getTransactions - Lists all transactions of an account");
                    System.out.println("getBalance - Gets current balance of an account");
                    System.out.println("exit - Exits client");
                    break;
                case "exit":
                    System.out.println("Client exiting...");
                    exit = true;
                    break;
                default:
                    System.out.println("Unknown command. Type help for command list.");
            }
        }
    }

    private static double obtainCoins(String ip, int port, ObtainCoinsParams p) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 3000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/receive");
        Response r = target.request()
                .post(Entity.entity(gson.toJson(p), MediaType.APPLICATION_JSON));

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(Double.class);
        } else {
            throw new WebApplicationException(r.getStatus());
        }
    }

    private static void transferCoins(String ip, int port, TransferCoinsParams p) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 3000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/send");
        Response r = target.request()
                .post(Entity.entity(gson.toJson(p), MediaType.APPLICATION_JSON));

        if (r.getStatus() != Response.Status.NO_CONTENT.getStatusCode() ) {
            throw new WebApplicationException(r.getStatus());
        }
    }

    private static double getBalanceOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 3000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/" + addr);
        Response r = target.request()
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(Double.class);
        } else {
            throw new WebApplicationException(r.getStatus());
        }
    }

    private static List<Block> getTransactionsOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 3000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/transactions/" + addr);
        Response r = target.request()
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(new GenericType<List<Block>>() {});
        } else {
            throw new WebApplicationException(r.getStatus());
        }
    }

    private static List<Block> getAllTransactions(String ip, int port) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 3000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/allTransactions");
        Response r = target.request()
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(new GenericType<List<Block>>() {});
        } else {
            throw new WebApplicationException(r.getStatus());
        }
    }
}
