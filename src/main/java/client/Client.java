package client;

import api.WalletResource;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import data.ObtainCoinsParams;

import java.util.Arrays;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        WalletResource w = new WalletResource(3, 1004);
        boolean exit = false;
        while(!exit) {
            String input = scanner.nextLine();
            switch (input) {
                case "obtainCoins":
                    System.out.println(w.obtainCoins(new ObtainCoinsParams("1", 10)));
                    break;
                case "transferCoins":
                    System.out.println(Arrays.toString(w.getTransactionsData()));
                    break;
                case "AllTransactions":
                    // TODO
                    break;
                case "getTransactions":
                    // TODO
                    break;
                case "getBalance":
                    // TODO
                    break;
                case "exit":
                    System.out.println("Client exiting...");
                    exit = true;
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }
}
