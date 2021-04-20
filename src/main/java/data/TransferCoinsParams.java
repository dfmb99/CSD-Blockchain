package data;

public class TransferCoinsParams {
	private String sender;
	private String receiver;
	private Double amount;
	
	public TransferCoinsParams() { }
	
	public TransferCoinsParams(String sender, String receiver, Double amount) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
	}
	
	public String getSender() {
		return this.sender;
	}
	
	public String getReceiver() {
		return this.receiver;
	}
	
	public Double getAmount() {
		return this.amount;
	}
	
	public boolean isDataValid() {
		return this.sender != null && this.sender != "" && this.receiver != null && this.receiver != "" && this.amount > 0;
	}
}
