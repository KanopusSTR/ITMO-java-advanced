package info.kgeorgiy.ja.rynk.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    /**
     * Utility class.
     */
    private Client() {
    }

    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        final String name = args.length >= 1 ? args[0] : "ivan";
        final String surname = args.length >= 2 ? args[1] : "ivanov";
        final String passport = args.length >= 3 ? args[2] : "1000";
        final String id = args.length >= 4 ? args[3] : "0";
        int diff;
        try {
            diff = args.length >= 5 ? Integer.parseInt(args[4]) : 0;
        } catch (NumberFormatException e) {
            System.out.println("You write incorrect sum, we use diff = 0");
            diff = 0;
        }

        String accountId = passport + ":" + id;

        Account account = bank.getAccount(accountId);
        Person person;

        if (account == null) {
            System.out.println("Creating account");
            System.out.println("Creating person");
            person = bank.createPerson(name, surname, passport);
            account = bank.createAccount(accountId);
        } else {
            person = bank.getRemotePerson(passport);
            System.out.println("Account already exists");
        }
        System.out.println("Person id: " + person.name() + " " + person.surname() + " " + person.passport());
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + diff);
        System.out.println("Money: " + account.getAmount());
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
}
