package WorthServer.Project;

import MyExceptions.*;

import java.util.List;

public interface Project {
    // Metodo per ottenere una lista dei membri del progetto
    List<String> getMembers();
    // Metodo per ottenere una lista delle card
    List<Card> getCards();
    // Metodo per ottenere la card il cui nome è name
    Card getCard(String name) throws NoSuchCardException;
    // Metodo per spostare una card da una lista a un altra
    void moveCard(String card_name, String old_listName , String new_listName)
            throws NullPointerException, InvalidStatusException, NoSuchCardException, CardMoveException, CardNotInListException;

    // Metodi per ottenere il nome del progetto, il nome del creatore e
    String getProjectName();
    String getCreator();

    // Metodo per aggiungere una lista di card preesistenti ( gestisce anche la lista in cui deve andare la card )
    void addCards(List<Card> cards);
    // Metodo per aggiungere un membro a un progetto
    void addProjectMember(String name) throws AlreadyMemberException;
    // Metodo per creare una card
    void createCard(String card, String sofo) throws CardExistsException;
}
