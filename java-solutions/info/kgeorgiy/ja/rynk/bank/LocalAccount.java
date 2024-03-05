package info.kgeorgiy.ja.rynk.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount implements Account, Serializable {
    private final String id;
    private int amount;

    public LocalAccount(final String id, final int amount) throws RemoteException {
        this.id = id;
        this.amount = amount;
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
