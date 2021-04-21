package client;

import api.WalletResource;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import data.ObtainCoinsParams;

import java.util.Arrays;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        WalletResource w = new WalletResource(3, 1003);
        boolean exit = false;
        while(!exit) {
            String input = scanner.nextLine();
            switch (input) {
                case "obtainCoins":
                    System.out.println(w.obtainCoins(new ObtainCoinsParams("1", 10)));
                    break;
                case "transactions":
                    System.out.println(Arrays.toString(w.getTransactionsData()));
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
