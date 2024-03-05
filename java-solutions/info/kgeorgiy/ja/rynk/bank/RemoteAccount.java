package info.kgeorgiy.ja.rynk.bank;

import java.rmi.RemoteException;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;

    public RemoteAccount(final String id) throws RemoteException {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }


    @Override
    public synchronized int getAmount() throws RemoteException {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) throws RemoteException {
        this.amount = amount;
    }
}
