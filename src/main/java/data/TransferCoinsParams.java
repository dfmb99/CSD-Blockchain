package data;

public class TransferCoinsParams {
	private String sender;
	private String receiver;
	private Long amount;
	private long timestamp;
	private String signature;
	
	public TransferCoinsParams() { }
	
	public TransferCoinsParams(String sender, String receiver, long amount, long timestamp, String signature) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		this.timestamp = timestamp;
		this.signature = signature;
	}
	
	public String getSender() {
		return this.sender;
	}
	
	public String getReceiver() {
		return this.receiver;
	}
	
	public Long getAmount() {
		return this.amount;
	}

	public long getTimestamp() {
		return this.timestamp;
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
				&& this.timestamp > 0
				&& this.signature != null
				&& !this.signature.equals("");
	}
}
