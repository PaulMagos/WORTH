package MyExceptions;

public class UserAlreadyPresentException extends Exception {
    public UserAlreadyPresentException(String s) {
        super(s);
    }
}
