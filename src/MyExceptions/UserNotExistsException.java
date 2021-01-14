package MyExceptions;

public class UserNotExistsException extends Exception {
    public UserNotExistsException(String s) {
        super(s);
    }
}
