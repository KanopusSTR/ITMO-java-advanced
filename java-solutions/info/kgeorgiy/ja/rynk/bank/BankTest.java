package info.kgeorgiy.ja.rynk.bank;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankTest {

    private final static int DEFAULT_PORT = 8888;
    private static Bank bank;


    @BeforeClass
    public static void startService() {
        bank = new RemoteBank(DEFAULT_PORT);
        try {
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            e.printStackTrace();
            assert false;
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }

    @Test
    public void test1_getNull() throws RemoteException {
        Assert.assertNull(bank.getRemotePerson("1"));
        Assert.assertNull(bank.getLocalPerson("1"));
    }

    @Test
    public void test2_remoteAmount() throws RemoteException {
        String passport = "0001";
        Person arturPerson = bank.createPerson("Artur", "Rynk", passport);
        Account arturAccount = bank.createAccount(passport + ":1");
        int amount = arturAccount.getAmount();
        arturAccount.setAmount(arturAccount.getAmount() + 200);
        Assert.assertEquals(amount + 200, arturAccount.getAmount());
        Assert.assertEquals(bank.getRemotePerson(passport), arturPerson);
        LocalPerson localPerson = bank.getLocalPerson(passport);
        arturAccount.setAmount(arturAccount.getAmount() + 200);
        Assert.assertEquals((localPerson.accounts()).iterator().next().getAmount() + 200, arturAccount.getAmount());
        Assert.assertEquals(bank.getRemotePerson(passport), arturPerson);
    }

    @Test
    public void test3_localAmount() throws RemoteException {
        String passport = "0002";
        bank.createPerson("Artur", "Rynk", passport);
        Account arturAccount = bank.createAccount(passport + ":1");
        LocalPerson localPerson = bank.getLocalPerson(passport);
        arturAccount.setAmount(arturAccount.getAmount() + 200);
        Assert.assertEquals((localPerson.accounts()).iterator().next().getAmount() + 200, arturAccount.getAmount());
    }

    @Test
    public void test4_manyPerson() throws RemoteException {
        for (int i = 0; i < 10; ++i) {
            String passport = Integer.toString(i).repeat(4);
            bank.createPerson("name" + i, "surname" + i, passport);
            Account account = bank.createAccount(passport + ":1");
            account.setAmount(account.getAmount() + 100);
        }

        for (int i = 0; i < 10; ++i) {
            String passport = Integer.toString(i).repeat(4);
            Person person = bank.getRemotePerson(passport);
            Assert.assertEquals(person.name(), "name" + i);
            Assert.assertEquals(person.surname(), "surname" + i);

            Account account = bank.getAccount(passport + ":1");
            Assert.assertEquals(account.getAmount(), 100);
        }
    }

    @Test
    public void test5_client() throws RemoteException {
        Client.main("ivan", "ivanov", "1000", "1", "100");
        LocalPerson person = bank.getLocalPerson("1000");
        Assert.assertNotNull(person);
        Assert.assertEquals(person.accounts().iterator().next().getAmount(), 100);
    }

    @Test
    public void test5_forbiddenChar() throws RemoteException {
        Map<String, Account> accounts = bank.getAccounts();
        for (String id : accounts.keySet()) {
            Assert.assertTrue(id.chars().filter(ch -> ch == ':').count() > 1);
        }
    }

    @Test
    public void test6_parallel() throws RemoteException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        String passport = "0003";
        bank.createPerson("Artur", "Rynk", passport);
        Account arturAccount = bank.createAccount(passport + ":1");

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                try {
                    arturAccount.setAmount(arturAccount.getAmount() + 100);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdownNow();
        executorService.close();
        Assert.assertEquals(arturAccount.getAmount(), 1000);
    }
}
