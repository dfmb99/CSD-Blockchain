package client;

import com.google.gson.Gson;
import data.Block;
import data.ObtainCoinsParams;
import data.TransferCoinsParams;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

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
                    int amt = scanner.nextInt();
                    scanner.nextLine();
                    obtainCoins(ip, port, new ObtainCoinsParams(addr, amt));
                    break;
                case "transferCoins":
                    System.out.print("Sender address: ");
                    String sender = scanner.nextLine();
                    System.out.print("Receiver address: ");
                    String rec = scanner.nextLine();
                    System.out.print("Amount: ");
                    amt = scanner.nextInt();
                    scanner.nextLine();
                    transferCoins(ip, port, new TransferCoinsParams(sender, rec, amt));
                    break;
                case "AllTransactions":
                    getAllTransactions(ip, port);
                    break;
                case "getTransactions":
                    System.out.print("Address: ");
                    addr = scanner.nextLine();
                    getTransactionsOf(ip, port, addr);
                    break;
                case "getBalance":
                    System.out.print("Address: ");
                    addr = scanner.nextLine();
                    getBalanceOf(ip, port, addr);
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

    private static void obtainCoins(String ip, int port, ObtainCoinsParams p) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/receive");
        Response r = target.request()
                .post(Entity.entity(gson.toJson(p), MediaType.APPLICATION_JSON));

        int status = r.getStatus();
        if (status == Response.Status.NO_CONTENT.getStatusCode()) {
            System.out.println("Request successful!");
        } else {
            System.out.println("Error: " + status);
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
            System.out.println("Request successful!");
        } else {
            System.out.println("Error: " + status);
        }
    }

    private static void getBalanceOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/" + addr);
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Response: " + r.readEntity(Double.class));
        } else {
            System.out.println("Error: " + status);
        }
    }

    private static void getTransactionsOf(String ip, int port, String addr) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/transactions/" + addr);
        getListResponse(target);
    }

    private static void getListResponse(WebTarget target) {
        Response r = target.request()
                .get();

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Response: ");
            List<Block> res = r.readEntity(new GenericType<List<Block>>() {});
            for(Block b: res) {
                b.getTransaction().printTransactionData();
            }
        } else {
            System.out.println("Error: " + status);
        }
    }

    private static void getAllTransactions(String ip, int port) {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        javax.ws.rs.client.Client client = ClientBuilder.newClient(config);
        WebTarget target;

        target = client.target(String.format("http://%s:%s/rest", ip, port)).path("wallet/allTransactions");
        getListResponse(target);
    }
}
