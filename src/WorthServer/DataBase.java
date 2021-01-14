package WorthServer;

import Client.ClientInterface;
import MyExceptions.*;
import WorthServer.Project.Card;
import WorthServer.Project.Project;
import WorthServer.Project.ProjectImpl;
import WorthServer.User.Account;
import WorthServer.User.User;

import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataBase extends RemoteServer implements RMIWORTHServer {
    private final String servent = "SERVER: ";

    // Lista dei progetti
    private List<Project> projectList;
    // Lista degli utenti
    private List<User> Users;
    // Lista degli utenti online
    private List<String> OnlineUsers;
    // Lista dei progetti "aperti" da almeno un utente
    private List<String> openedProjects;
    // Lista dei client per le callback
    private List<ClientInterface> clients;
    // FileManager per la gestione della persistenza
    private FileManager files;

    // Costruttore con parametro il path per il salvataggio dei dati
    public DataBase(String filesName) throws IOException, AlreadyBoundException {
        // Inizializzo le varie liste
        this.files = new FileManager(filesName);
        // Inizializzo la lista dei progetti con la lista dei progetti presenti in memoria
        // ( se vuota inizializzo solo la lista )
        this.projectList = Collections.synchronizedList(files.getProjects());

        // Inizializzo la lista degli utenti con la lista degli utenti presenti in memoria
        // ( se vuota inizializzo solo la lista )
        this.Users = Collections.synchronizedList(files.getUsers());

        // Inizializzo le altre liste ( Uso synchronizedList per per mettere l'accesso da parte di più thread )
        this.OnlineUsers = Collections.synchronizedList(new ArrayList<>());
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.openedProjects = Collections.synchronizedList(new ArrayList<>());

        // Avvio il Thread per la gestione delle connessioni TCP al server
        TCPHandler TCPHandler = new TCPHandler(this);
        TCPHandler.start();

        // Metto in "vista" le risorse RMI per la registrazione e per la callback
        RMIWORTHServer stub1 = (RMIWORTHServer) UnicastRemoteObject.exportObject(this, 30001);
        LocateRegistry.createRegistry(6789);
        LocateRegistry.createRegistry(7800);
        Registry registerRegistry = LocateRegistry.getRegistry("localhost", 6789);
        Registry updateRegistry = LocateRegistry.getRegistry( "localhost", 7800);
        registerRegistry.bind("WORTH", stub1);
        updateRegistry.bind("call", stub1);

        System.out.println("Server RMI per la registrazione avviato sulla porta: " + 6789);
        System.out.println("Server RMI per la callback avviato sulla porta: " + 7800);
    }


    // Metodo per accedere a un determinato progetto (verifica solamente se l'utente ne è membro)
    public boolean accessProject(String username, String projectName)
            throws NotExistingProjectException, UserNotMemberException, IOException {
        // Verifico i parametri
        if ( username == null || projectName == null) throw new NullPointerException("accessProject - Invalid Parameters");
        // Ottengo il progetto con il nome projectName
        Project tm = getProject(projectName);
        // Verifico che esista
        if  (tm == null) throw new NotExistingProjectException("Project " + projectName + " not exists");

        // Controllo che l'utente faccia parte del progetto
        if (tm.getMembers().contains(username)) {
            if (!openedProjects.contains(tm.getProjectName())){
                openedProjects.add(tm.getProjectName());
            }
            // Comunico l'accesso al progetto agli altri membri del progetto online sulla chat
            ServerCommunication(username + " opened " + '\"' + projectName + '\"', projectName);
            return true;
        }
        else throw new UserNotMemberException("User not part of this project");
    }
    // Metodo per creare un progetto
    public boolean createProject(String projectName, String Creator) throws ProjectAlreadyExistsException,
            IOException {
        // Controllo che non esista già un progetto con lo stesso nome
        for (Project i:projectList) if (i.getProjectName().equals(projectName))
            throw new ProjectAlreadyExistsException("Project already exists");
        // Creo il progetto
        Project tmp = new ProjectImpl(projectName, Creator);
        // Lo aggiungo alla lista dei progetti
        projectList.add(tmp);
        // Aggiorno i file locali per mantenere la persistenza
        files.updateProjects(tmp);
        return true;
    }
    // Metodo per aggiungere un membro al progetto
    public boolean addMember(String projectName, String userName)
            throws AlreadyMemberException, UserNotExistsException, IOException {
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if(tm != null) {
            // Controllo tra tutti gli utenti se esiste un utente con il nome userName
            for (User i: Users)
                // Se esiste aggiungo il suo nome ai membri del team e
                // aggiorno i file del database per mantenere la persistenza
                if (i.getName().equals(userName)) {
                    tm.addProjectMember(userName);
                    files.updateProjects(tm);
                    ServerCommunication(userName + " added to " + projectName, projectName);
                    return true;
                }
            throw new UserNotExistsException("");
        }
        throw new NullPointerException("addMember - No project with " + projectName + " name");
    }
    // Metodo per vedere la lista di membri di un certo progetto
    public String showMembers(String projectName){
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if(tm != null) {
            // Creo una stringa con tutti i nomi dei membri separati da uno spazio e la ritorno
            List<String> t = tm.getMembers();
            if (t==null) return null;
            StringBuilder out = new StringBuilder();
            for (String i : t) out.append(i).append(" ");
            return out.toString();
        }
        else throw new NullPointerException("showMembers - No project with " + projectName + " name");
    }
    // Metodo per ottenere tutti i nomi delle card di un progetto
    public String showCards(String projectName){
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if(tm != null) {
            // Creo una stringa con tutti i nomi delle card separati da uno spazio e la ritorno
            List<Card> t = tm.getCards();
            if (t==null) return null;
            StringBuilder out = new StringBuilder();
            for (Card i : t) out.append(i.getName()).append(" ");
            return out.toString();
        }
        else throw new NullPointerException("showCards - No project with " + projectName + " name");
    }
    // Metodo per ottenere informazioni su una specifica card di un progetto
    public String showCard(String projectName, String cardName) throws NoSuchCardException {
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if(tm != null) {
            // Ottengo la card relativa al nome cardName
            Card tmp = tm.getCard(cardName);
            // Se questa esiste
            if (tmp != null){
                // ritorno una stringa con "nome descrizione"
                return tmp.getName() + " " + tmp.getDescription();
            }
        }
        throw new NullPointerException("showCard - No project with " + projectName + " name");
    }
    // Metodo per aggiungere una card a un progetto ( e quindi crearla )
    public boolean addCard(String projectName, String cardName, String description)
            throws CardExistsException, NotExistingProjectException, IOException {
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if (tm != null) {
            // Creo la card
            tm.createCard(cardName, description);
            // Aggiorno i file su disco con la nuova card
            files.updateProjects(tm);
            // Comunico la modifica alla chat del gruppo relativo al progetto
            ServerCommunication('\"' + cardName + '\"' + " card created", projectName);
            return true;
        }
        else throw new NotExistingProjectException("addCard - No project with " + projectName + " name");
    }
    // Metodo per muovere una card da una lista di partenza a una lista di destinazione
    public boolean moveCard(String projectName, String cardName, String listaPartenza, String listDestinazione)
            throws NoSuchCardException, CardMoveException, InvalidStatusException, CardNotInListException, IOException {
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if (tm != null) {
            // Sposto la card dalla lista di partenza alla lista di destinazione
            // ( se si verificano le condizioni in move card ovviamente )
            tm.moveCard(cardName, listaPartenza, listDestinazione);
            // Aggiorno i file per rendere persistente la modifica;
            files.updateProjects(tm);
            // Comunico l azione alla chat del gruppo relativa al progetto
            ServerCommunication(" Moved " + "\"" + cardName + "\"" + " from " + listaPartenza + " to " + listDestinazione, projectName);
            return true;
        }
        throw new NullPointerException("moveCard - No project with " + projectName + " name");
    }
    // Metodo per ottenere lo storico dei movimenti tra le liste di una determinata card
    public String getCardHistory(String projectName, String cardName) throws NoSuchCardException {
        // Ottengo il progetto relativo al nome projectName
        Project tm = getProject(projectName);
        StringBuilder history = new StringBuilder();
        // Pattern per la data del movimento (più leggibile)
        String pattern = "dd/MM/yyyy HH:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        // Se il progetto esiste
        if (tm != null) {
            // Creo una stringa formata da
            // "stato_card_1 data_creazione_card_1 .....  stato_card_n data_creazione_card_n"
            // (nell ipotesi di un numero di spostamenti ragionevoli)
            for (String i: tm.getCard(cardName).getUpdates()) {
                history.append(Card.getState(i));
                history.append(" ");
                history.append(simpleDateFormat.format(Card.getDate(i)));
                history.append(" ");
            }
            return history.toString();
        }
        else throw new NullPointerException("addCard - No project with " + projectName + " name");
    }
    // Metodo per cancellare un progetto
    public boolean cancelProject(String projectName, String userName) throws ProjectNotDoneException, IOException {
        // Ottengo il progetto con nome projectName
        Project tm = getProject(projectName);
        // Se questo esiste
        if (tm != null) {
            // Controllo che il progetto sia finito ( ovvero tutte le card siano in done o non esistano card )
            // Controllo che l'utente sia membro del progetto
            if(((ProjectImpl)tm).isDone() && tm.getMembers().contains(userName)) {
                // Comunico l'eliminazione ai membri del progetto sulla chat
                ServerCommunication("PROJECT DELETED BY "+ userName, projectName);
                // Rimuovo il progetto dai progetti del database
                projectList.remove(tm);
                // Aggiorno i file rimuovendo tutti i dati persistenti relativi al progetto in questione
                files.updateProjects(tm, "delete");
                return true;
            }
            else throw new ProjectNotDoneException("Project "+ projectName + " has cards not done");
        }
        else throw new NullPointerException("No project with " + projectName + " name");
    }
    // Metodo per ottenere un determinato progetto
    private Project getProject(String projectName) {
        for (Project i : projectList)
            // Controllo tra tutti i progetti se esiste un progetto con nome projectName e lo restituisco nel caso
            if (i.getProjectName().equals(projectName)) {
                return i;
            }
        // Se non c'è restituisco null
        return null;
    }

    // RMI SERVER Method for register users
    @Override
    public synchronized boolean register(String name, String password)
            throws IOException, UserAlreadyPresentException {
        if (name == null || password == null || name.equals("") || password.equals(""))
            throw new NullPointerException("Register - Invalid Parameter");

        // Aggiorno la lista degli utenti del sistema e controllo che non sia già presente
        Users = files.getUsers();
        for (User i: Users){
            if(i.getName().equals(name))
                throw new UserAlreadyPresentException("Register - User " + name + " already exists");
        }
        // Creo e aggiungo l'utente al database
        User user = new Account(name, password);
        Users.add(user);
        // Inoltre aggiorno il i file persistenti con il nuovo utente
        return files.updateUsers(user);
    }

    // RMI SERVER Methods for callback
    @Override
    // Metodo per aggiungersi alla lista
    public synchronized void registerForCallback(ClientInterface stub) throws RemoteException {
        // Se l'utente non è presente lo aggiungo alla lista dei client
        // a cui potrò fare la callback per informarli di un certo avvenimento
        if (!clients.contains(stub)){
            clients.add(stub);
            doCallBacks();
        }else System.out.println("cant register client");
    }
    @Override
    // Metodo per eliminarsi dalla lista
    public synchronized void unregisterForCallback(ClientInterface stub) {
        // Provo a rimuovere il client dalla lista di client che sono disponibili alla callback
        if (clients.remove(stub)){
            try {
                // Comunico che l'utente relativo al client che si era connesso è uscito agli altri membri nella chat
                ServerCommunication(stub.getUserName() + " exit", stub.getProjectName());
            } catch (IOException e) {
                System.out.println("Errore comunicazione server");
            }
        }else System.out.println("Unable to release callback");
    }
    // Metodo per fare la callback ai client
    public synchronized void doCallBacks() throws RemoteException {
        // Creo una hash map
        Map<String, String> out = new HashMap<>();
        // Aggiungo tutti gli utenti nella rete e per quelli online aggiungo come valore nella hashmap "Online",
        // "Offline" per gli altri
        for (User i : Users)  out.put(i.getName(), (OnlineUsers.contains(i.getName())? "Online":"Offline"));
        // Genero un iterator per poter scorrere la lista dei client per la callback
        Iterator<ClientInterface> i = clients.iterator();
        int clientsize = clients.size();
        while (i.hasNext()){
            // Itero e informo tutti i client del nuovo avvenimento (che un utente è entrato o uscito)
            ClientInterface client = i.next();
            try {
                client.notifyMe(out);
            }catch (RemoteException e){
                // Se non riesco tolgo il client dalla lista degli utenti per la callback
                clients.remove(client);
                return;
            }
        }
    }

    // Metodo per il login nel server
    public synchronized boolean login(String name, String password) throws WrongPasswordException, IOException, UserOnlineException {
        if (name == null || password == null || name.equals("") || password.equals(""))
            throw new NullPointerException("Login - Invalid Parameter");
        // Aggiorno la lista degli utenti del sistema
        Users = files.getUsers();

        // Controllo per ogni utente presente se il suo nome è uguale a quello inserito
        for (User i: Users){
            if (i.getName().equals(name)){
                // Verifico che la password inserita sia valida e che l'utente non sia già online
                if(i.verifyPassword(password) && !OnlineUsers.contains(i.getName())) {
                    // Aggiorno la lista degli utenti online e faccio la callback su tutti i client per informarli
                    OnlineUsers.add(i.getName());
                    // Effettuo le callback per informare gli altri utenti
                    doCallBacks();
                    return true;
                }else if(OnlineUsers.contains(i.getName()))
                    throw new UserOnlineException("User " + name + " already online");
            }
        }
        return false;
    }
    // Metodo per il logout dal server
    public synchronized boolean logout(String name) throws RemoteException {
        if (name == null) throw new NullPointerException("Login - Invalid Parameter");
        // Scorro la lista degli utenti per trovare il nome di quello che vuole uscire
        for (User i: Users){
            if (i.getName().equals(name)){
                // Se l'utente risulta online aggiorno la lista degli utenti online
                if(OnlineUsers.contains(i.getName())){
                    OnlineUsers.remove(i.getName());
                    // Informo tutti i client attraverso la callback
                    doCallBacks();
                    return true;
                }else return false;
            }
        }
        return false;
    }

    // Ritorno un indirizzo per la chat del progetto di nome s
    public String getChat(String s) {
        // Cerco il progetto per poter stabilire poi la sua posizione nella lista dei progetti
        Project tm = getProject(s);
        StringBuilder ip = new StringBuilder();
        // Inizio la stringa con 239 per partire direttamente da un indirizzo multicast
        ip.append("239.");
        if (tm!=null) {
            int index = projectList.indexOf(tm);
            // Divido il numero per ottenere gli altri byte, primo byte index/2^16,
            // Secondo byte di cui necessito (index/2^8)%2^8
            // ( se maggiore, ci penserà la precedente divisione per 2^16 a gestire il resto dell indirizzo),
            // poi faccio il mod del numero per ottenere l ultimo byte
            ip.append(( index/256)/256).append(".").append((index/256) %256).append(".").append(index % 256);
        }
        try {
            //Controllo che sia un indirizzo multicast valido
            if (InetAddress.getByName(ip.toString()).isMulticastAddress())
                return ip.toString();
        } catch (UnknownHostException e) {
            return "Non multicast address found";
        }
        return s;
    }
    // Ritorna una stringa che contiene tutti i progetti di un certo utente name, separati da uno spazio
    public String listProjects(String name){
        if (name==null) throw new NullPointerException("listProjects - Invalid parameter");

        StringBuilder str = new StringBuilder();
        // Controllo tutti i progetti di cui è membro
        for(Project i: projectList){
            if(i.getMembers().contains(name)) {
                // "appendo" il progetto alla stringa (concateno il nome del progetto e uno spazio)
                str.append(i.getProjectName());
                str.append(" ");
            }
        }
        return str.toString();
    }

    private void ServerCommunication(String content, String prj) throws IOException {
        // Creo la socket
        MulticastSocket socket = new MulticastSocket();
        // Se sono su mac devo verificare l interfaccia che uso (MacOS usa determinate funzioni per AirDrop)
        if ((System.getProperty("os.name").toLowerCase()).startsWith("mac os"))
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));
        // Genero il byte del messaggio che devo inviare
        byte[] data = (servent + content).getBytes();
        // Creo il DatagramPacket che devo inviare, con l indirizzo e la porta
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(getChat(prj)), 5000);
        // Se almeno un utente ha aperto il progetto in quel momento, lo informo
        if(openedProjects.contains(prj)) {
            socket.send(packet);
        }
    }
}
