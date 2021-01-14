import MyExceptions.WrongPasswordException;

import java.util.Date;

public interface User {
    String getName();
    /* EFFECTS: return il nome dell'utente this */

    Date getSubscriptionTime();
    /* EFFECTS: return la data di iscrizione dell'utente this */

    String getPassword();
    /* EFFECTS: return l'hash della password dell'utente this */


    boolean verifyPassword(String pwd) throws NullPointerException, WrongPasswordException;
    /*
    *  REQUIRES: pwd != null && pwd == this.password
    *  THROWS: NullPointerException se pwd è null (unchecked)
    *          WrongPasswordException se pwd è errata (checked)
    *  EFFECTS: return true se la password è corretta, false altrimenti
    */
}
