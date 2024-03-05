package info.kgeorgiy.ja.rynk.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param subId account id
     * @return created or existing account.
     */
    Account createAccount(String subId) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    Person createPerson(String name, String surname, String passport) throws RemoteException;

    Person getRemotePerson(String passport) throws RemoteException;

    LocalPerson getLocalPerson(String passport) throws RemoteException;

    Map<String, Account> getAccounts() throws RemoteException;
}
