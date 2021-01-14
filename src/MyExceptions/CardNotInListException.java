package MyExceptions;

public class CardNotInListException extends Throwable {
    public CardNotInListException(String s) {
        super(s);
    }
}
