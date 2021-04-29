package data;

public class ObtainCoinsParams {
	private String address;
	private double amount;
	
	public ObtainCoinsParams() { }
	
	public ObtainCoinsParams(String address, double amount) {
		this.address = address;
		this.amount = amount;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public double getAmount() {
		return this.amount;
	}
	
	public boolean isDataValid() {
		return this.address != null && !this.address.equals("") && this.amount > 0;
	}
}
