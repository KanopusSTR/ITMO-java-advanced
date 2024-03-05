package info.kgeorgiy.ja.rynk.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {

    String name() throws RemoteException;

    String surname() throws RemoteException;

    String passport() throws RemoteException;

}
