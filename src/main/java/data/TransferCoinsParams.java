package data;

public class TransferCoinsParams {
	private String sender;
	private String receiver;
	private Double amount;
	private String signature;
	
	public TransferCoinsParams() { }
	
	public TransferCoinsParams(String sender, String receiver, double amount, String signature) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		this.signature = signature;
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

	public String getSignature() {
		return this.signature;
	}
	
	public boolean isDataValid() {
		return this.sender != null
				&& !this.sender.equals("")
				&& this.receiver != null
				&& !this.receiver.equals("")
				&& this.amount > 0
				&& this.signature != null
				&& !this.signature.equals("");
	}
}
