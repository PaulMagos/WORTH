package WorthServer;

import MyExceptions.WrongPasswordException;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class Account implements User {

    // Account informations
    private String name;
    private String password;
    private final Date subscriptionTime;

    // Costruttore
    public Account(String name, String password){
        this.name = name;
        this.subscriptionTime = new Date();
        try {
            MessageDigest msgD = MessageDigest.getInstance("MD5");
            msgD.update(password.getBytes());
            byte[] digest = msgD.digest();
            this.password = DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // Costruttore per jackson
    public Account(){
        this.name = null;
        this.password = null;
        this.subscriptionTime = null;
    }

    // Metodi Getter per le informazioni dell'utente
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Date getSubscriptionTime() {
        return this.subscriptionTime;
    }

    // Verifico la password, se in chiaro
    @Override
    public boolean verifyPassword(String pwd) throws NullPointerException, WrongPasswordException {
            if(pwd.equals(this.password)) return true;
            else throw new WrongPasswordException("WrongPassword");
         // Non avevo previsto che successivamente avrei utilizzato lo scambio della password solo attraverso l'hash md5
        /*String myHash = null;
        try {
            MessageDigest msgD = MessageDigest.getInstance("MD5");
            msgD.update(pwd.getBytes());
            byte[] digest = msgD.digest();
            myHash = DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert myHash != null;
        if  (myHash.equals(this.password)){
            return true;
        }else throw new WrongPasswordException("WrongPassword");*/
    }
}
