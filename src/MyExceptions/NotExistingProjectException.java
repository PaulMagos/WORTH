package MyExceptions;

public class NotExistingProjectException extends Exception {
    public NotExistingProjectException(String s) {
        super(s);
    }
}
