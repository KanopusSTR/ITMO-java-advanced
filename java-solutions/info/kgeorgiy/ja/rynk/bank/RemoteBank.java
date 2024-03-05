package info.kgeorgiy.ja.rynk.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(final String subId) throws RemoteException {
        if (subId.chars().filter(ch -> ch == ':').count() > 1) {
            System.err.println("incorrect subId");
            return null;
        }
        final Account account = new RemoteAccount(subId);
        if (accounts.putIfAbsent(subId, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(subId);
        }
    }

    @Override
    public Account getAccount(final String id) {
        return accounts.get(id);
    }

    @Override
    public Person createPerson(String name, String surname, String passport) throws RemoteException {
        final Person person = new RemotePerson(name, surname, passport);
        if (persons.putIfAbsent(passport, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passport);
        }
    }

    @Override
    public Person getRemotePerson(String passport) throws RemoteException {
        return persons.get(passport);
    }

    @Override
    public LocalPerson getLocalPerson(String passport) throws RemoteException {
        Person person = persons.get(passport);
        if (person == null) {
            return null;
        }
        Set<LocalAccount> personAccounts = new HashSet<>();
        for (String id : accounts.keySet()) {
            if (id.contains(passport + ":")) {
                personAccounts.add(new LocalAccount(id, accounts.get(id).getAmount()));
            }
        }
        return new LocalPerson(person.name(), person.surname(), person.passport(), personAccounts);
    }

    @Override
    public ConcurrentMap<String, Account> getAccounts() {
        return accounts;
    }

}
