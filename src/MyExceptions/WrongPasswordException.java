package MyExceptions;

public class WrongPasswordException extends Exception {
    public WrongPasswordException(String wrongPassword){
        super(wrongPassword);
    }
}
