package data;

public class ObtainCoinsParams {
	private String address;
	private double amount;
	private String signature;
	
	public ObtainCoinsParams() { }
	
	public ObtainCoinsParams(String address, double amount, String signature) {
		this.address = address;
		this.amount = amount;
		this.signature = signature;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public double getAmount() {
		return this.amount;
	}
	public String getSignature	() {
		return this.signature;
	}
	
	public boolean isDataValid() {
		return this.address != null
				&&  !this.address.equals("")
				&& this.amount > 0
				&& this.signature != null
				&& !this.signature.equals("");
	}
}
