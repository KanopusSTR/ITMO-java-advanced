package info.kgeorgiy.ja.rynk.bank;

import java.io.Serializable;
import java.util.Set;

public record LocalPerson(String name, String surname, String passport,
                          Set<LocalAccount> accounts) implements Person, Serializable {

}
