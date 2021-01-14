package MyExceptions;

public class CardMoveException extends Exception {
    public CardMoveException(String moveCard) {
        super(moveCard);
    }
}
