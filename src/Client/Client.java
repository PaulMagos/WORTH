package Client;
import Client.Frames.LoginFrame;
import Client.Frames.MenuFrame;
import Client.Frames.ProjectFrame;
import WorthServer.RMIWORTHServer;
import MyExceptions.UserAlreadyPresentException;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.lang.System.exit;

public class Client extends RemoteServer implements Runnable, ClientInterface {
    public static final int RMI_PORT = 6789;
    public static final int TCP_Port = 9000;
    public static final int Chat_Port = 5000;
    private final String ServerIP;

    // Le varie interfacce
    private LoginFrame inter;
    private MenuFrame menu;
    private ProjectFrame prj;

    // Dati relativi al nome dell'utente loggato e al progetto se vi ha fatto accesso
    private String userName;
    private String projectName;
    // Indirizzo ip della chat relativa al progetto, dopo avervi fatto accesso
    private String  ipForChat;
    // Client per ricevere ed inviare messaggi sulla Multicast
    private ChatClient chat;
    // Lista degli utenti
    private Map<String, String> users;

    // Socket address del server TCP per tutte le operazioni quali :
    // Login, Accesso e creazione di progetti, Aggiunta di membri al team,
    // Modifica e visualizzazione delle informazioni relative ad una card,
    // Visualizzazione di tutti i membri del team, e di tutte le card,
    // Eliminazione del progetto e logout
    private SocketAddress address;
    private SocketChannel channel;

    // RMI server attributes
    RMIWORTHServer RMIserver;
    Registry registry;
    ClientInterface callStub;

    private final boolean interfaceTypeGUI;


    // Costruttore con indirizzo
    public Client(String indirizzo, String interfaceC) throws IOException, NotBoundException {
        userName = null;
        projectName = null;
        inter = null;
        menu = null;
        this.ServerIP = indirizzo;
        this.interfaceTypeGUI = !(interfaceC.equals("terminal"));
        address = new InetSocketAddress(indirizzo, TCP_Port);
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(true);
        this.channel.connect(this.address);
        this.registry = LocateRegistry.getRegistry(ServerIP , RMI_PORT);
        this.RMIserver =  (RMIWORTHServer) registry.lookup("WORTH");
    }

    public void run(){
        if(interfaceTypeGUI) inter = new LoginFrame(this);
        else {
            System.out.println("Per ottenere un manuale sui comandi utilizzabili digita -help");
            while (Thread.currentThread().isAlive()){
                Scanner stream = new Scanner(System.in);
                String input = stream.nextLine();
                if (input.equals("-help")){
                    help();
                    continue;
                }
                StringTokenizer tokenizer = new StringTokenizer(input);
                List<String> comandi = new ArrayList<>();
                while (tokenizer.hasMoreElements()) comandi.add(tokenizer.nextToken());
                if (this.userName == null) {
                    // Caso in cui non è stato ancora effettuato l accesso
                    if (comandi.size() == 3) {
                        // Verifico cosa voglia fare se registrazione o login
                        if (comandi.get(0).equalsIgnoreCase("register")) {
                            try {
                                ServerReg(comandi.get(1), comandi.get(2));
                            } catch (IOException | NotBoundException e) {
                                System.out.println("Errore, esco");
                                exit(0);
                            }
                        } else if (comandi.get(0).equalsIgnoreCase("login")) {
                            ServerLog(comandi.get(1), comandi.get(2));
                        }
                    }
                    help();
                }else {
                    // L'utente ha già effettuato l accesso verifico cosa voglia fare
                    if (this.projectName==null) {
                        try {
                        switch (comandi.get(0).toLowerCase()) {
                            // Accesso al server già effettuato, verifico se si vuole accedere o creare un progetto
                            case "projectcreate":
                                try {
                                    if (!(comandi.size() > 1)) help();
                                    else createProject(comandi.get(1));
                                    continue;
                                } catch (IOException e) {
                                    System.out.println("Errore, esco");
                                    exit(0);
                                }
                                break;
                            case "projectaccess":
                                if (!(comandi.size() > 1)) help();
                                else accessProject(comandi.get(1));
                                break;
                            // Verifico se si voglia visualizzare la lista degli utenti
                            case "users":
                                users.keySet().forEach(System.out::println);
                                break;
                            // Verifico se si voglia visualizzare la lista degli utenti online
                            case "onlineusers":
                                users.forEach((k, v) -> {
                                    if (v.equals("Online")) System.out.println(k);
                                });
                                break;
                                // Verifico se si voglia visualizzare la lista dei progetti dell utente
                            case "myprojects":
                                getInfosforUser().forEach(System.out::println);
                                break;
                        }
                        }catch (IndexOutOfBoundsException e){
                            help();
                        }
                        help();
                    } else{
                        try {
                            // Login effettuato e accesso al progetto effettuato
                            switch (comandi.get(0).toLowerCase()) {
                                case "createcard":
                                    if (comandi.size() != 3) {
                                        help();
                                    } else {
                                        createCard(comandi.get(1), comandi.get(2));
                                    }
                                    continue;
                                case "showcard":
                                    if (comandi.size() != 2) {
                                        help();
                                    } else {
                                        showCard(comandi.get(1));
                                    }
                                    continue;
                                case "cardhistory":
                                    if (comandi.size() != 2) {
                                        help();
                                    } else {
                                        showCardHistory(comandi.get(1));
                                    }
                                    continue;
                                case "movecard":
                                    if (comandi.size() != 4) {
                                        help();
                                    } else {
                                        if (verifyList(comandi.get(2)) && verifyList(comandi.get(3)))
                                            MoveCard(comandi.get(1), comandi.get(2), comandi.get(3));
                                        else {
                                            System.out.println("List names can be: TODO INPROGRESS TOBEREVISITED DONE");
                                        }
                                    }
                                    continue;
                                case "logout":
                                    logOut();
                                    continue;
                                case "addmember":
                                    if (comandi.size() != 2) {
                                        help();
                                    } else {
                                        addMember(comandi.get(1));
                                    }
                                    continue;
                                case "deleteproject":
                                    if (comandi.size() != 1) {
                                        help();
                                    } else {
                                        System.out.println("Sicuro ? Digita si, o no:");
                                        Scanner delete = new Scanner(System.in);
                                        String ine = delete.nextLine();
                                        if (ine.equals("si"))
                                            deleteProject();
                                    }
                                    continue;
                                case "cardslist":
                                    if (comandi.size() != 1) {
                                        help();
                                    } else {
                                        CardsList();
                                    }
                                    continue;
                                case "memberslist":
                                    if (comandi.size() != 1) {
                                        help();
                                    } else {
                                        MembersList();
                                    }
                                    continue;
                                case "readchat":
                                    if (comandi.size() != 1) {
                                        help();
                                    } else {
                                        this.chat.read();
                                    }
                                    continue;
                                case "sendonchat":
                                    if (comandi.size() == 1) {
                                        help();
                                    } else {
                                        StringBuilder b = new StringBuilder();
                                        for (String i : comandi.subList(1, comandi.size())) b.append(i).append(" ");
                                        try {
                                            this.chat.Send(b.toString());
                                        } catch (IOException e) {
                                            System.out.println("Errore Chat,esco");
                                            exit(0);
                                        }
                                    }
                                    continue;
                            }
                        }catch (IndexOutOfBoundsException e){
                            help();
                            break;
                        }
                        help();
                    }
                }
            }
        }
    }

    // Verifico che la stringa inserita equivalga a una delle stringhe di cui necessito
    private boolean verifyList(String name){
        return  (name.equalsIgnoreCase("TODO")
                || name.equalsIgnoreCase("INPROGRESS")
                || name.equalsIgnoreCase("TOBEREVISITED")
                || name.equalsIgnoreCase("DONE"));
    }

    // Help menu per la versione da terminale
    private void help() {
        System.out.println();
        if (this.userName==null){
            System.out.println("Comandi possibili:");
            System.out.println("login nome_utente password");
            System.out.println("register nome_utente password");
        }else {
            if (this.projectName==null){
                System.out.println("Utente: " + this.getUserName());
                System.out.println("Comandi possibili:");
                System.out.println("Creare un progetto      :    projectcreate nome_progetto");
                System.out.println("Aprire un progetto      :    projectaccess nome_progetto");
                System.out.println("Lista degli utenti      :    users");
                System.out.println("Utenti online           :    onlineusers");
                System.out.println("Tuoi progetti           :    myprojects");
            }else {
                System.out.println("Utente: " + this.getUserName() + " Progetto: " + this.getProjectName());
                System.out.println("Vedere una card         :    showcard     nome_card");
                System.out.println("Storico card            :    cardhistory  nome_card");
                System.out.println("Creare una card         :    createcard   nome_card descrizione_card");
                System.out.println("Spostare una card       :    movecard     nome_card lista_partenza lista_destinazione");
                System.out.println("Aggiungi un membro      :    addmember    nome_utente");
                System.out.println("Cancella progetto       :    deleteproject");
                System.out.println("Lista delle card        :    cardslist");
                System.out.println("Lista dei membri        :    memberslist");
                System.out.println("Leggi chat              :    readchat");
                System.out.println("Invia messaggio chat    :    sendonchat messagge");
                System.out.println("Log out                 :    logout");
            }
        }
        System.out.println();
    }

    // Getter e setter per gli attributi del nome del client e del progetto che sta accedendo
    public String getUserName() { return userName; }
    public String getProjectName() {return projectName;}
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    // Metodo per registrarsi attraverso il metodo register RMI del server
    public synchronized void ServerReg(String name, String password)
            throws IOException, NotBoundException, NullPointerException {

        // Effettuo richiesta di registrazione e subito dopo di login
        try {
            if(RMIserver.register(name, password)){
                // Imposto il nome dell'utente
                this.setUserName(name);
                ServerLog(name, password);
            }else {
                if(interfaceTypeGUI) inter.error("Register Error");
                else System.out.println("Register Error");
            }
        } catch (UserAlreadyPresentException e) {
            // Nel caso in cui si immetta il nome di un utente già registrato
            if(interfaceTypeGUI) inter.error("Utente già presente");
            else System.out.println("Utente già presente");
        }
    }
    // Metodo per effettuare il login al server
    public void ServerLog(String name,String password) {
        // Effettuo l'hash della password per evitare di dover scambiare la password in chiaro
        String pwd = null;
        try {
            MessageDigest msgD = MessageDigest.getInstance("MD5");
            msgD.update(password.getBytes());
            byte[] digest = msgD.digest();
            pwd = DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            if(this.channel.isConnected()){
                //Contatto il server e gli mando la richiesta di login
                    String out = pwd  + " " + name ;

                    ByteBuffer et = ByteBuffer.wrap(out.getBytes());
                    channel.write(et);
                    //if (et.hasRemaining()) return;
                    et.clear();

                    StringBuilder sb = new StringBuilder();
                // "Leggo" la risposta del server per determinare l esito dell operazione
                    channel.read(et);
                    et.flip();
                    byte[] bytes = new byte[et.limit()];
                    et.get(bytes);
                    sb.append(new String(bytes));
                    String msg = sb.toString();
                    // Se il messaggio ricevuto inizia per session vuole dire che il login è andato a buon fine
                    // e che ora è presente una sessione di lavoro per questo utente
                    if(msg.equals("session:" + name)){
                        // Imposto il nome dell'utente sul client
                        setUserName(name);
                        // Registro il client per la callback in modo da ottenere informazioni su eventuali login
                        // o logout
                        callStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
                        if(interfaceTypeGUI) menu = new MenuFrame(this , inter.getLocation());
                        else System.out.println("Login Effettuato come "+ this.getUserName());
                        RMIserver.registerForCallback(callStub);
                        if(interfaceTypeGUI) {
                            inter.setVisible(false);
                            inter.dispose();
                        }
                    }else {
                        // Eventuali errori di login
                        if(interfaceTypeGUI)  inter.error(msg);
                        else System.out.println(msg);
                    }
            }else channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Richiesta di logout da parte del client al server
    public void logOut() {
        String proj = "project: " + this.getProjectName() + " LOGOUT " + this.getUserName();
        if (this.channel.isConnected()) {
            try {
                // Rimuovo il client dalla lista di utenti disponibili alla callback
                RMIserver.unregisterForCallback(callStub);
                // Invio la richiesta di logout al server
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(proj);
                if (msgs.size()==1) {
                    // Se ho solo un messaggio di ritorno significa che il logout è andato a buon fine
                    if(interfaceTypeGUI) {
                        prj.ok("Log out complete");
                        this.prj.setVisible(false);
                        this.prj.dispose();
                    }else System.out.println("Log out complete");
                    exit(0);
                }else {
                    // Errore nel logout
                    if(interfaceTypeGUI) prj.error(msgs.get(2));
                    else System.out.println(msgs.get(2));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Metodo per accedere ad un determinato progetto
    public void accessProject(String name) {

        String accProj = "ACCESSPROJECT " + name;
        if (this.channel.isConnected()){
            try {
                // Invio la richiesta di accesso al progetto al server
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(accProj);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente {project:nome_progetto_corrente o eventuali errori altrimenti}
                if(msgs.get(0).startsWith("session:" + this.getUserName()) && projectName==null){
                    if (msgs.size()>1) {
                        // Se le stringhe ottenute sono quella di sessione e quella di progetto ho effettuato l accesso
                        if(msgs.get(1).equals("project:"+ name)){
                            // Imposto il nome del progetto
                            projectName = name;
                            // Informo del successo dell operazione
                            if(interfaceTypeGUI) prj = new ProjectFrame(this, menu.getLocation());
                            else System.out.println("Accesso al progetto " + this.getProjectName() +" effettuato");
                            // Ottengo l indirizzo per la chat di gruppo
                            getChatIp(name);
                            if(interfaceTypeGUI) chat = new ChatClient(Chat_Port, this.getUserName(), prj.getChatFrame(), this.ipForChat);
                            else chat = new ChatClient(Chat_Port, this.getUserName(), this.ipForChat);
                            if(interfaceTypeGUI) menu.setVisible(false);
                            if(interfaceTypeGUI) menu.dispose();
                        }else {
                            // Eventuali errori nell accesso
                            if(interfaceTypeGUI) menu.error(msgs.get(1));
                            else System.out.println(msgs.get(1));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per creare un nuovo progetto
    public void createProject(String name) throws IOException {
        String createProj = "CREATEPROJECT " + name;
        if (this.channel.isConnected()){
            try {
                // Invio la richiesta di creazione del progetto name al server
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(createProj);
                if(msgs.get(0).startsWith("session")){
                    // Se ciò che ottengo è solo il messaggio di session l operazione è andata a buon fine ed effettuo
                    // l accesso al progetto creato, altrimenti c'è stato un errore
                    if (msgs.size()>1) {
                        if (interfaceTypeGUI) this.menu.error(msgs.get(1));
                        else System.out.println(msgs.get(1));
                    }else this.accessProject(name);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per creare una nuova card relativa al progetto corrente
    public void createCard(String name, String description){
        String createCard = "project: " + this.getProjectName() + " CREATECARD " + name + " " + description;
        if (this.channel.isConnected()){
            try {
                // Invio la richiesta di creazione della card al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(createCard);
                if(msgs.get(0).startsWith("session:" + this.getUserName())){
                    if (msgs.size()<3) {
                        // Se ottengo indietro solo la stringa di sessione e quella di progetto, l operazione ha avuto
                        // esito positivo
                        if(msgs.get(1).startsWith("project:")){
                            if (interfaceTypeGUI) prj.ok("Created " + name);
                            else System.out.println("Created "+name);
                        }
                    }else {
                        // Errore nella creazione della card
                        if (interfaceTypeGUI) prj.error(msgs.get(2));
                        else System.out.println(msgs.get(2));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per ottenere le informazioni su una card
    public void showCard(String name) {
        String showcard = "project: " + this.getProjectName() + " SHOWCARD " + name;
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere le info della card al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                        if (msgs.get(1).startsWith("project:")) {
                            // Se ciò che ottengo è una lista di 4 stringhe con la 3 uguale al nome della card creata
                            // allora avrò ottenuto le informazioni richieste
                            if(msgs.get(2).equals(name)){
                                if (interfaceTypeGUI) prj.ok(msgs.get(2) + " - " + msgs.get(3));
                                else System.out.println(msgs.get(2) + " - " + msgs.get(3));
                            }else {
                                // Se ho ottenuto solo 3 stringhe significa che si è presentato un errore
                                if (interfaceTypeGUI) prj.error(msgs.get(2));
                                else System.out.println(msgs.get(2));
                            }
                        }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per ottenere
    public void showCardHistory(String name) {
        String showcard = "project: " + this.getProjectName() + " HISTORY " + name;
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere lo storico della card al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                // Uso la versione con buffer impostato a "mano"
                List<String> msgs = tryConn(showcard, 2048);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente [lista di movimenti {Lista Data Ora} o errori]
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    if (msgs.get(1).startsWith("project:")) {
                        if(msgs.size()>4){
                            // La stringa ottenuta dovrà avere almeno 5 argomenti per far si che questa sia almeno un
                            // movimento per card (viene messa in TOT0 alla creazione)
                            // Creo una lista di operazione
                            List<String> history = new ArrayList<>();
                            for (int i=2; i < msgs.size(); i+=3){
                                history.add(msgs.get(i) + " " + msgs.get(i+1) + " " + msgs.get(i+2));
                            }
                            // Informo dello storico della card
                            if (interfaceTypeGUI) prj.sPanel(history);
                            else history.forEach(System.out::println);
                        }else {
                            // Eventuali errori
                            if (interfaceTypeGUI) prj.error(msgs.get(2));
                            else System.out.println(msgs.get(2));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per spostare una card da una lista a un altra
    public void MoveCard(String name, String oldl, String newl) {
        String showcard = "project: " + this.getProjectName()
                        + " MOVECARD " + name + " " + oldl + " " + newl;
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di spostare la card al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                if (msgs.get(0).equals("session:" + this.getUserName())) {
                    if (msgs.get(1).equals("project:"+this.getProjectName())) {
                        if(!(msgs.size() > 2)){
                            // Se le stringhe ottenute sono solo due significa che lo spostamento è andato a buon fine
                            if (interfaceTypeGUI) {
                                prj.ok("Moved " + "\"" + name + "\"" + " from " + oldl + " to " + newl);
                            }else System.out.println("Moved " + "\"" + name + "\"" + " from " + oldl + " to " + newl);
                        }else {
                            // Informo di eventuali errori
                            if (interfaceTypeGUI) prj.error(msgs.get(2));
                            else System.out.println(msgs.get(2));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Metodo per "contattare", vi si inviano informazioni attraversol a SocketChannel e successivamente si
    // "Legge" la risposta attraverso il buffer
    private List<String> tryConn(String e) throws IOException {
        // Invio la richiesta nel parametro e
        ByteBuffer et = ByteBuffer.wrap(e.getBytes());
        channel.write(et);
        et.clear();
        // Mi preparo per leggere le risposta del server
        ByteBuffer buf = ByteBuffer.allocate(256);
        StringBuilder sb = new StringBuilder();
        // Leggo
        channel.read(buf);
        // Resetto la posizione e la limit del buffer
        buf.flip();
        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
        // Leggo il contenuto e lo restituisco come una lista di stringhe
        sb.append(new String(bytes));
        return new ArrayList<>(Arrays.asList(sb.toString().split(" ")));
    }
    // Esattamente lo stesso metodo precedente ma con la dimensione del buffer impostata via parametro
    private List<String> tryConn(String e, int size) throws IOException {
        ByteBuffer et = ByteBuffer.wrap(e.getBytes());
        channel.write(et);
        et.clear();

        ByteBuffer buf = ByteBuffer.allocate(size);
        StringBuilder sb = new StringBuilder();
        channel.read(buf);
        buf.flip();
        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
        sb.append(new String(bytes));

        List<String> li = new ArrayList<>(Arrays.asList(sb.toString().split(" ")));
        if (li.contains("CET")) li.removeIf(i -> i.equals("CET"));
        return li;
    }
    @Override
    // Lista degli utenti e relativo stato ( aggiornata tramite callback dopo aver effettuato il login )
    public void notifyMe(Map<String, String> usrs) throws RemoteException {
        if(interfaceTypeGUI) menu.setUsers(usrs);
        else this.users = usrs;
    }
    // Richiesta di aggiungere un membro al progetto
    public void addMember(String name) {
        String showcard = "project: " + this.getProjectName() + " ADDMEMBER " + name;
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di aggiunta dell utente al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    if (msgs.get(1).startsWith("project:")) {
                        if(msgs.size()<3){
                            // Se non ho altri messaggi oltre alla sessione e al progetto
                            // l operazione è andata a buon fine
                            if(interfaceTypeGUI) prj.ok("Added "+ name);
                            else System.out.println("Added "+name);
                        }else {
                            // Se ho degli errori probabilmente l utente non esiste o è già membro
                            if(interfaceTypeGUI) prj.error(msgs.get(2));
                            else System.out.println(msgs.get(2));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per richiedere al server di eliminare un progetto
    public void deleteProject() {
        String proj = "project: " + this.getProjectName() + " DELETEPROJ";
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di eliminare il progetto al server
                // Utilizzo il metodo tryConn contattare il server
                List<String> msgs = tryConn(proj);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    if (msgs.get(1).startsWith("project:")) {
                        if(msgs.size()<3){
                            // Se non ho nessun messaggio oltre alla stringa di sessione e di progetto allora
                            // l operazione è andata a buon fine
                            if(interfaceTypeGUI) prj.ok("Deleted");
                            else System.out.println("Deleted");
                            exit(0);
                        }else {
                            // Probabilmente il progetto ha ancora card non in done
                            if(interfaceTypeGUI) prj.error(msgs.get(2));
                            else System.out.println(msgs.get(2));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Metodo per richiedere l'ip della chat di gruppo al server
    private void getChatIp(String projectName){
        String showcard = "project: " + this.getProjectName() + " GETCHAT " + projectName;
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere i membri al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente indirizzo ip
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    if (msgs.get(1).startsWith("project:")) {
                        if(msgs.size()>2){
                            // Se la lista di stringhe contiene almeno 3 stringhe allora avrò l indirizzo ip
                            this.ipForChat = msgs.get(2);
                        }else {
                            // Se non ha 3 stringhe vuol dire che qualcosa è andato storto
                            if(interfaceTypeGUI) inter.error("Nochatforproject");
                            else System.out.println("No chat for project");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    // Metodo per inviare messaggi sulla chat di progetto
    public void sendMessage(String txt) throws Exception {
        this.chat.Send(txt);
    }
    // Metodo per richiedere informazioni relative ai progetti a cui appartiene il client
    public List<String> getInfosforUser(){
        String showcard =  "GETINFOS";
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere i progetti al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente [lista progetti]
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                        if(msgs.size()>1){
                            // Se la lista contiene più di una stringa vuol dire che l'utente appartiene
                            // ad almeno un progetto
                            return msgs.subList(1,msgs.size());
                        }else return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    // Ottengo la lista dei membri del progetto
    public void MembersList(){
        String showcard =  "project: " + this.getProjectName() + " MLIST";
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere i membri al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente [lista membri]
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    // Questo evento è quasi sicuramente mai presentabile
                    if(!(msgs.size() < 3)){
                        // Se la lista di risposta ha almeno 4 stringhe implica che c'è almeno un altro membro oltre il
                        // client (l'utente corrente)
                        if(msgs.size()>3){
                            if(interfaceTypeGUI) prj.sPanel(msgs.subList(2,msgs.size()), "Members");
                            else System.out.println(msgs.subList(2,msgs.size()));
                        }else {
                            // Se la lista contiene solo il nome del client
                            if(interfaceTypeGUI) prj.error("Only You");
                            else System.out.println("Only You");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Ottengo la lista delle card
    public  void CardsList(){
        String showcard =  "project: " + this.getProjectName() + " CLIST";
        if (this.channel.isConnected()) {
            try {
                // Invio la richiesta di ottenere le card al server relativa al progetto corrente
                // Utilizzo il metodo tryConn per ottenere informazioni dal server
                List<String> msgs = tryConn(showcard);
                // Parso le informazioni seguendo uno schema definito ovvero
                // session:nome_utente project:nome_progetto_corrente [lista di card]
                if (msgs.get(0).startsWith("session:" + this.getUserName())) {
                    if (msgs.get(1).startsWith("project:")) {
                        if(!(msgs.size() < 3)){
                            // Se il messaggio non è inferiore a 3 stringhe significa che esiste almeno una card
                            if(interfaceTypeGUI) prj.sPanel(msgs.subList(2,msgs.size()), "Cards");
                            else System.out.println(msgs.subList(2, msgs.size()));
                        }else {
                            // Se il messaggio contiene solo 2 stringhe implica che non ci sono card per il progetto
                            if(interfaceTypeGUI) prj.error("No cards");
                            else System.out.println("No cards");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
