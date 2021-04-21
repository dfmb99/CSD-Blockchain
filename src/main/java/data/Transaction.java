package data;

import java.io.Serializable;

public class Transaction implements Serializable {
	
	private static final long serialVersionUID = -3167265013825626676L;
	private String sender;
	private String receiver;
	private double amount;
	
	public Transaction() { }
	
	public Transaction(String sender, String receiver, double amount) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
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

	public void printTransactionData() {
		System.out.println("Sender: " + this.sender + " \n Receiver: " + this.receiver + " \n Amount: " + this.amount + " \n --------");
	}

}
