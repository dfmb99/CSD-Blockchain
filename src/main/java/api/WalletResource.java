package api;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import data.ObtainCoinsParams;
import data.TransferCoinsParams;
import data.Transaction;

@Singleton
@Path("/wallet")
public class WalletResource {

	private final String base_dir = System.getProperty("java.io.tmpdir");
	private final String filename = "transactions.data";
	private final Map<String, Double> userBalances;
	private final List<Transaction> transactions;
	private final ObjectOutputStream out;

	public WalletResource() throws Exception {
		this.transactions = new ArrayList<>();
		this.userBalances = new HashMap<>();

		try {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(base_dir + filename));
			while (true) {
				try {
					this.transactions.add((Transaction) is.readObject());
				} catch (EOFException e) {
					break;
				}
			}
			
			this.updateAccountBalances();
		} catch (IOException e) {
			// creates file
			ObjectOutputStream os1 = new ObjectOutputStream(new FileOutputStream(base_dir + filename));
			os1.close();
		}
		out = new ObjectOutputStream(new FileOutputStream(base_dir + filename, true)) {
			protected void writeStreamHeader() throws IOException {
				reset();
			}
		};
	}

	@POST
	@Path("/receive")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public double obtainCoins(ObtainCoinsParams p) throws IOException {
		if (!p.isDataValid()) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		String addr = p.getAddress();
		Double amnt = p.getAmount();
		Transaction t = new Transaction(null, addr, amnt);
		Double currBalance = userBalances.get(addr);
		synchronized (this) {
			out.writeObject(t);
			transactions.add(t);
			if (currBalance == null) {
				userBalances.put(addr, amnt);
			} else {
				userBalances.put(addr, currBalance + amnt);
			}
		}

		return userBalances.get(addr);
	}

	@POST
	@Path("/send")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void transferCoins(TransferCoinsParams p) throws IOException {
		if (!p.isDataValid()) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		String sender = p.getSender();
		String receiver = p.getReceiver();
		Double amount = p.getAmount();
		if (!hasSpendableBalance(sender, amount)) {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		synchronized (this) {
			Transaction t = new Transaction(sender, receiver, amount);
			out.writeObject(t);
			transactions.add(t);
			userBalances.put(sender, userBalances.get(sender) - amount);
			userBalances.merge(receiver, amount, Double::sum);
		}
	}

	@GET
	@Path("/{addr}")
	@Produces(MediaType.APPLICATION_JSON)
	public double getBalanceOf(@PathParam("addr") String addr) {
		Double balance = userBalances.get(addr);
		if (balance != null) {
			return balance;
		}
		return 0.0;
	}

	@GET
	@Path("/allTransactions")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Transaction> getTransactionsData() {
		return transactions;
	}

	@GET
	@Path("/transactions/{addr}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Transaction> getTransactionsOf(@PathParam("addr") String addr) {
		return transactions.stream().filter(t -> t.getReceiver() == addr || t.getSender() == addr)
				.collect(Collectors.toList());
	}

	/**
	 * Returns true if address 'address' has a balance of >= 'amount'
	 * 
	 * @param address - address to check balance
	 * @param amount  - minimum amount of coins address must hold to return true
	 */
	private boolean hasSpendableBalance(String address, double amount) {
		Double currBalance = userBalances.get(address);
		if (currBalance != null && currBalance >= amount) {
			return true;
		}
		return false;
	}

	/**
	 * Updates account balances based on transactions data 
	 */
	private void updateAccountBalances() {
		// Sync transaction list and updates every account balance in memory
		Double currBalance;
		for (Transaction t : transactions) {
			String receiver = t.getReceiver();
			String sender = t.getSender();
			double amnt = t.getAmount();

			currBalance = userBalances.get(receiver);
			if (currBalance == null) {
				userBalances.put(receiver, amnt);
			} else {
				userBalances.put(receiver, currBalance + amnt);
			}
			if (sender != null) {
				userBalances.put(sender, userBalances.get(sender) - amnt);
			}
		}
	}

}
