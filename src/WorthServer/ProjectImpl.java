package WorthServer;

import MyExceptions.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class ProjectImpl implements Project {

    // Simply enum
    enum cardList{
        TODO,
        INPROGRESS,
        TOBEREVISITED,
        DONE
    }

    // Attributi del progetto, Lista dei membri e Liste delle card
    private String projectName;
    private String creator;
    private List<String> members;
    private List<Card> TODO;
    private List<Card> INPROGRESS;
    private List<Card> TOBEREVISITED;
    private List<Card> DONE;

    // Costruttore
    public ProjectImpl(String projectName, String Creator) {
        if (projectName == null || Creator == null) throw new NullPointerException("ProjectImpl - Invalid Parameters");
        this.projectName = projectName;
        this.creator = Creator;

        // Inizializzo e aggiungo il creatore alla lista dei membri
        this.members = new ArrayList<>();
        this.members.add(Creator);

        // Inizializzo le varie liste
        this.TODO = new ArrayList<>();
        this.INPROGRESS = new ArrayList<>();
        this.TOBEREVISITED = new ArrayList<>();
        this.DONE = new ArrayList<>();
    }

    // Costruttore per jackson
    public ProjectImpl() {
        // Inizializzo tutti gli argomenti
        this.projectName = null;
        this.creator = null;
        this.members = new ArrayList<>();
        this.TODO = new ArrayList<>();
        this.INPROGRESS = new ArrayList<>();
        this.TOBEREVISITED = new ArrayList<>();
        this.DONE = new ArrayList<>();
    }

    // Metodo per aggiungere un membro al team del progetto
    public void addProjectMember(String name) throws AlreadyMemberException {
        // Verifico se il parametro è valido
        if (name == null) throw new NullPointerException("AddProjectMember - Invalid Parameter");
        // Verifico e il membro fa già parte del team
        if (this.members.contains(name)) throw new AlreadyMemberException("AddProjectMember - Already in");
        // Aggiungo l'utente ai membri
        this.members.add(name);
    }

    //Creo una carta e la aggiungo alla lista TOD0
    public void createCard(String name, String description) throws CardExistsException {
        // Verifico la correttezza dei parametri;
        if (name == null || description == null) throw new NullPointerException("createCard - Invalid Parameters");
        // Verifico che non ci siano card con lo stesso nome in nessuna delle liste
        for (Card i :TODO) if (i.getName().equals(name))
            throw new CardExistsException("createCard - Already present card");
        for (Card i :INPROGRESS) if (i.getName().equals(name))
            throw new CardExistsException("createCard - Already present card");
        for (Card i :TOBEREVISITED) if (i.getName().equals(name))
            throw new CardExistsException("createCard - Already present card");
        for (Card i :DONE) if (i.getName().equals(name))
            throw new CardExistsException("createCard - Already present card");

        // Creo la card e la aggiungo alla lista TOD0
        Card tmp = new Card(name, description);
        this.TODO.add(tmp);
    }

    // Ritorno una copia della lista dei membri
    public List<String> getMembers(){
        return new ArrayList<>(this.members);
    }

    @JsonIgnore
    // Metodo per ritornare tutte le card presenti nel progetto
    public List<Card> getCards(){
        // Inizializzo una lista
        List<Card> t = new ArrayList<>();
        // Aggiungo tutte le card di tutte le liste
        t.addAll(TODO);
        t.addAll(INPROGRESS);
        t.addAll(TOBEREVISITED);
        t.addAll(DONE);
        return t;
    }

    // Restituisco lo storico dei movimenti di una Card
    public List<String> getCardHistory(String card_name) throws NoSuchCardException {
        // Verifico i parametri
        if (card_name == null) throw new NullPointerException("CardHistory - InvalidParameter");
        // Verifico che esista in almeno una delle 4 liste
        if (!(TODO.contains(getCard(card_name)) || INPROGRESS.contains(getCard(card_name)) ||
                TOBEREVISITED.contains(getCard(card_name)) || DONE.contains(getCard(card_name))))
                    throw new NoSuchCardException("CardHistory - Invalid Card");

        // Prendo la card col metodo getCard e restituisco una copia della lista degli aggiornamenti
        return new ArrayList<>(getCard(card_name).getUpdates());
    }

    // Restituisco la card con nome , name
    public Card getCard(String name) throws NoSuchCardException {
        // Verifico il parametro
        if (name == null) throw new NullPointerException("getCard - Invalid Parameter");
        // Cerco la card in tutte le liste finchè non la trovo, se non esiste Exception
        for (Card i: TODO) if(i.getName().equals(name)) return i;
        for (Card i: INPROGRESS) if(i.getName().equals(name)) return i;
        for (Card i: TOBEREVISITED) if(i.getName().equals(name)) return i;
        for (Card i: DONE) if(i.getName().equals(name)) return i;
        throw new NoSuchCardException("getCard - Card not exists");
    }

    @JsonIgnore
    // Verifico che tutte le liste , se non la DONE , siano vuote, per stabilire se il progetto è eliminabile
    public boolean isDone(){
        return TOBEREVISITED.isEmpty() && TODO.isEmpty() && INPROGRESS.isEmpty();
    }

    // Muovo un card da una lista di partenza a una lista di destinazione
    public void moveCard(String card_name, String old_listName, String new_listName)
            throws NullPointerException, InvalidStatusException, NoSuchCardException, CardMoveException, CardNotInListException {
        // Verifico i parametri
        if (new_listName == null || old_listName == null)
            throw new NullPointerException("shiftCard - Invalid Parameter");
        // Trasformo la stringa in un valore della mia enum cardList
        cardList list_name = toStaticCardlist(new_listName);
        assert list_name != null;
        // Verifico che non si voglia spostare la card in TOD0
        if (list_name.equals(cardList.TODO)) {
            throw new InvalidStatusException("shiftCard - Card can't downgraded to TODO or is already in TODO");
        }

        // Ottengo la card con nome card_name
        Card card = getCard(card_name);

        switch (Objects.requireNonNull(toStaticCardlist(new_listName))) {
            case DONE:
                // Se voglio spostarla da TOBEREVISITED a DONE
                if(toStaticCardlist(old_listName).equals(cardList.TOBEREVISITED)){
                    // Verifico che sia in TOBEREVISITED
                    if(!TOBEREVISITED.contains(card)) throw new CardNotInListException("");
                    // La aggiungo a DONE e la rimuovo da TOBEREVISITED
                    DONE.add(card);
                    TOBEREVISITED.remove(card);
                    // Aggiorno lo storico dei movimenti con il nuovo movimento
                    card.update(cardList.DONE);
                    break;
                }
                // Se voglio spostarla da INPROGRESS a DONE
                else if(toStaticCardlist(old_listName).equals(cardList.INPROGRESS)){
                    // Verifico che sia in INPROGRESS
                    if(!INPROGRESS.contains(card)) throw new CardNotInListException("");
                    // La aggiungo a DONE e la rimuovo da INPROGRESS
                    DONE.add(card);
                    INPROGRESS.remove(card);
                    // Aggiorno lo storico dei movimenti con il nuovo movimento
                    card.update(cardList.DONE);
                    break;
                }else {
                    throw new CardMoveException("moveCard - Can't move card from "
                            + old_listName + " to " + new_listName);
                }
            case TOBEREVISITED:
                // Se voglio spostarla da INPROGRESS in TOBEREVISITED
                if(toStaticCardlist(old_listName).equals(cardList.INPROGRESS)){
                    // Verifico che sia in INPROGRESS
                    if(!INPROGRESS.contains(card)) throw new CardNotInListException("");
                    // La aggiungo a TOBEREVISITED e la rimuovo da INPROGRESS
                    TOBEREVISITED.add(card);
                    INPROGRESS.remove(card);
                    // Aggiorno lo storico dei movimenti con il nuovo movimento
                    card.update(cardList.TOBEREVISITED);
                    break;
                }else {
                    throw new CardMoveException("moveCard - Can't move card from "
                            + old_listName + " to " + new_listName);
                }
            case INPROGRESS:
                // Se voglio spostarla da TOD0 in INPROGRESS
                if(toStaticCardlist(old_listName).equals(cardList.TODO)){
                    // Verifico che sia in TOD0
                    if(!TODO.contains(card)) throw new CardNotInListException("");
                    // La aggiungo a INPROGRESS e la rimuovo da TOD0
                    INPROGRESS.add(card);
                    TODO.remove(card);
                    // Aggiorno lo storico dei movimenti con il nuovo movimento
                    card.update(cardList.INPROGRESS);
                    break;
                }
                // Se voglio spostarla da INPROGRESS in TOBEREVISITED
                else if(toStaticCardlist(old_listName).equals(cardList.TOBEREVISITED)){
                    // Verifico che sia in TOBEREVISITED
                    if(!TOBEREVISITED.contains(card)) throw new CardNotInListException("");
                    // La aggiungo a INPROGRESS e la rimuovo da TOBEREVISITED
                    INPROGRESS.add(card);
                    TOBEREVISITED.remove(card);
                    // Aggiorno lo storico dei movimenti con il nuovo movimento
                    card.update(cardList.INPROGRESS);
                    break;
                }else {
                    throw new CardMoveException("moveCard - Can't move card from "
                            + old_listName + " to " + new_listName);
                }

        }
    }

    // GETTER E SETTER per gli attributi del progetto
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName){
        this.projectName = projectName;
    }
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator){
        this.creator = creator;
    }
    public void setMembers(List<String> members){
        this.members = members;
    }

    // Metodo per aggiungere una lista di card (preesistenti e con una lista di movimenti)
    // al progetto con il relativo storico di movimenti
    public void addCards(List<Card> cards){
        // Verifico i parametri
        if (cards == null) throw new NullPointerException("addCards - Invalid Parameters");
        // Scorro la lista
        for (Card i : cards) {
            // Per ogni card ottengo il suo ultimo stato dal suo ultimo movimento
            ProjectImpl.cardList cstate = i.getState();
            // Aggiungo la card alla lista dovuta in base a cstate
            switch (cstate){
                case TODO:
                    TODO.add(i);
                    break;
                case INPROGRESS:
                    INPROGRESS.add(i);
                    break;
                case TOBEREVISITED:
                    TOBEREVISITED.add(i);
                    break;
                case DONE:
                    DONE.add(i);
                    break;
            }
        }
    }

    // Metodo static per "parsare" la lista passata come stringa nel parametro name
    protected static cardList toStaticCardlist(String name) throws NullPointerException{
        if (name == null) throw new NullPointerException("toCardList - Invalid Parameter");
        switch (name.toUpperCase()){
            case "TODO":
                return cardList.TODO;
            case "INPROGRESS":
                return cardList.INPROGRESS;
            case "TOBEREVISITED":
                return cardList.TOBEREVISITED;
            case "DONE":
                return cardList.DONE;
        }
        throw new NullPointerException("toCardList - No match for " + name);
    }
}
