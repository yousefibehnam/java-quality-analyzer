package fx.rfc;

/*
 * Hand-derived RFC = |M| + |R|.
 *
 * Account
 *   M (declared): Account(), Account(int), deposit, withdraw, check, describe      = 6
 *   R (called, not declared here):
 *     Ledger.<init>()                 field initializer
 *     Ledger.record(String, int)      deposit, withdraw (counted once)
 *     Ledger.flush()                  withdraw
 *     IllegalArgumentException.<init>(String)   check
 *     String.valueOf(int)             describe
 *     String.trim()                   describe (receiver typed from valueOf's return type)  = 6
 *   this(0) and check(..) call methods already in M.                       RFC = 12
 *
 * Ledger: M = record, flush; R = {}                                         RFC = 2
 */
public class Account {

    private int balance;
    private final Ledger ledger = new Ledger();

    public Account() {
        this(0);
    }

    public Account(int initial) {
        balance = initial;
    }

    public void deposit(int amount) {
        check(amount);
        balance += amount;
        ledger.record("deposit", amount);
    }

    public void withdraw(int amount) {
        check(amount);
        balance -= amount;
        ledger.record("withdraw", amount);
        ledger.flush();
    }

    private void check(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public String describe() {
        return String.valueOf(balance).trim();
    }
}

class Ledger {

    void record(String what, int amount) {
    }

    void flush() {
    }
}
